package com.mycelium.wapi.wallet.bip44

import com.google.common.base.Optional
import com.google.common.base.Preconditions
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.BipDerivationType.Companion.getDerivationTypeByAddress
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.WalletManager.Event
import java.util.*
import java.util.concurrent.TimeUnit

open class HDAccount(
        protected var context: HDAccountContext,
        protected val keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        protected val backing: Bip44AccountBacking,
        wapi: Wapi,
        protected val changeAddressModeReference: Reference<ChangeAddressMode>
) : AbstractAccount(backing, network, wapi), ExportableAccount {

    // Used to determine which bips this account supports
    private val derivePaths = context.indexesMap.keys

    protected var externalAddresses: MutableMap<BipDerivationType, BiMap<Address, Int>> = initAddressesMap()
    protected var internalAddresses: MutableMap<BipDerivationType, BiMap<Address, Int>> = initAddressesMap()

    private var receivingAddressMap: MutableMap<AddressType, Address> = mutableMapOf()
    @Volatile
    private var isSynchronizing = false

    // public method that needs no synchronization
    val accountIndex: Int
        get() = context.accountIndex

    //used for message signing picker
    //get all used external plus the next unused
    //get all used internal
    val allAddresses: List<Address>
        get() {
            val addresses = ArrayList<Address>()

            derivePaths.forEach { derivationType ->
                val externalIndex = context.getLastExternalIndexWithActivity(derivationType) + 1
                val external = externalAddresses[derivationType]!!.inverse()
                for (i in 0..externalIndex) {
                    addresses.add(external[i]!!)
                }

                val internalIndex = context.getLastInternalIndexWithActivity(derivationType)
                val internal = internalAddresses[derivationType]!!.inverse()
                for (i in 0..internalIndex) {
                    addresses.add(internal[i]!!)
                }
            }
            return addresses
        }


    open fun getPrivateKeyCount() = derivePaths.sumBy {
        context.getLastExternalIndexWithActivity(it) +
                2 + context.getLastInternalIndexWithActivity(it) + 1
    }


    val accountType = context.accountType

    init {
        type = WalletAccount.Type.BTCBIP44
        if (!isArchived) {
            ensureAddressIndexes()
            _cachedBalance = calculateLocalBalance()
        }
    }

    override fun getId() = context.id

    // public method that needs no synchronization
    override fun isArchived() = context.isArchived()

    // public method that needs no synchronization
    override fun isActive() = !isArchived

    @Synchronized
    override fun archiveAccount() {
        if (context.isArchived()) {
            return
        }
        clearInternalStateInt(true)
    }

    @Synchronized
    override fun activateAccount() {
        if (!context.isArchived()) {
            return
        }
        clearInternalStateInt(false)
    }

    override fun dropCachedData() {
        if (context.isArchived()) {
            return
        }
        clearInternalStateInt(false)
        context.persistIfNecessary(backing)
    }

    override fun isValidEncryptionKey(cipher: KeyCipher) = keyManagerMap.values.any { it.isValidEncryptionKey(cipher) }

    override fun isDerivedFromInternalMasterseed() = accountType == HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED

    private fun clearInternalStateInt(isArchived: Boolean) {
        backing.clear()
        externalAddresses = initAddressesMap()
        internalAddresses = initAddressesMap()
        receivingAddressMap.clear()
        _cachedBalance = null
        initContext(isArchived)
        if (isActive) {
            ensureAddressIndexes()
            _cachedBalance = calculateLocalBalance()
        }
    }

    override fun getAvailableAddressTypes(): List<AddressType> =
        derivePaths.asSequence().map { it.addressType }.toList()


    override fun setDefaultAddressType(addressType: AddressType) {
        context.defaultAddressType = addressType
        context.persistIfNecessary(backing)
    }

    protected fun initContext(isArchived: Boolean) {
        context = HDAccountContext(context.id, context.accountIndex, isArchived, context.accountType, context.accountSubId, derivePaths, context.defaultAddressType)
        context.persist(backing)
    }

    /**
     * Figure out whether this account has ever had any activity.
     *
     *
     * An account has had activity if it has one or more external addresses with
     * transaction history.
     *
     * @return true if this account has ever had any activity, false otherwise
     */
    fun hasHadActivity(): Boolean {
        // public method that needs no synchronization
        return derivePaths.any { context.getLastExternalIndexWithActivity(it) != -1 }
    }

    override fun isSynchronizing() = isSynchronizing

    /**
     * Ensure that all addresses in the look ahead window have been created
     */
    protected fun ensureAddressIndexes() {
        derivePaths.forEachIndexed { index, derivationType ->
            ensureAddressIndexes(true, true, derivationType)
            ensureAddressIndexes(false, true, derivationType)
            // The current receiving address is the next external address just above
            // the last
            // external address with activity
            val receivingAddress = externalAddresses[derivationType]!!.inverse()[context.getLastExternalIndexWithActivity(derivationType) + 1]
            if (receivingAddress != null && receivingAddress != receivingAddressMap[receivingAddress.type]) {
                receivingAddressMap[receivingAddress.type] = receivingAddress
                postEvent(Event.RECEIVING_ADDRESS_CHANGED)
            }
            LoadingProgressTracker.setPercent((index + 1) * 100 / derivePaths.size)
        }
    }

    private fun ensureAddressIndexes(isChangeChain: Boolean, fullLookAhead: Boolean, derivationType: BipDerivationType) {
        var index: Int
        val addressMap: BiMap<Address, Int>
        if (isChangeChain) {
            index = context.getLastInternalIndexWithActivity(derivationType)
            index += if (fullLookAhead) {
                INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
            } else {
                INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH
            }
            addressMap = internalAddresses[derivationType]!!
        } else {
            index = context.getLastExternalIndexWithActivity(derivationType)
            index += if (fullLookAhead) {
                EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
            } else {
                EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH
            }
            addressMap = externalAddresses[derivationType]!!
        }
        while (index >= 0) {
            if (addressMap.inverse().containsKey(index)) {
                return
            }
            addressMap[keyManagerMap[derivationType]!!.getAddress(isChangeChain, index)] = index
            index--
        }
    }

    private fun getAddressesToSync(mode: SyncMode): List<Address> {
        var addresses = mutableListOf<Address>()
        derivePaths.forEach { derivationType ->
            val currentInternalAddressId = context.getLastInternalIndexWithActivity(derivationType) + 1
            val currentExternalAddressId = context.getLastExternalIndexWithActivity(derivationType) + 1
            if (mode.mode == SyncMode.Mode.FULL_SYNC) {
                // check the full change-chain and external-chain
                addresses.addAll(getAddressRange(true, 0,
                        currentInternalAddressId + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH, derivationType))
                addresses.addAll(getAddressRange(false, 0,
                        currentExternalAddressId + EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH, derivationType))
            } else if (mode.mode == SyncMode.Mode.NORMAL_SYNC) {
                // check the current change address plus small lookahead;
                // plus the current external address plus a small range before and after it
                addresses.addAll(getAddressRange(true, currentInternalAddressId,
                        currentInternalAddressId + INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH, derivationType))
                addresses.addAll(getAddressRange(false, currentExternalAddressId - 3,
                        currentExternalAddressId + EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH, derivationType))

            } else if (mode.mode == SyncMode.Mode.FAST_SYNC) {
                // check only the current change address
                // plus the current external plus small lookahead
                addresses.add(keyManagerMap[derivationType]!!
                        .getAddress(true, currentInternalAddressId + 1) as Address)
                addresses.addAll(getAddressRange(false, currentExternalAddressId,
                        currentExternalAddressId + 2, derivationType))
            } else if (mode.mode == SyncMode.Mode.ONE_ADDRESS && mode.addressToSync != null) {
                // only check for the supplied address
                addresses = if (isMine(mode.addressToSync)) {
                    Lists.newArrayList(mode.addressToSync)
                } else {
                    throw IllegalArgumentException("Address " + mode.addressToSync + " is not part of my account addresses")
                }
            } else {
                throw IllegalArgumentException("Unexpected SyncMode")
            }
        }
        return ImmutableList.copyOf(addresses)
    }

    protected fun getAddressRange(isChangeChain: Boolean, fromIndex: Int, toIndex: Int,
                                  derivationType: BipDerivationType): List<Address> {
        val clippedFromIndex = Math.max(0, fromIndex) // clip at zero
        val ret = ArrayList<Address>(toIndex - clippedFromIndex + 1)
        for (i in clippedFromIndex..toIndex) {
            ret.add(keyManagerMap[derivationType]!!.getAddress(isChangeChain, i))
        }
        return ret
    }

    @Synchronized
    public override fun doSynchronization(proposedMode: SyncMode): Boolean {
        var mode = proposedMode
        checkNotArchived()
        isSynchronizing = true
        syncTotalRetrievedTransactions = 0
        _logger.logInfo("Starting sync: $mode")
        if (needsDiscovery()) {
            mode = SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED
        }
        try {
            if (mode.mode == SyncMode.Mode.FULL_SYNC) {
                // Discover new addresses once in a while
                if (!discovery()) {
                    return false
                }
            }

            // Update unspent outputs
            return updateUnspentOutputs(mode)
        } finally {
            isSynchronizing = false
            syncTotalRetrievedTransactions = 0
        }
    }

    private fun needsDiscovery() = !isArchived && context.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System
            .currentTimeMillis()

    @Synchronized
    private fun discovery(): Boolean {
        try {
            // discovered as in "discovered maybe something. further exploration is needed."
            // thus, method is done, once all are false.
            var discovered = derivePaths.map { it to true }.toMap()
            do {
                val pathsToDiscover = discovered.filter { it.value }
                        .map { it.key }
                        .toSet()
                discovered = doDiscovery(pathsToDiscover)
            } while (discovered.any { it.value })
        } catch (e: WapiException) {
            _logger.logError("Server connection failed with error code: " + e.errorCode, e)
            postEvent(Event.SERVER_CONNECTION_ERROR)
            return false
        }

        context.setLastDiscovery(System.currentTimeMillis())
        context.persistIfNecessary(backing)
        return true
    }

    /**
     * Do a look ahead on the external address chain. If any transactions were
     * found the external and internal last active addresses are updated, and the
     * transactions and their parent transactions stored.
     *
     * @return true if something was found and the call should be repeated.
     * @throws com.mycelium.wapi.api.WapiException
     */
    @Throws(WapiException::class)
    private fun doDiscovery(derivePaths: Set<BipDerivationType> = this.derivePaths): Map<BipDerivationType, Boolean> {
        // Ensure that all addresses in the look ahead window have been created
        ensureAddressIndexes()
        return doDiscoveryForAddresses(derivePaths.flatMap { getAddressesToDiscover(it) })
    }

    private fun getAddressesToDiscover(derivationType: BipDerivationType): ArrayList<Address> {
        // Make look ahead address list
        val lookAhead = ArrayList<Address>(EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH)
        val extInverse = externalAddresses[derivationType]!!.inverse()
        val intInverse = internalAddresses[derivationType]!!.inverse()

        for (i in 0 until EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH) {
            val externalAddress = extInverse[context.getLastExternalIndexWithActivity(derivationType) + 1 + i]
                    ?: continue
            lookAhead.add(externalAddress)
        }
        for (i in 0 until INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH) {
            lookAhead.add(intInverse[context.getLastInternalIndexWithActivity(derivationType) + 1 + i]!!)
        }
        return lookAhead
    }

    @Throws(WapiException::class)
    override fun doDiscoveryForAddresses(addresses: List<Address>): Map<BipDerivationType, Boolean> {
        // Do look ahead query
        val result = _wapi.queryTransactionInventory(
                QueryTransactionInventoryRequest(Wapi.VERSION, addresses)).result
        blockChainHeight = result.height
        val ids = result.txIds
        if (ids.isEmpty()) {
            // nothing found
            return derivePaths.map { it to false }.toMap()
        }

        val lastExternalIndexesBefore = derivePaths.map { it to context.getLastExternalIndexWithActivity(it) }.toMap()
        val lastInternalIndexesBefore = derivePaths.map { it to context.getLastInternalIndexWithActivity(it) }.toMap()
        ids.chunked(50).forEach { fewIds ->
            val transactions = getTransactionsBatched(fewIds).result.transactions
            handleNewExternalTransactions(transactions)
        }
        return derivePaths.map { derivationType ->
            // Return true if the last external or internal index has changed
            derivationType to
                    (lastExternalIndexesBefore[derivationType] != context.getLastExternalIndexWithActivity(derivationType)
                            || lastInternalIndexesBefore[derivationType] != context.getLastInternalIndexWithActivity(derivationType))
        }.toMap()
    }

    private fun updateUnspentOutputs(mode: SyncMode): Boolean {
        var checkAddresses = getAddressesToSync(mode)

        val newUtxos = synchronizeUnspentOutputs(checkAddresses)

        if (newUtxos == -1) {
            return false
        }

        if (newUtxos > 0 && mode.mode != SyncMode.Mode.FULL_SYNC) {
            // we got new UTXOs but did not made a full sync. The UTXO might be coming
            // from change outputs spending from addresses we are currently not checking
            // -> rerun the synchronizeUnspentOutputs for a FULL_SYNC
            checkAddresses = getAddressesToSync(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED)
            if (synchronizeUnspentOutputs(checkAddresses) == -1) {
                return false
            }
        }

        // update state of recent received transaction to update their confirmation state
        if (mode.mode != SyncMode.Mode.ONE_ADDRESS) {
            // Monitor young transactions
            if (!monitorYoungTransactions()) {
                return false
            }
        }

        updateLocalBalance()

        context.persistIfNecessary(backing)
        return true
    }

    private fun tightenInternalAddressScanRange() {
        // Find the lowest internal index at which we have an unspent output
        val unspent = backing.allUnspentOutputs
        val minInternalIndexesMap = mutableMapOf<BipDerivationType, Int>()
        derivePaths.associateByTo(minInternalIndexesMap, { it }, { Int.MAX_VALUE })
        for (output in unspent) {
            val outputScript = ScriptOutput.fromScriptBytes(output.script)
                    ?: continue // never happens, we have parsed it before
            val address = outputScript.getAddress(_network)
            val derivationType = getDerivationTypeByAddress(address)
            val index = internalAddresses[derivationType]!![address]
                    ?: continue
            val minInternalIndex = minInternalIndexesMap[derivationType]!!
            minInternalIndexesMap[derivationType] = Math.min(minInternalIndex, index)
        }

        // XXX also, from all the outgoing unconfirmed transactions we have, check
        // if any of them have outputs that we send from our change chain. If the
        // related address is lower than the one we had above, use its index as
        // the first monitored one.

        derivePaths.forEach { derivationType ->
            context.setFirstMonitoredInternalIndex(derivationType,
                    if (minInternalIndexesMap[derivationType] == Integer.MAX_VALUE) {
                        // there are no unspent outputs in our change chain
                        Math.max(0, context.getFirstMonitoredInternalIndex(derivationType))
                    } else {
                        minInternalIndexesMap[derivationType]!!
                    })
        }
    }

    // Get the next internal address just above the last address with activity
    public override fun getChangeAddress(destinationAddress: Address): Address =
            getChangeAddress(listOf(destinationAddress))

    public override fun getChangeAddress(destinationAddresses: List<Address>): Address =
            when (changeAddressModeReference.get()!!) {
                ChangeAddressMode.P2WPKH -> getChangeAddress(BipDerivationType.BIP84)
                ChangeAddressMode.P2SH_P2WPKH -> getChangeAddress(BipDerivationType.BIP49)
                ChangeAddressMode.PRIVACY -> {
                    val mostCommonOutputType = destinationAddresses.groupingBy {
                        BipDerivationType.getDerivationTypeByAddress(it)
                    }.eachCount().maxBy { it.value }!!.key
                    getChangeAddress(mostCommonOutputType)
                }
                ChangeAddressMode.NONE -> throw IllegalStateException()
            }

    override fun getChangeAddress(): Address {
        return when (changeAddressModeReference.get()!!) {
            ChangeAddressMode.P2WPKH -> getChangeAddress(BipDerivationType.BIP84)
            ChangeAddressMode.P2SH_P2WPKH, ChangeAddressMode.PRIVACY -> getChangeAddress(BipDerivationType.BIP49)
            ChangeAddressMode.NONE -> throw IllegalStateException()
        }
    }

    private fun getChangeAddress(preferredDerivationType: BipDerivationType): Address {
        val derivationType = if (derivePaths.contains(preferredDerivationType)) {
            preferredDerivationType
        } else {
            derivePaths.first()
        }
        return internalAddresses[derivationType]!!
                .inverse()[context.getLastInternalIndexWithActivity(derivationType) + 1]!!
    }

    override fun getReceivingAddress(): Optional<Address> {
        // if this account is archived, we cant ensure that we have the most recent ReceivingAddress (or any at all)
        // so return absent.
        return if (isArchived) {
            Optional.absent()
        } else {
            val receivingAddress = getReceivingAddress(context.defaultAddressType)
                    ?: receivingAddressMap.values.first()
            Optional.of(receivingAddress)
        }
    }

    override fun getReceivingAddress(addressType: AddressType): Address? {
        return receivingAddressMap[addressType]
    }

    override fun isMine(address: Address): Boolean {
        val derivationType = getDerivationTypeByAddress(address)
        return internalAddresses[derivationType]?.containsKey(address) ?: false ||
                externalAddresses[derivationType]?.containsKey(address) ?: false
    }

    // check whether we need to update our last index for activity
    override fun onNewTransaction(t: Transaction) = updateLastIndexWithActivity(t)

    override fun onTransactionsBroadcasted(txids: List<Sha256Hash>) {
        // See if we can reduce the internal scan range
        tightenInternalAddressScanRange()
        context.persistIfNecessary(backing)
    }

    /**
     * Update the index for the last external and internal address with activity.
     *
     * @param t transaction
     */
    private fun updateLastIndexWithActivity(t: Transaction) {
        // Investigate whether the transaction sends us any coins
        for (out in t.outputs) {
            val receivingAddress = out.script.getAddress(_network)
            val derivationType = getDerivationTypeByAddress(receivingAddress)
            val externalIndex = externalAddresses[derivationType]?.get(receivingAddress)
            if (externalIndex != null) {
                updateLastExternalIndex(externalIndex, derivationType)
            } else {
                updateLastInternalIndex(receivingAddress)
            }
        }
        ensureAddressIndexes()
    }

    /**
     * Update the new last external address with activity
     *
     * @param externalIndex new index
     */
    protected fun updateLastExternalIndex(externalIndex: Int, derivationType: BipDerivationType) {
        // Sends coins to an external address, update internal max index if
        // necessary
        context.setLastExternalIndexWithActivity(derivationType,
                Math.max(context.getLastExternalIndexWithActivity(derivationType), externalIndex))
    }

    /**
     * Update the new last internal address with activity.
     *
     * @param receivingAddress
     */
    protected fun updateLastInternalIndex(receivingAddress: Address) {
        val derivationType = getDerivationTypeByAddress(receivingAddress)
        val internalIndex = internalAddresses[derivationType]?.get(receivingAddress)
        if (internalIndex != null) {
            // Sends coins to an internal address, update internal max index
            // if necessary

            context.setLastInternalIndexWithActivity(derivationType,
                    Math.max(context.getLastInternalIndexWithActivity(derivationType), internalIndex))
        }
    }

    @Throws(InvalidKeyCipher::class)
    override fun getPrivateKey(publicKey: PublicKey, cipher: KeyCipher): InMemoryPrivateKey? {
        for (address in publicKey.getAllSupportedAddresses(_network).values) {
            return getPrivateKeyForAddress(address, cipher)
                    ?: continue
        }
        return null
    }

    @Throws(InvalidKeyCipher::class)
    public override fun getPrivateKeyForAddress(address: Address, cipher: KeyCipher): InMemoryPrivateKey? {
        val derivationType = getDerivationTypeByAddress(address)
        if (!availableAddressTypes.contains(address.type)) {
                return null
        }
        val indexLookUp = getIndexLookup(address, derivationType)
        return if (indexLookUp == null) {
            // still not found? give up...
            null
        } else {
            keyManagerMap[derivationType]!!.getPrivateKey(indexLookUp.isChange, indexLookUp.index!!, cipher)
        }
    }

    override fun getPublicKeyForAddress(address: Address): PublicKey? {
        val derivationType = getDerivationTypeByAddress(address)
        if (!availableAddressTypes.contains(address.type)) {
            return null
        }
        val indexLookUp = getIndexLookup(address, derivationType)
        return if (indexLookUp == null) {
            // still not found? give up...
            null
        } else {
            Preconditions.checkNotNull(keyManagerMap[derivationType]!!.getPublicKey(indexLookUp.isChange, indexLookUp
                    .index!!))
        }

    }

    private fun getIndexLookup(address: Address, derivationType: BipDerivationType): IndexLookUp? {
        var indexLookUp = IndexLookUp.forAddress(address, externalAddresses[derivationType]!!, internalAddresses[derivationType]!!)
        if (indexLookUp == null) {
            // we did not find it - to be sure, generate all addresses and search again
            ensureAddressIndexes()
            indexLookUp = IndexLookUp.forAddress(address, externalAddresses[derivationType]!!, internalAddresses[derivationType]!!)
        }
        return indexLookUp
    }

    override fun toString(): String {
        val sb = StringBuilder("HD ID: ").append(id)
        if (isArchived) {
            sb.append(" Archived")
        } else {
            if (_cachedBalance == null) {
                sb.append(" Balance: not known")
            } else {
                sb.append(" Balance: $_cachedBalance")
            }
            val receivingAddress = receivingAddress
            sb.append(" Receiving Address: ${if (receivingAddress.isPresent) receivingAddress.get().toString() else ""}")
            toStringMonitoredAddresses(sb)
            sb.append(" Spendable Outputs: ${getSpendableOutputs(0).size}")
        }
        return sb.toString()
    }

    protected fun toStringMonitoredAddresses(sb: StringBuilder) {
        sb.append(" Monitored Addresses:")
        derivePaths.forEach { derivationType ->
            sb.append(" BIP: ${derivationType.name} external= ${context.getLastExternalIndexWithActivity
            (derivationType) + 2}")
                    .append(" internal=${context.getLastInternalIndexWithActivity(derivationType) + 1 -
                            context.getFirstMonitoredInternalIndex(derivationType)}")
        }
    }

    fun getAddressId(address: Address): Optional<Array<Int>> {
        if (address.type !in availableAddressTypes) {
            return Optional.absent()
        }
        val derivationType = getDerivationTypeByAddress(address)
        val (changeIndex, addressMap) =  when (address) {
            in externalAddresses[derivationType]!!.keys -> Pair(0, externalAddresses)
            in internalAddresses[derivationType]!!.keys -> Pair(1, internalAddresses)
            else -> return Optional.absent()
        }
        return Optional.of(arrayOf(changeIndex, addressMap[derivationType]!![address]!!))
    }

    // returns true if this is one of our already used or monitored internal (="change") addresses
    override fun isOwnInternalAddress(address: Address): Boolean {
        val addressId = getAddressId(address)
        return addressId.isPresent && addressId.get()[0] == 1
    }

    // returns true if this is one of our already used or monitored external (=normal receiving) addresses
    override fun isOwnExternalAddress(address: Address): Boolean {
        val addressId = getAddressId(address)
        return addressId.isPresent && addressId.get()[0] == 0
    }

    override fun canSpend() = true

    override fun getBlockChainHeight(): Int {
        // public method that needs no synchronization
        checkNotArchived()
        return context.getBlockHeight()
    }

    public override fun setBlockChainHeight(blockHeight: Int) {
        checkNotArchived()
        context.setBlockHeight(blockHeight)
    }

    override fun persistContextIfNecessary() {
        context.persistIfNecessary(backing)
    }

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        val privateDataMap = if (canSpend()) {
            try {
                keyManagerMap.keys.map { derivationType ->
                    derivationType to (keyManagerMap[derivationType]!!.getPrivateAccountRoot(cipher, derivationType)
                            .serialize(_network, derivationType))
                }.toMap()
            } catch (ignore: InvalidKeyCipher) {
                null
            }
        } else {
            null
        }
        val publicDataMap = keyManagerMap.keys.map { derivationType ->
            derivationType to (keyManagerMap[derivationType]!!.publicAccountRoot
                    .serialize(_network, derivationType))
        }.toMap()
        return ExportableAccount.Data(privateDataMap, publicDataMap)
    }

    // deletes everything account related from the backing
    // this method is only allowed for accounts that use a SubValueKeystore
    fun clearBacking() = keyManagerMap.values.forEach(HDAccountKeyManager::deleteSubKeyStore)

    // Helper class to find the mapping for a Address in the internal or external chain
    private class IndexLookUp private constructor(val isChange: Boolean, val index: Int?) {
        companion object {
            fun forAddress(address: Address, external: Map<Address, Int>, internal: Map<Address, Int>): IndexLookUp? {
                var index: Int? = external[address]
                return if (index == null) {
                    index = internal[address]
                    if (index == null) {
                        null
                    } else {
                        // found it in the internal(=change)-chain
                        IndexLookUp(true, index)
                    }
                } else {
                    // found it in the external chain
                    IndexLookUp(false, index)
                }
            }
        }
    }

    protected fun initAddressesMap(): MutableMap<BipDerivationType, BiMap<Address, Int>> = derivePaths
            .map { it to HashBiMap.create<Address, Int>() }.toMap().toMutableMap()

    companion object {
        const val EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        const val INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        private const val EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 4
        private const val INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 1
        private val FORCED_DISCOVERY_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }
}

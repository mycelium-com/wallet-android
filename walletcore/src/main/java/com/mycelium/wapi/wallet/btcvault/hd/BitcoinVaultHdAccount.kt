package com.mycelium.wapi.wallet.btcvault.hd

import com.google.common.base.Preconditions
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Lists
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.Reference
import com.mycelium.wapi.wallet.btc.bip44.AddressesListProvider
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btcvault.AbstractBtcvAccount
import com.mycelium.wapi.wallet.btcvault.BTCVNetworkParameters
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.btcvault.BtcvTransaction
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.manager.HDAccountKeyManager
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min


class BitcoinVaultHdAccount(protected var accountContext: BitcoinVaultHDAccountContext,
                            protected val keyManagerMap: Map<BipDerivationType, HDAccountKeyManager<BtcvAddress>>,
                            val networkParameters: BTCVNetworkParameters,
                            wapi: Wapi,
                            val backing: BitcoinVaultHDAccountBacking,
                            accountListener: AccountListener?,
                            protected val changeAddressModeReference: Reference<ChangeAddressMode>)
    : AbstractBtcvAccount(backing, networkParameters, wapi, accountListener),
        ExportableAccount,
        AddressesListProvider<BtcvAddress>,
        SyncPausable {

    private val derivePaths = accountContext.indexesMap.keys
    protected var externalAddresses: MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = initAddressesMap()
    protected var internalAddresses: MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = initAddressesMap()
    private val safeLastExternalIndex: MutableMap<BipDerivationType, Int> = mutableMapOf()
    private val safeLastInternalIndex: MutableMap<BipDerivationType, Int> = mutableMapOf()
    private var receivingAddressMap: MutableMap<AddressType, BtcvAddress> = mutableMapOf()

    val accountIndex: Int
        get() = accountContext.accountIndex


    init {
        if (!isArchived) {
            ensureAddressIndexes()
            cachedBalance = calculateLocalBalance()
        }
        initSafeLastIndexes(false)
    }

    val allAddresses: List<BtcvAddress>
        get() {
            val addresses = ArrayList<BtcvAddress>()

            derivePaths.forEach { derivationType ->
                val externalIndex = accountContext.getLastExternalIndexWithActivity(derivationType) + 1
                val external = externalAddresses[derivationType]!!.inverse()
                for (i in 0..externalIndex) {
                    addresses.add(external[i]!!)
                }

                val internalIndex = accountContext.getLastInternalIndexWithActivity(derivationType)
                val internal = internalAddresses[derivationType]!!.inverse()
                for (i in 0..internalIndex) {
                    addresses.add(internal[i]!!)
                }
            }
            return addresses
        }

    override val id: UUID
        get() = accountContext.id

    override val isArchived: Boolean
        get() = accountContext.archived

    override val isActive: Boolean
        get() = !accountContext.archived

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        val btcTx: BtcvTransaction = tx as BtcvTransaction
        return broadcastTransaction(btcTx.tx!!)
    }

    override val receiveAddress: BtcvAddress?
        get() = receivingAddressMap[accountContext.defaultAddressType]

    fun getReceiveAddress(addressType: AddressType): BtcvAddress? = receivingAddressMap[addressType]

    override val coinType: CryptoCurrency
        get() = accountContext.currency

    override val basedOnCoinType: CryptoCurrency
        get() = accountContext.currency

    override fun isMineAddress(address: Address?): Boolean {
        return try {
            if (address !is BtcvAddress) {
                false
            } else {
                val derivationType = BipDerivationType.getDerivationTypeByAddress(address)
                internalAddresses[derivationType]?.containsKey(address) ?: false ||
                        externalAddresses[derivationType]?.containsKey(address) ?: false
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    override fun createTx(
        outputs: List<Pair<Address, Value>>,
        fee: Fee,
        data: TransactionData?
    ): Transaction {
        TODO("Not yet implemented")
    }

    override fun isExchangeable(): Boolean = true

    override suspend fun doDiscoveryForAddresses(addresses: List<BtcvAddress>): Set<BipDerivationType> {
        // Do look ahead query
        val result = wapi.queryTransactionInventory(
                QueryTransactionInventoryRequest(Wapi.VERSION, addresses)).result
        if (!maySync) { return emptySet() }
        setBlockChainHeight(result.height)
        val ids = result.txIds
        if (ids.isEmpty()) {
            // nothing found
            return emptySet()
        }

        val lastExternalIndexesBefore = derivePaths.map { it to accountContext.getLastExternalIndexWithActivity(it) }.toMap()
        val lastInternalIndexesBefore = derivePaths.map { it to accountContext.getLastInternalIndexWithActivity(it) }.toMap()
        // query DB only once to sort TXIDs into new and old ones. Unconfirmed transactions are
        // "new" in this sense until we know which block they fell into.
        val newIds = mutableSetOf<Sha256Hash>()
        val knownTransactions = mutableSetOf<TransactionEx>()
        ids.forEach {
            if (!maySync) { return emptySet() }
            val dbTransaction = backing.getTransaction(it)
            if (dbTransaction?.height ?: 0 > 0) {
                // we have it and know its block
                knownTransactions.add(dbTransaction!!)
            } else {
                // we have to query for details
                newIds.add(it)
            }
        }
        newIds.chunked(50).forEach { fewIds ->
            val transactions = getTransactionsBatched(fewIds).result.transactions
            handleNewExternalTransactions(transactions)
        }
        handleNewExternalTransactions(knownTransactions, true)
        return derivePaths.filter { derivationType ->
            // only include if the last external or internal index has changed
            (lastExternalIndexesBefore[derivationType] != accountContext.getLastExternalIndexWithActivity(derivationType)
                    || lastInternalIndexesBefore[derivationType] != accountContext.getLastInternalIndexWithActivity(derivationType))
        }.toSet()
    }

    override var label: String
        get() = accountContext.accountName
        set(value) {
            accountContext.accountName = value
        }

    override suspend fun doSynchronization(proposedMode: SyncMode): Boolean {
        if (!maySync) { return false }
        var mode = proposedMode
        checkNotArchived()
        syncTotalRetrievedTxs = 0
        logger.log(Level.INFO, "Starting sync: $mode")
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
            if (!maySync) { return false }
            // Update unspent outputs
            return updateUnspentOutputs(mode)
        } catch (e: RuntimeException) {
            logger.log(Level.SEVERE, "doSynchronization: $mode", e)
            return true
        } finally {
            syncTotalRetrievedTxs = 0
        }
    }

    private fun needsDiscovery() = !isArchived &&
            accountContext.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System.currentTimeMillis()

    override fun toBtcvAddress(bitcoinAddress: BitcoinAddress): BtcvAddress =
            when (bitcoinAddress) {
                is SegwitAddress -> BtcvSegwitAddress(coinType, networkParameters, bitcoinAddress.version.toInt(), bitcoinAddress.program)
                is BtcvSegwitAddress -> bitcoinAddress
                is BtcvAddress -> bitcoinAddress
                else -> BtcvAddress(coinType, bitcoinAddress.allAddressBytes)
            }

    private suspend fun discovery(): Boolean {
        try {
            // discovered as in "discovered maybe something. further exploration is needed."
            // thus, method is done once discovered is empty.
            var discovered = derivePaths.toSet()
            do {
                if (!maySync) { return false }
                discovered = doDiscovery(discovered)
            } while (discovered.isNotEmpty())
        } catch (e: WapiException) {
            logger.log(Level.SEVERE, "Server connection failed with error code: " + e.errorCode, e)
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            accountListener?.serverConnectionError(this, "Bitcoin Vault")
            return false
        }

        accountContext.setLastDiscovery(System.currentTimeMillis())
        accountContext.persistIfNecessary(backing)
        return true
    }

    private suspend fun updateUnspentOutputs(mode: SyncMode): Boolean {
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

        accountContext.persistIfNecessary(backing)
        return true
    }

    private fun getAddressesToSync(mode: SyncMode): List<BtcvAddress> {
        var addresses = mutableListOf<BtcvAddress>()
        derivePaths.forEach { derivationType ->
            val currentInternalAddressId = accountContext.getLastInternalIndexWithActivity(derivationType) + 1
            val currentExternalAddressId = accountContext.getLastExternalIndexWithActivity(derivationType) + 1
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
                keyManagerMap[derivationType]?.getAddress(true, currentInternalAddressId + 1)?.let {
                    addresses.add(it)
                }
                addresses.addAll(getAddressRange(false, currentExternalAddressId,
                        currentExternalAddressId + 2, derivationType))
            } else if (mode.mode == SyncMode.Mode.ONE_ADDRESS && mode.addressToSync != null) {
                // only check for the supplied address
                addresses = if (isMineAddress(mode.addressToSync)) {
                    Lists.newArrayList(BtcvAddress.fromString(coinType, mode.addressToSync.toString()))
                } else {
                    throw IllegalArgumentException("Address " + mode.addressToSync + " is not part of my account addresses")
                }
            } else {
                throw IllegalArgumentException("Unexpected SyncMode")
            }
        }
        return addresses
    }

    protected fun getAddressRange(isChangeChain: Boolean, fromIndex: Int, toIndex: Int,
                                  derivationType: BipDerivationType): List<BtcvAddress> {
        val clippedFromIndex = max(0, fromIndex) // clip at zero
        val ret = ArrayList<BtcvAddress>(toIndex - clippedFromIndex + 1)
        for (i in clippedFromIndex..toIndex) {
            keyManagerMap[derivationType]!!.getAddress(isChangeChain, i)?.let {
                ret.add(it)
            }
        }
        return ret
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
    private suspend fun doDiscovery(derivePaths: Set<BipDerivationType>): Set<BipDerivationType> {
        // Ensure that all addresses in the look ahead window have been created
        ensureAddressIndexes()
        return doDiscoveryForAddresses(derivePaths.flatMap { getAddressesToDiscover(it) })
    }

    private fun getAddressesToDiscover(derivationType: BipDerivationType): List<BtcvAddress> {
        // getAddressesToDiscover has to progress covering all addresses while last address with
        // activity might advance in jumps from sending to oneself from an old UTXO address to one
        // 30 later.
        // Make look ahead address list
        val lastExternalIndex = min(accountContext.getLastExternalIndexWithActivity(derivationType), safeLastExternalIndex[derivationType]
                ?: 0)
        val lastInternalIndex = min(accountContext.getLastInternalIndexWithActivity(derivationType), safeLastInternalIndex[derivationType]
                ?: 0)
        safeLastExternalIndex[derivationType] = lastExternalIndex + HDAccount.EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
        safeLastInternalIndex[derivationType] = lastInternalIndex + HDAccount.INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
        return (externalAddresses[derivationType]!!.filter {
            lastExternalIndex <= it.value && it.value <= lastExternalIndex + HDAccount.EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
        } + internalAddresses[derivationType]!!.filter {
            lastInternalIndex <= it.value && it.value <= lastInternalIndex + HDAccount.INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
        }).map { it.key }
    }

    override fun getBlockChainHeight(): Int = accountContext.blockHeight

    override fun canSpend(): Boolean = true

    override fun canSign(): Boolean = true

    override fun archiveAccount() {
        accountContext.archived = true
        clearInternalStateInt()
        accountListener?.onAccountActiveStateChanged(accountContext.id)
    }

    override fun activateAccount() {
        accountContext.archived = false
        clearInternalStateInt()
        accountListener?.onAccountActiveStateChanged(accountContext.id)
    }

    override fun dropCachedData() {
        if (accountContext.archived) {
            return
        }
        clearInternalStateInt()
        accountContext.persistIfNecessary(backing)
    }

    protected fun initContext() {
        accountContext = BitcoinVaultHDAccountContext(accountContext.id, accountContext.currency,
                accountContext.accountIndex, accountContext.archived, accountContext.accountName,
                Balance.getZeroBalance(accountContext.currency), backing::updateAccountContext,
                accountType = accountContext.accountType, accountSubId = accountContext.accountSubId,
                defaultAddressType = accountContext.defaultAddressType)
        accountContext.persist(backing)
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = accountContext.accountType == BitcoinVaultHDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED

    override fun getPrivateKey(publicKey: PublicKey, cipher: KeyCipher): InMemoryPrivateKey? {
        for (address in publicKey.getAllSupportedAddresses(network).values) {
            return getPrivateKeyForAddress(address, cipher)
                    ?: continue
        }
        return null
    }

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        // This method should NOT be called for HD account since it has more than one private key
        throw RuntimeException("Calling getPrivateKey() is not supported for HD account")
    }

    override fun onNewTransaction(t: BitcoinTransaction) = updateLastIndexWithActivity(t)

    override fun onTransactionsBroadcasted(txids: List<Sha256Hash>) {
        // See if we can reduce the internal scan range
        tightenInternalAddressScanRange()
        accountContext.persistIfNecessary(backing)
    }

    private fun tightenInternalAddressScanRange() {
        // Find the lowest internal index at which we have an unspent output
        val unspent = backing.allUnspentOutputs
        val minInternalIndexesMap = derivePaths.associateWith { Int.MAX_VALUE }.toMutableMap()
        for (output in unspent) {
            val outputScript = ScriptOutput.fromScriptBytes(output.script)
                    ?: continue // never happens, we have parsed it before
            val address = outputScript.getAddress(network)
            val derivationType = BipDerivationType.getDerivationTypeByAddress(address)
            val index = internalAddresses[derivationType]!![address]
                    ?: continue
            val minInternalIndex = minInternalIndexesMap[derivationType]!!
            minInternalIndexesMap[derivationType] = min(minInternalIndex, index)
        }

        // XXX also, from all the outgoing unconfirmed transactions we have, check
        // if any of them have outputs that we send from our change chain. If the
        // related address is lower than the one we had above, use its index as
        // the first monitored one.

        derivePaths.forEach { derivationType ->
            accountContext.setFirstMonitoredInternalIndex(derivationType,
                    if (minInternalIndexesMap[derivationType] == Integer.MAX_VALUE) {
                        // there are no unspent outputs in our change chain
                        max(0, accountContext.getFirstMonitoredInternalIndex(derivationType))
                    } else {
                        minInternalIndexesMap[derivationType]!!
                    })
        }
    }

    /**
     * Update the index for the last external and internal address with activity.
     *
     * @param t transaction
     */
    private fun updateLastIndexWithActivity(t: BitcoinTransaction) {
        // Investigate whether the transaction sends us any coins
        for (out in t.outputs) {
            val receivingAddress = toBtcvAddress(out.script.getAddress(network))
            val derivationType = BipDerivationType.getDerivationTypeByAddress(receivingAddress)
            updateLastExternalIndex(receivingAddress, derivationType)
            updateLastInternalIndex(receivingAddress, derivationType)
        }
        ensureAddressIndexes()
    }

    /**
     * Update the new last external address with activity
     *
     * @param externalIndex new index
     */
    protected fun updateLastExternalIndex(receivingAddress: BtcvAddress, derivationType: BipDerivationType) {
        externalAddresses[derivationType]?.get(receivingAddress)?.also { externalIndex ->
            // Sends coins to an external address, update internal max index if necessary
            if (accountContext.getLastExternalIndexWithActivity(derivationType) < externalIndex) {
                accountContext.setLastExternalIndexWithActivity(derivationType, externalIndex)
            }
        }
    }

    /**
     * Update the new last internal address with activity.
     *
     * @param receivingAddress
     */
    protected fun updateLastInternalIndex(receivingAddress: BtcvAddress, derivationType: BipDerivationType) {
        internalAddresses[derivationType]?.get(receivingAddress)?.also { internalIndex ->
            // Sends coins to an internal address, update internal max index if necessary
            if (accountContext.getLastInternalIndexWithActivity(derivationType) < internalIndex) {
                accountContext.setLastInternalIndexWithActivity(derivationType, internalIndex)
            }
        }
    }

    override fun setBlockChainHeight(blockHeight: Int) {
        checkNotArchived()
        accountContext.blockHeight = blockHeight
    }

    override fun persistContextIfNecessary() {
        accountContext.persistIfNecessary(backing)
    }

    override fun getChangeAddress(vararg destinationAddresses: BtcvAddress): BtcvAddress =
            when (changeAddressModeReference.get()!!) {
                ChangeAddressMode.P2WPKH -> getChangeAddress(BipDerivationType.BIP84)
                ChangeAddressMode.P2SH_P2WPKH -> getChangeAddress(BipDerivationType.BIP49)
                ChangeAddressMode.PRIVACY -> {
                    val mostCommonOutputType = destinationAddresses.groupingBy {
                        BipDerivationType.getDerivationTypeByAddress(it)
                    }.eachCount().maxBy { it.value }!!.key
                    getChangeAddress(mostCommonOutputType)
                }
                ChangeAddressMode.P2TR -> throw IllegalStateException()
                ChangeAddressMode.NONE -> throw IllegalStateException()
            }

    override val changeAddress: BtcvAddress
        get() {
            return when (changeAddressModeReference.get()!!) {
                ChangeAddressMode.P2WPKH -> getChangeAddress(BipDerivationType.BIP84)
                ChangeAddressMode.P2SH_P2WPKH, ChangeAddressMode.PRIVACY -> getChangeAddress(BipDerivationType.BIP49)
                ChangeAddressMode.P2TR -> throw IllegalStateException()
                ChangeAddressMode.NONE -> throw IllegalStateException()
            }
        }

    private fun getChangeAddress(preferredDerivationType: BipDerivationType): BtcvAddress {
        val derivationType = if (derivePaths.contains(preferredDerivationType)) {
            preferredDerivationType
        } else {
            derivePaths.first()
        }
        return internalAddresses[derivationType]!!
                .inverse()[accountContext.getLastInternalIndexWithActivity(derivationType) + 1]!!
    }

    override fun getPrivateKeyForAddress(address: BitcoinAddress, cipher: KeyCipher): InMemoryPrivateKey? {
        val btcvAddress = toBtcvAddress(address)
        val derivationType = BipDerivationType.getDerivationTypeByAddress(btcvAddress)
        if (!availableAddressTypes.contains(btcvAddress.type)) {
            return null
        }
        val indexLookUp = getIndexLookup(btcvAddress, derivationType)
                ?: return null
        return keyManagerMap[derivationType]!!.getPrivateKey(indexLookUp.isChange, indexLookUp.index!!, cipher)
    }

    private fun getIndexLookup(address: BtcvAddress, derivationType: BipDerivationType): IndexLookUp? {
        var indexLookUp = IndexLookUp.forAddress(address, externalAddresses[derivationType]!!, internalAddresses[derivationType]!!)
        if (indexLookUp == null) {
            // we did not find it - to be sure, generate all addresses and search again
            ensureAddressIndexes()
            indexLookUp = IndexLookUp.forAddress(address, externalAddresses[derivationType]!!, internalAddresses[derivationType]!!)
        }
        return indexLookUp
    }

    // Helper class to find the mapping for a Address in the internal or external chain
    private class IndexLookUp private constructor(val isChange: Boolean, val index: Int?) {
        companion object {
            fun forAddress(address: BtcvAddress, external: Map<BtcvAddress, Int>, internal: Map<BtcvAddress, Int>): IndexLookUp? {
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

    override val availableAddressTypes: List<AddressType>
        get() {
            return derivePaths.asSequence().map { it.addressType }.toList()
        }


    override fun setDefaultAddressType(addressType: AddressType) {
        accountContext.defaultAddressType = addressType
        accountContext.persistIfNecessary(backing)
    }

    override fun getPublicKeyForAddress(address: BitcoinAddress): PublicKey? {
        val derivationType = BipDerivationType.getDerivationTypeByAddress(address)
        if (!availableAddressTypes.contains(address.type)) {
            return null
        }
        val indexLookUp = getIndexLookup(toBtcvAddress(address), derivationType)
        return if (indexLookUp == null) {
            // still not found? give up...
            null
        } else {
            Preconditions.checkNotNull(keyManagerMap[derivationType]!!.getPublicKey(indexLookUp.isChange, indexLookUp
                    .index!!))
        }
    }

    override fun isOwnExternalAddress(address: BtcvAddress): Boolean {
        val addressId = getAddressId(address)
        return addressId?.get(0) == 0
    }

    private fun getAddressId(address: BtcvAddress): Array<Int>? {
        if (address.type !in availableAddressTypes) {
            return null
        }
        val derivationType = BipDerivationType.getDerivationTypeByAddress(address)
        val (changeIndex, addressMap) = when (address) {
            in externalAddresses[derivationType]!!.keys -> Pair(0, externalAddresses)
            in internalAddresses[derivationType]!!.keys -> Pair(1, internalAddresses)
            else -> return null
        }
        return arrayOf(changeIndex, addressMap[derivationType]!![address]!!)
    }

    override fun isValidEncryptionKey(cipher: KeyCipher?): Boolean = keyManagerMap.values.any { it.isValidEncryptionKey(cipher) }

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        val privateDataMap = if (canSpend()) {
            try {
                keyManagerMap.mapValues {
                    it.value.getPrivateAccountRoot(cipher, it.key).serialize(network, it.key)
                }
            } catch (ignore: KeyCipher.InvalidKeyCipher) {
                null
            }
        } else {
            null
        }
        val publicDataMap = keyManagerMap.mapValues {
            it.value.publicAccountRoot.serialize(network, it.key)
        }
        return ExportableAccount.Data(privateDataMap, publicDataMap)
    }

    private fun clearInternalStateInt() {
        backing.clear()
        externalAddresses = initAddressesMap()
        internalAddresses = initAddressesMap()
        receivingAddressMap.clear()
        cachedBalance = null
        initContext()
        initSafeLastIndexes(true)
        if (isActive) {
            ensureAddressIndexes()
            cachedBalance = calculateLocalBalance()
        }
    }

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
            val receivingAddress = externalAddresses[derivationType]!!.inverse()[accountContext.getLastExternalIndexWithActivity(derivationType) + 1]
            if (receivingAddress != null && receivingAddress != receivingAddressMap[receivingAddress.type]) {
                receivingAddressMap[receivingAddress.type] = receivingAddress
                accountListener?.receivingAddressChanged(this, receivingAddress)
            }
            LoadingProgressTracker.setPercent((index + 1) * 100 / derivePaths.size)
        }
    }

    private fun ensureAddressIndexes(isChangeChain: Boolean, fullLookAhead: Boolean, derivationType: BipDerivationType) {
        var index: Int
        val addressMap: BiMap<BtcvAddress, Int>
        if (isChangeChain) {
            index = accountContext.getLastInternalIndexWithActivity(derivationType)
            index += if (fullLookAhead) {
                INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH
            } else {
                INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH
            }
            addressMap = internalAddresses[derivationType]!!
        } else {
            index = accountContext.getLastExternalIndexWithActivity(derivationType)
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
            keyManagerMap[derivationType]!!.getAddress(isChangeChain, index)?.let {
                addressMap[it] = index
            }
            index--
        }
    }

    protected fun initAddressesMap(): MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = derivePaths
            .associateWith { HashBiMap.create<BtcvAddress, Int>() }.toMutableMap()

    private fun initSafeLastIndexes(reset: Boolean) {
        listOf(BipDerivationType.BIP44,
                BipDerivationType.BIP49,
                BipDerivationType.BIP84).forEach {
            safeLastExternalIndex[it] = if (reset) 0 else accountContext.getLastExternalIndexWithActivity(it)
            safeLastInternalIndex[it] = if (reset) 0 else accountContext.getLastInternalIndexWithActivity(it)
        }
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
        return derivePaths.any { accountContext.getLastExternalIndexWithActivity(it) != -1 }
    }

    override val dummyAddress: BtcvAddress
        get() = BtcvAddress.getNullAddress(coinType, networkParameters)

    override fun getDummyAddress(subType: String): BtcvAddress = BtcvAddress.getNullAddress(coinType, networkParameters, AddressType.valueOf(subType))

    override fun addressesList(): List<BtcvAddress> = allAddresses

    companion object {
        const val EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        const val INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        private const val EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 4
        private const val INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 1
        private val FORCED_DISCOVERY_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }

    override fun signMessage(message: String, address: Address?): String {
        return try {
            val privKey = getPrivateKeyForAddress((address as BtcAddress).address, AesKeyCipher.defaultKeyCipher())
            privKey!!.signMessage(message).base64Signature
        } catch (invalidKeyCipher: KeyCipher.InvalidKeyCipher) {
            throw java.lang.RuntimeException(invalidKeyCipher)
        }
    }

}

package com.mycelium.wapi.wallet.btcvault.hd

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.btcvault.AbstractBtcvAccount
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level


class BitcoinVaultHDAccount(protected var accountContext: BitcoinVaultHDAccountContext,
                            protected val keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
                            network: NetworkParameters,
                            wapi: Wapi,
                            val backing: BitcoinVaultHDAccountBacking,
                            private val accountListener: AccountListener?)
    : AbstractBtcvAccount(backing, network, wapi), ExportableAccount {

    protected var externalAddresses: MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = initAddressesMap()
    protected var internalAddresses: MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = initAddressesMap()
    private val safeLastExternalIndex: MutableMap<BipDerivationType, Int> = mutableMapOf()
    private val safeLastInternalIndex: MutableMap<BipDerivationType, Int> = mutableMapOf()
    private var receivingAddressMap: MutableMap<AddressType, BtcvAddress> = mutableMapOf()

    private val derivePaths = accountContext.indexesMap.keys

    val accountIndex: Int
        get() = accountContext.accountIndex


    init {
        if (!isArchived) {
            ensureAddressIndexes()
            _cachedBalance = calculateLocalBalance()
        }
        initSafeLastIndexes(false)
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: Transaction?): BroadcastResult {
        TODO("Not yet implemented")
    }

    override fun getReceiveAddress(): BtcvAddress = receivingAddressMap[accountContext.defaultAddressType]!!


    override fun getCoinType(): CryptoCurrency = accountContext.currency

    override fun getBasedOnCoinType(): CryptoCurrency = accountContext.currency

    override fun getAccountBalance(): Balance = Balance.getZeroBalance(accountContext.currency)

    override fun isMineAddress(address: Address?): Boolean = false

    override fun isExchangeable(): Boolean = true
    override fun doDiscoveryForAddresses(lookAhead: List<BitcoinAddress>?): Set<BipDerivationType?>? {
        TODO("Not yet implemented")
    }


    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> = mutableListOf()

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> = mutableListOf()

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> = mutableListOf()

    override fun getLabel(): String = accountContext.accountName

    override fun setLabel(label: String) {
        accountContext.accountName = label
    }

    override fun doSynchronization(mode: SyncMode?): Boolean {
        TODO("Not yet implemented")
    }

    private fun needsDiscovery() = !isArchived &&
            accountContext.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System.currentTimeMillis()


    private fun discovery(): Boolean {
        try {
            // discovered as in "discovered maybe something. further exploration is needed."
            // thus, method is done once discovered is empty.
            var discovered = derivePaths.toSet()
            do {
                discovered = doDiscovery(discovered)
            } while (discovered.isNotEmpty())
        } catch (e: WapiException) {
            _logger.log(Level.SEVERE, "Server connection failed with error code: " + e.errorCode, e)
//            accountListener?.postEvent(WalletManager.Event.SERVER_CONNECTION_ERROR)
            return false
        }

        accountContext.setLastDiscovery(System.currentTimeMillis())
        accountContext.persistIfNecessary(backing)
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
    private fun doDiscovery(derivePaths: Set<BipDerivationType>): Set<BipDerivationType> {
        // Ensure that all addresses in the look ahead window have been created
        ensureAddressIndexes()
//        return doDiscoveryForAddresses(derivePaths.flatMap { getAddressesToDiscover(it) })
        return setOf()
    }

    override fun getBlockChainHeight(): Int = accountContext.blockHeight

    override fun canSpend(): Boolean = true

    override fun canSign(): Boolean = true

    override fun isArchived(): Boolean = accountContext.isArchived()


    override fun isActive(): Boolean = !accountContext.isArchived()

    override fun archiveAccount() {
        accountContext.setArchived(true)
    }

    override fun activateAccount() {
        accountContext.setArchived(false)
    }

    override fun dropCachedData() {
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = accountContext.accountType == BitcoinVaultHDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED

    override fun getId(): UUID = accountContext.id

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun removeAllQueuedTransactions() {
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: BtcvAddress?): Value {
        TODO("Not yet implemented")
    }

    override fun getTypicalEstimatedTransactionSize(): Int = 0
    override fun getPrivateKey(publicKey: PublicKey?, cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        // This method should NOT be called for HD account since it has more than one private key
        throw RuntimeException("Calling getPrivateKey() is not supported for HD account")
    }

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }


    override fun onNewTransaction(t: BitcoinTransaction?) {
        TODO("Not yet implemented")
    }

    override fun setBlockChainHeight(blockHeight: Int) {
        TODO("Not yet implemented")
    }

    override fun persistContextIfNecessary() {
        TODO("Not yet implemented")
    }

    override fun getChangeAddress(destinationAddress: BitcoinAddress?): BitcoinAddress {
        TODO("Not yet implemented")
    }

    override fun getChangeAddress(destinationAddresses: List<BitcoinAddress>?): BitcoinAddress {
        TODO("Not yet implemented")
    }

    override val changeAddress: BitcoinAddress
        get() = TODO("Not yet implemented")


    override fun getPrivateKeyForAddress(address: BitcoinAddress?, cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override val availableAddressTypes: List<AddressType?>?
        get() = TODO("Not yet implemented")


    override fun setDefaultAddressType(addressType: AddressType?) {
        TODO("Not yet implemented")
    }

    override fun getPublicKeyForAddress(address: BitcoinAddress?): PublicKey {
        TODO("Not yet implemented")
    }

    override fun isOwnExternalAddress(address: BitcoinAddress?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isValidEncryptionKey(cipher: KeyCipher?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        val privateDataMap = if (canSpend()) {
            try {
                keyManagerMap.keys.map { derivationType ->
                    derivationType to (keyManagerMap[derivationType]!!.getPrivateAccountRoot(cipher, derivationType)
                            .serialize(network, derivationType))
                }.toMap()
            } catch (ignore: KeyCipher.InvalidKeyCipher) {
                null
            }
        } else {
            null
        }
        val publicDataMap = keyManagerMap.keys.map { derivationType ->
            derivationType to (keyManagerMap[derivationType]!!.publicAccountRoot
                    .serialize(network, derivationType))
        }.toMap()
        return ExportableAccount.Data(privateDataMap, publicDataMap)
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
//                postEvent(WalletManager.Event.RECEIVING_ADDRESS_CHANGED)
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
            //TODO redefine Address Type
            addressMap[BtcvAddress(coinType, keyManagerMap[derivationType]!!.getAddress(isChangeChain, index).toString())] = index
            index--
        }
    }

    protected fun initAddressesMap(): MutableMap<BipDerivationType, BiMap<BtcvAddress, Int>> = derivePaths
            .map { it to HashBiMap.create<BtcvAddress, Int>() }.toMap().toMutableMap()

    private fun initSafeLastIndexes(reset: Boolean) {
        listOf(BipDerivationType.BIP44,
                BipDerivationType.BIP49,
                BipDerivationType.BIP84).forEach {
            safeLastExternalIndex[it] = if (reset) 0 else accountContext.getLastExternalIndexWithActivity(it)
            safeLastInternalIndex[it] = if (reset) 0 else accountContext.getLastInternalIndexWithActivity(it)
        }
    }

    companion object {
        const val EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        const val INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20
        private const val EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 4
        private const val INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 1
        private const val INTERNAL_MINIMAL_ADDRESS_LOOK_BACK_LENGTH = 2
        private const val EXTERNAL_MINIMAL_ADDRESS_LOOK_BACK_LENGTH = 3
        private val FORCED_DISCOVERY_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)
    }
}
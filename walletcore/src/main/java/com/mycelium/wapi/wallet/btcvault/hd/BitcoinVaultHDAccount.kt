package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


class BitcoinVaultHDAccount(protected var accountContext: BitcoinVaultHDAccountContext,
                            protected val keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
                            val network: NetworkParameters,
                            val backing: BitcoinVaultHDAccountBacking,
                            private val accountListener: AccountListener?) : WalletAccount<BtcvAddress>, ExportableAccount {

    var accountName = ""
    protected val _logger = Logger.getLogger(javaClass.simpleName)
    protected var syncTotalRetrievedTxs = 0
    private var receivingAddressMap: MutableMap<AddressType, BtcvAddress> = mutableMapOf()

    private val derivePaths = accountContext.indexesMap.keys

    @Volatile
    protected var syncing = false

    val accountIndex: Int
        get() = accountContext.accountIndex

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: Address?, amount: Value?, fee: Fee?, data: TransactionData?): Transaction {
        TODO("Not yet implemented")
    }

    override fun signTx(request: Transaction?, keyCipher: KeyCipher?) {
        TODO("Not yet implemented")
    }

    override fun broadcastTx(tx: Transaction?): BroadcastResult {
        TODO("Not yet implemented")
    }

    override fun getReceiveAddress(): Address = BtcvAddress(coinType, "RNpmnbN2ystNC9y4H5v1Gk5yi7VVWLHccR")


    override fun getCoinType(): CryptoCurrency = accountContext.currency

    override fun getBasedOnCoinType(): CryptoCurrency = accountContext.currency

    override fun getAccountBalance(): Balance = Balance.getZeroBalance(accountContext.currency)

    override fun isMineAddress(address: Address?): Boolean = false

    override fun isExchangeable(): Boolean = true

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary? {
        checkNotArchived()
        return backing.getTransactionSummary(HexUtils.toHex(transactionId))
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> = mutableListOf()

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> = mutableListOf()

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> = mutableListOf()

    override fun getLabel(): String = accountName

    override fun setLabel(label: String) {
        accountName = label
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean {
        TODO("Not yet implemented")
    }

    override fun synchronize(proposedMode: SyncMode): Boolean {
        var mode = proposedMode
        checkNotArchived()
        syncing = true
        syncTotalRetrievedTxs = 0
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
//            return updateUnspentOutputs(mode)
        } finally {
            syncTotalRetrievedTxs = 0
        }

        //TODO  need implementation
        syncing = false
        return true
    }

    private fun needsDiscovery() = !isArchived &&
            accountContext.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System.currentTimeMillis()


    private fun discovery(): Boolean {
        try {
            // discovered as in "discovered maybe something. further exploration is needed."
            // thus, method is done once discovered is empty.
            var discovered = derivePaths.toSet()
            do {
//                discovered = doDiscovery(discovered)
            } while (discovered.isNotEmpty())
        } catch (e: WapiException) {
            _logger.log(Level.SEVERE, "Server connection failed with error code: " + e.errorCode, e)
//            accountListener?.postEvent(WalletManager.Event.SERVER_CONNECTION_ERROR)
            return false
        }

        accountContext.setLastDiscovery(System.currentTimeMillis())
//        accountContext.persistIfNecessary(backing)
        return true
    }

    override fun getBlockChainHeight(): Int = accountContext.blockHeight

    override fun canSpend(): Boolean = true

    override fun canSign(): Boolean = true

    override fun isSyncing(): Boolean = syncing

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

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun getTypicalEstimatedTransactionSize(): Int = 0

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): BtcvAddress {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(subType: String?): BtcvAddress {
        TODO("Not yet implemented")
    }

    override fun getDependentAccounts(): MutableList<WalletAccount<Address>> = mutableListOf()

    override fun queueTransaction(transaction: Transaction) {
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

    protected fun checkNotArchived() {
        val usingArchivedAccount = "Using archived account"
        if (isArchived) {
            _logger.log(Level.SEVERE, usingArchivedAccount)
            throw RuntimeException(usingArchivedAccount)
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
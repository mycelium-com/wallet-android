package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAccountBacking
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


class CoinapultAccount(val context: CoinapultAccountContext, val accountKey: InMemoryPrivateKey
                       , val api: CoinapultApi
                       , val accountBacking: CoinapultAccountBacking
                       , val backing: WalletBacking<CoinapultAccountContext>
                       , val _network: NetworkParameters
                       , val currency: Currency
                       , val listener: AccountListener?)
    : WalletAccount<BtcAddress> {


    override fun queueTransaction(transaction: GenericTransaction) {
    }

    override fun getBasedOnCoinType(): CryptoCurrency {
        return coinType
    }

    override fun getDependentAccounts(): List<WalletAccount<*>> {
        return emptyList()
    }

    override fun getTransactions(offset: Int, limit: Int): MutableList<GenericTransaction> {
        // Coinapult is currently disabled
        return ArrayList<GenericTransaction>()
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTx(transactionId: ByteArray): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction? {
        return null;
    }

    override fun isSyncing(): Boolean {
        return false
    }

    override fun getDummyAddress(subType: String?): BtcAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress(): BtcAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isExchangeable(): Boolean {
        return true
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        return accountKey
    }

    var accountLabel: String = ""

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
        // Coinapult is currently disabled
        val history = ArrayList<GenericTransactionSummary>()
        return history
    }

    override fun getLabel(): String {
        return accountLabel
    }

    override fun setLabel(label: String?) {
        accountLabel = label.toString()
    }

    val uuid: UUID = CoinapultUtils.getGuidForAsset(currency, accountKey.publicKey.publicKeyBytes)
    protected var cachedBalance = Balance(Value.zeroValue(coinType), Value.zeroValue(coinType)
            , Value.zeroValue(coinType), Value.zeroValue(coinType))
    @Volatile
    protected var _isSynchronizing: Boolean = false

    var address: GenericAddress? = context.address

    override fun getId() = uuid

    override fun getReceiveAddress(): GenericAddress = address!!

    override fun isMineAddress(address: GenericAddress?): Boolean = receiveAddress == address

    override fun getTxSummary(transactionId: ByteArray): GenericTransactionSummary? {
        return null
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): List<GenericTransactionSummary> {
        return ArrayList<GenericTransactionSummary>()
    }

    override fun isActive() = !context.isArchived()

    override fun archiveAccount() {
        context.setArchived(true)
        backing.updateAccountContext(context)
    }

    override fun broadcastOutgoingTransactions(): Boolean = true

    override fun isArchived() = context.isArchived()

    protected fun checkNotArchived() {
        val usingArchivedAccount = "Using archived account"
        if (isArchived) {
            throw RuntimeException(usingArchivedAccount)
        }
    }

    override fun activateAccount() {
        context.setArchived(false)
        backing.updateAccountContext(context)
    }

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun dropCachedData() {
    }

    override fun isSynchronizing(): Boolean = _isSynchronizing

    override fun getCoinType(): CryptoCurrency = currency

    override fun getBlockChainHeight(): Int = 0

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddress: BtcAddress): Value {
        return Value.zeroValue(if (_network.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFeeEstimations(): FeeEstimationsGeneric {
        return FeeEstimationsGeneric(Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType), System.currentTimeMillis())
    }

    override fun getCachedFeeEstimations() = getFeeEstimations()

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun isVisible(): Boolean = true

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        // Coinapult is currently disabled
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        // Coinapult is currently disabled
        return BroadcastResult(BroadcastResultType.REJECTED)
    }

    override fun getAccountBalance(): Balance = cachedBalance

    override fun synchronize(mode: SyncMode?): Boolean {
        _isSynchronizing = true
        // Coinapult is currently disabled
        _isSynchronizing = false
        return true
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        return 0
    }

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        return mutableListOf()
    }
}
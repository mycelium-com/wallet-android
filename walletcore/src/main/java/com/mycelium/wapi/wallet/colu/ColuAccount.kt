package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutputList
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputSummary
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.SynchronizeAbleWalletBtcAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue


class ColuAccount(val context: ColuAccountContext, val accountKey: InMemoryPrivateKey)
    : SynchronizeAbleWalletBtcAccount() {

    @Volatile
    private var _isSynchronizing: Boolean = false

    override fun getTransactions(offset: Int, limit: Int): MutableList<GenericTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isActive() = !context.isArchived()

    override fun broadcastOutgoingTransactions(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTransaction(transactionId: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBalance(): BalanceSatoshis {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeAndSignTx(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAccountBalance(): Balance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun archiveAccount() = context.setArchived(true)

    override fun getTransactionEx(txid: Sha256Hash?): TransactionEx {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTransaction(transaction: Transaction?): WalletBtcAccount.BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queueTransaction(transaction: TransactionEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNetwork(): NetworkParameters {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrencyBasedBalance(): CurrencyBasedBalance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnspentTransactionOutputSummary(): MutableList<TransactionOutputSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDerivedFromInternalMasterseed(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMine(address: Address?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTransaction(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCoinType(): CryptoCurrency {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dropCachedData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceivingAddress(): Optional<Address> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createUnsignedPop(txid: Sha256Hash?, nonce: ByteArray?): UnsignedTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun doSynchronization(mode: SyncMode?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isValidEncryptionKey(cipher: KeyCipher?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAccountDefaultCurrency(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long): ExactCurrencyValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeTransaction(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: BtcTransaction?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransaction(transactionId: Sha256Hash?): BtcTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: GenericAddress?, amount: Value?): SendRequest<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOwnExternalAddress(address: Address?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSyncTotalRetrievedTransactions(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun cancelQueuedTransaction(transaction: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend(): Boolean {
        return accountKey.
    }

    override fun isArchived() = context.isArchived()

    override fun activateAccount() = context.setArchived(false)

    override fun isSynchronizing() = _isSynchronizing

    override fun isOwnInternalAddress(address: Address?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTransaction(unsigned: UnsignedTransaction?, cipher: KeyCipher?): Transaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createUnsignedTransaction(receivers: MutableList<WalletAccount.Receiver>?, minerFeeToUse: Long): UnsignedTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createUnsignedTransaction(outputs: OutputList?, minerFeeToUse: Long): UnsignedTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionHistory(offset: Int, limit: Int): MutableList<TransactionSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionsSince(receivingSince: Long?): MutableList<TransactionSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummary(txid: Sha256Hash?): TransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
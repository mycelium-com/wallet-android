package com.mycelium.wapi.wallet.coinapult

import com.google.common.base.Optional
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutputList
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.*
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.SynchronizeAbleWalletBtcAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue
import java.nio.ByteBuffer
import java.util.*


class CoinapultAccount(val context: CoinapultAccountContext, val accountKey: InMemoryPrivateKey
                       , val _network: NetworkParameters, coinapultCurrency: Currency)
    : SynchronizeAbleWalletBtcAccount() {

    override fun getCoinType(): CryptoCurrency {
        if (_network.isProdnet) {
            return BitcoinMain.get()
        }
        return BitcoinTest.get()
    }

    override fun getType(): WalletBtcAccount.Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionDetails(txid: Sha256Hash?): TransactionDetails {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress(): GenericAddress {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMineAddress(address: GenericAddress?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTx(transactionId: Sha256Hash?): BtcTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val uuid: UUID

    init {
        val byteWriter = ByteWriter(36)
        byteWriter.putBytes(accountKey.publicKey.publicKeyBytes)
        byteWriter.putRawStringUtf8(coinapultCurrency.name)
        val accountId = HashUtils.sha256(byteWriter.toBytes())
        uuid = getGuidFromByteArray(accountId.bytes)
    }

    private fun getGuidFromByteArray(bytes: ByteArray): UUID {
        val bb = ByteBuffer.wrap(bytes)
        val high = bb.long
        val low = bb.long
        return UUID(high, low)
    }

    override fun getId() = uuid

    override fun getTransactions(offset: Int, limit: Int): List<BtcTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isActive() = !context.isArchived()

    override fun archiveAccount() {
        context.setArchived(true)
    }

    override fun broadcastOutgoingTransactions(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTransaction(transaction: Transaction?): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun queueTransaction(transaction: TransactionEx?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUnspentTransactionOutputSummary(): MutableList<TransactionOutputSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransaction(transactionId: Sha256Hash?): TransactionEx? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isArchived() = context.isArchived()

    override fun activateAccount() {
        context.setArchived(false)
    }

    override fun isDerivedFromInternalMasterseed(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrencyBasedBalance(): CurrencyBasedBalance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isMine(address: Address?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeAndSignTx(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dropCachedData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAccountDefaultCurrency(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isSynchronizing(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceivingAddress(): Optional<Address> = Optional.absent()

    override fun isOwnInternalAddress(address: Address?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteTransaction(transactionId: Sha256Hash?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getNetwork() = _network

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
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

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long): ExactCurrencyValue {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeTransaction(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTransaction(request: SendRequest<BtcTransaction>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: BtcTransaction?): BroadcastResult? {
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

    override fun getAccountBalance(): Balance {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBalance(): BalanceSatoshis {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import fiofoundation.io.fiosdk.toSUF
import org.web3j.crypto.Credentials
import java.util.*

class FioAccount(private val fioKeyManager: FioKeyManager, private val fiosdk: FIOSDK, val credentials: Credentials) : WalletAccount<FioAddress> {
    private var balance: Balance =  Balance(Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain))

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        if (amount > calculateMaxSpendableAmount((fee as FeePerKbFee).feePerKb, address as FioAddress)) {
            throw BuildTransactionException(Throwable("Invalid amount"))
        }

        return FioTransaction(coinType, address.toString(), amount, "0.2".toSUF())
    }

    override fun signTx(request: Transaction?, keyCipher: KeyCipher?) {
    }

    override fun broadcastTx(tx: Transaction?): BroadcastResult {
        val fioTx = tx as FioTransaction
        return try {
            val response = fiosdk.transferTokens(fioTx.toAddress, fioTx.value.value, fioTx.fee)
            val actionTraceResponse = response.getActionTraceResponse()
            if (actionTraceResponse != null && actionTraceResponse.status == "OK") {
                BroadcastResult(BroadcastResultType.SUCCESS)
            } else {
                BroadcastResult("Status: ${actionTraceResponse?.status}", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
        } catch (e: FIOError) {
            e.printStackTrace()
            BroadcastResult(e.toJson(), BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        } catch (e: Exception) {
            e.printStackTrace()
            BroadcastResult(e.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        }
    }

    override fun getReceiveAddress(): Address {
        return FioAddress(FIOMain, FioAddressData(fiosdk.publicKey))
    }

    override fun getCoinType(): CryptoCurrency {
        return FIOMain
    }

    override fun getBasedOnCoinType(): CryptoCurrency {
        return FIOMain
    }

    override fun getAccountBalance(): Balance {
        return balance
    }

    override fun isMineAddress(address: Address?): Boolean = address == receiveAddress


    override fun isExchangeable(): Boolean {
        return true
    }

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary {
        TODO("Not yet implemented")
    }

    override fun getTransactionSummaries(offset: Int, limit: Int): MutableList<TransactionSummary> {
        return mutableListOf()
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<TransactionSummary> {
        return mutableListOf()
    }

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        return mutableListOf()
    }

    override fun getLabel(): String {
        return "FIO"
    }

    override fun setLabel(label: String?) {
        "FIO label"
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean {
        return true
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val fioBalance = fiosdk.getFioBalance()
        balance =  Balance(Value.valueOf(FIOMain, fioBalance.balance), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain), Value.zeroValue(FIOMain))
        return true
    }

    override fun getBlockChainHeight(): Int {
        TODO("Not yet implemented")
    }

    override fun canSpend(): Boolean {
       return true
    }

    override fun canSign(): Boolean {
        return false
    }

    override fun isSyncing(): Boolean {
        return false
    }

    override fun isArchived(): Boolean {
        return false
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun archiveAccount() {
        TODO("Not yet implemented")
    }

    override fun activateAccount() {

    }

    override fun dropCachedData() {

    }

    override fun isVisible(): Boolean {
        return true
    }

    override fun isDerivedFromInternalMasterseed(): Boolean {
        return true
    }

    override fun getId(): UUID = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receiveAddress.getBytes())


    override fun broadcastOutgoingTransactions(): Boolean {
        return true
    }

    override fun removeAllQueuedTransactions() {

    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: FioAddress?): Value {
        return balance.spendable - (minerFeePerKilobyte ?: Value.zeroValue(coinType))
    }

    override fun getSyncTotalRetrievedTransactions(): Int {
        return 0
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        return 0
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("Not yet implemented")
    }

    override fun getDummyAddress(): FioAddress = FioAddress(coinType, FioAddressData(""))

    override fun getDummyAddress(subType: String?): FioAddress = dummyAddress

    override fun getDependentAccounts(): MutableList<WalletAccount<Address>> {
       return mutableListOf()
    }

    override fun queueTransaction(transaction: Transaction) {
        TODO("Not yet implemented")
    }

    fun getTransferTokensFee() = fiosdk.getFee(FIOApiEndPoints.FeeEndPoint.TransferTokens).fee
}
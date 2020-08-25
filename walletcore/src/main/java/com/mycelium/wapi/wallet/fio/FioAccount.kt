package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import fiofoundation.io.fiosdk.models.fionetworkprovider.response.GetFIONamesResponse
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class FioAccount(private val accountContext: FioAccountContext,
                 private val accountListener: AccountListener?,
                 private val fiosdk: FIOSDK) : WalletAccount<FioAddress> {
    private val logger: Logger = Logger.getLogger("asdaf")

    //TODO
    val maxFee = BigInteger.ZERO
    private lateinit var label: String

    fun registerFIOAddress(fioAddress: String) {
        fiosdk.registerFioAddress(fioAddress, maxFee)
    }

    fun registerFioDomain(fioDomain: String) {
        fiosdk.registerFioDomain(fioDomain, maxFee)
    }

    fun getFioNames(): GetFIONamesResponse {
        return fiosdk.getFioNames()
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("Not yet implemented")
    }

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        if (amount > calculateMaxSpendableAmount((fee as FeePerKbFee).feePerKb, address as FioAddress)) {
            throw BuildTransactionException(Throwable("Invalid amount"))
        }

        logger.log(Level.INFO, "asdaf fee: ${fee.feePerKb}")
        return FioTransaction(coinType, address.toString(), amount, fee.feePerKb.value)
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

    override fun getReceiveAddress(): Address = FioAddress(coinType, FioAddressData(fiosdk.publicKey))

    override fun getCoinType(): CryptoCurrency = accountContext.currency

    override fun getBasedOnCoinType(): CryptoCurrency = coinType

    override fun getAccountBalance(): Balance = accountContext.balance

    override fun isMineAddress(address: Address?): Boolean = address == receiveAddress

    override fun isExchangeable(): Boolean = true

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

    override fun getLabel(): String = accountContext.accountName

    override fun setLabel(label: String?) {
        this.label = label ?: "FIO"
    }

    override fun isSpendingUnconfirmed(tx: Transaction?): Boolean = false

    override fun synchronize(mode: SyncMode?): Boolean {
        val fioBalance = fiosdk.getFioBalance()
        val newBalance = Balance(Value.valueOf(coinType, fioBalance.balance),
                Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return true
    }

    override fun getBlockChainHeight(): Int = accountContext.blockHeight

    override fun canSpend(): Boolean = true

    override fun canSign(): Boolean = false

    override fun isSyncing(): Boolean {
        return false
    }

    override fun isArchived(): Boolean = accountContext.archived

    override fun isActive(): Boolean = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        accountContext.balance = Balance.getZeroBalance(coinType)
    }

    override fun isVisible(): Boolean = true

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions(): Boolean = true

    override fun removeAllQueuedTransactions() {
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: FioAddress?): Value {
        return accountBalance.spendable - (minerFeePerKilobyte ?: Value.zeroValue(coinType))
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
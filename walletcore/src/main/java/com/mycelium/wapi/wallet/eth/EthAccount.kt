package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.max
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import io.reactivex.disposables.Disposable
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread


class EthAccount(private val accountContext: EthAccountContext,
                 private val credentials: Credentials? = null,
                 private val backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 web3jService: HttpService,
                 address: EthAddress? = null) : WalletAccount<EthAddress> {
    private val web3j = Web3j.build(web3jService)
    private val logger = Logger.getLogger(EthBalanceService::javaClass.name)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!
    private var pendingTxDisposable: Disposable? = null

    init {
        pendingTxDisposable = subscribeOnPendingIncomingTx()
    }

    fun syncWithRemote() {
        EtherscanIoFetcher.syncWithRemote(coinType, receivingAddress.addressString, backing) {
            accountListener?.balanceUpdated(this)
        }
    }

    // save incoming tx we detected to the txs table
    private fun subscribeOnPendingIncomingTx(): Disposable {
        return web3j.pendingTransactionFlowable()
                .subscribe({ tx ->
                    if (tx.to == receivingAddress.addressString) {
                        backing.putTransaction(-1, System.currentTimeMillis() / 1000, tx.hash,
                                tx.raw, tx.from, receivingAddress.addressString, valueOf(coinType, tx.value),
                                valueOf(coinType, tx.gasPrice * typicalEstimatedTransactionSize.toBigInteger()), 0)
                    }
                }, {})
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    @Throws(GenericInsufficientFundsException::class, GenericBuildTransactionException::class)
    override fun createTx(toAddress: GenericAddress, value: Value, gasPrice: GenericFee): GenericTransaction {
        val gasPriceValue = (gasPrice as FeePerKbFee).feePerKb
        if (gasPriceValue.value <= BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value <= BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Value should be positive and non-zero"))
        }
        // check whether account has enough funds
        if (value > calculateMaxSpendableAmount(gasPriceValue, null)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPriceValue.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }

        try {
            val nonce = getNonce(receivingAddress)
            val rawTransaction = RawTransaction.createEtherTransaction(nonce,
                    gasPrice.feePerKb.value, BigInteger.valueOf(typicalEstimatedTransactionSize.toLong()),
                    toAddress.toString(), value.value)
            return EthTransaction(coinType, toAddress, value, gasPrice, rawTransaction)
        } catch (e: Exception) {
            throw GenericBuildTransactionException(Throwable(e.localizedMessage))
        }
    }

    @Throws(Exception::class)
    private fun getNonce(address: EthAddress): BigInteger {
        return try {
            val ethGetTransactionCount = web3j.ethGetTransactionCount(address.toString(),
                    DefaultBlockParameterName.PENDING)
                    .send()

            accountContext.nonce = ethGetTransactionCount.transactionCount
            accountContext.nonce
        } catch (e: Exception) {
            accountContext.nonce
        }
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        val rawTransaction = (request as EthTransaction).rawTransaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.signedHex = hexValue
        request.txHash = TransactionUtils.generateTransactionHash(rawTransaction, credentials)
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val ethSendTransaction = web3j.ethSendRawTransaction((tx as EthTransaction).signedHex).send()
        if (ethSendTransaction.hasError()) {
            return BroadcastResult(ethSendTransaction.error.message, BroadcastResultType.REJECTED)
        }
        backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                tx.signedHex!!, receivingAddress.addressString, tx.toAddress.toString(),
                tx.value, (tx.gasPrice as FeePerKbFee).feePerKb * typicalEstimatedTransactionSize.toBigInteger(), 0)
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getCoinType() = accountContext.currency

    override fun getBasedOnCoinType() = coinType

    private val ethBalanceService = EthBalanceService(receivingAddress.toString(), coinType, web3jService)

    private var balanceDisposable: Disposable = subscribeOnBalanceUpdates()

    override fun getAccountBalance() = accountContext.balance

    override fun isMineAddress(address: GenericAddress?) =
            address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary =
            backing.getTransactionSummary("0x" + HexUtils.toHex(transactionId), receivingAddress.addressString)!!

    override fun getTransactionSummaries(offset: Int, limit: Int) =
            backing.getTransactionSummaries(offset.toLong(), limit.toLong(), receivingAddress.addressString)

    override fun getTransactionsSince(receivingSince: Long) = emptyList<GenericTransactionSummary>()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel() = accountContext.accountName

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return false
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val succeed = ethBalanceService.updateBalanceCache()
        if (succeed) {
            accountContext.balance = ethBalanceService.balance
            accountListener?.balanceUpdated(this)
            if (balanceDisposable.isDisposed) {
                balanceDisposable = subscribeOnBalanceUpdates()
            }
            renewSubscriptions()
        }
        return succeed
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = credentials != null

    override fun isSyncing() = false

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
        stopSubscriptions()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        accountContext.balance = Balance.getZeroBalance(coinType)
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override fun getId() = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

    override fun isSynchronizing() = false

    override fun broadcastOutgoingTransactions() = true

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(gasPrice: Value, ign: EthAddress?): Value {
        val spendable = accountBalance.spendable - gasPrice * typicalEstimatedTransactionSize.toLong()
        return max(spendable, Value.zeroValue(coinType))
    }

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress() = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String?): EthAddress = dummyAddress

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun renewSubscriptions() {
        if (pendingTxDisposable?.isDisposed == true) {
            pendingTxDisposable = subscribeOnPendingIncomingTx()
        }
    }

    fun stopSubscriptions() {
        thread {
            balanceDisposable.dispose()
            pendingTxDisposable?.dispose()
        }
    }

    private fun subscribeOnBalanceUpdates(): Disposable {
        return ethBalanceService.balanceFlowable.subscribe({ balance ->
            accountContext.balance = balance
            accountListener?.balanceUpdated(this)
        }, {
            logger.log(Level.SEVERE, "Error synchronizing ETH, $it")
        })
    }
}

fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))
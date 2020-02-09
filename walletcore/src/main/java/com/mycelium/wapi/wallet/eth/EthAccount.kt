package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.ServerEndpoints
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
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.max

class EthAccount(private val accountContext: EthAccountContext,
                 private val credentials: Credentials? = null,
                 private val backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 endpoints: List<HttpEndpoint>,
                 address: EthAddress? = null) : WalletAccount<EthAddress>, ServerEthListChangedListener {
    private var endpoints = ServerEndpoints(endpoints.toTypedArray())
    private val logger = Logger.getLogger(EthBalanceService::class.simpleName)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!

    var enabledTokens: MutableList<String> = accountContext.enabledTokens?.toMutableList()
            ?: mutableListOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    lateinit var client: Web3j

    init {
        updateClient()
    }

    private fun buildCurrentEndpoint() = Web3j.build(HttpService(endpoints.currentEndpoint.baseUrl))

    private fun updateClient() {
        client = buildCurrentEndpoint()
    }

    fun removeEnabledToken(tokenName: String) {
        enabledTokens.remove(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun addEnabledToken(tokenName: String) {
        enabledTokens.add(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun isEnabledToken(tokenName: String) = enabledTokens.contains(tokenName)

    fun hasHadActivity(): Boolean {
        return accountBalance.spendable.isPositive() || accountContext.nonce > BigInteger.ZERO
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
            val ethGetTransactionCount = client.ethGetTransactionCount(address.toString(),
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
        val ethSendTransaction = client.ethSendRawTransaction((tx as EthTransaction).signedHex).send()
        if (ethSendTransaction.hasError()) {
            return BroadcastResult(ethSendTransaction.error.message, BroadcastResultType.REJECTED)
        }
        backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                tx.signedHex!!, receivingAddress.addressString, tx.toAddress.toString(),
                tx.value, (tx.gasPrice as FeePerKbFee).feePerKb * typicalEstimatedTransactionSize.toBigInteger(), 0, accountContext.nonce)
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getCoinType() = accountContext.currency

    override fun getBasedOnCoinType() = coinType

    private val ethBalanceService = EthBalanceService(receivingAddress.toString(), coinType, client, this.endpoints)

    private var balanceDisposable: Disposable = subscribeOnBalanceUpdates()

    private var incomingTxsDisposable: Disposable = subscribeOnIncomingTx()

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

    override fun getTransactionsSince(receivingSince: Long) =
            backing.getTransactionSummariesSince(receivingSince / 1000, receivingAddress.addressString)

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
        if (!selectEndpoint()) {
            return false
        }
        if (!ethBalanceService.updateBalanceCache()) {
            return false
        }
        accountContext.balance = ethBalanceService.balance
        accountListener?.balanceUpdated(this)

        if (!syncBlockHeight()) {
            return false
        }

        if (!syncTransactions()) {
            return false
        }

        renewSubscriptions()
        return true
    }

    private fun selectEndpoint(): Boolean {
        val currentEndpointIndex = endpoints.currentEndpointIndex
        for (x in 0 until endpoints.size()) {
            val ethUtils = EthSyncChecker(client)
            try {
                if (ethUtils.isSynced) {
                    if (currentEndpointIndex != endpoints.currentEndpointIndex) {
                        ethBalanceService.client = client
                        balanceDisposable = subscribeOnBalanceUpdates()
                        incomingTxsDisposable = subscribeOnIncomingTx()
                    }
                    return true
                }
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Error synchronizing ETH, $ex")
                logger.log(Level.SEVERE, "Switching to next endpoint...")
            }
            endpoints.switchToNextEndpoint()
            updateClient()
        }
        return false
    }

    override fun getBlockChainHeight(): Int {
        return accountContext.blockHeight
    }

    override fun canSpend() = credentials != null

    override fun isSyncing() = false

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
        stopSubscriptions(newThread = true)
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

    override fun getId(): UUID = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

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

    private fun syncBlockHeight(): Boolean {
        try {
            val latestBlock = client.ethBlockNumber().send()
            if (latestBlock.hasError()) {
                return false
            }
            accountContext.blockHeight = latestBlock.blockNumber.toInt()
            return true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH, ${e.localizedMessage}")
            return false
        }
    }

    /**
     * during this sync we update transactions confirmations number
     * and check for local transactions that have been created more than 5 minutes ago
     * but are missing on the server and remove them from local db
     * assuming the transactions haven't been propagated (transaction queueing for eth not supported yet)
     */
    private fun syncTransactions(): Boolean {
        val localTransactions = backing.getTransactionSummaries(0, Long.MAX_VALUE,
                receivingAddress.addressString)
        localTransactions.forEach {
            try {
                val remoteTx = client.ethGetTransactionByHash("0x" + it.idHex).send()
                if (!remoteTx.hasError()) {
                    if (remoteTx.result != null) {
                        // blockNumber is not null when transaction is confirmed
                        // https://github.com/ethereum/wiki/wiki/JSON-RPC#returns-28
                        if (remoteTx.result.blockNumberRaw != null) {
                            // "it.height == -1" indicates that this is a newly created transaction
                            // and we haven't received any information about it's confirmation from the server yet
                            if (it.height == -1) { // update gasUsed only once when tx has just been confirmed
                                val txReceipt = client.ethGetTransactionReceipt("0x" + it.idHex).send()
                                if (!txReceipt.hasError()) {
                                    val newFee = valueOf(coinType, remoteTx.result.gasPrice * txReceipt.result.gasUsed)
                                    backing.updateGasUsed("0x" + it.idHex, txReceipt.result.gasUsed, newFee)
                                }
                            }
                            val confirmations = if (it.height != -1) accountContext.blockHeight - it.height
                                                else max(0, accountContext.blockHeight - remoteTx.result.blockNumber.toInt())
                            backing.updateTransaction("0x" + it.idHex, remoteTx.result.blockNumber.toInt(), confirmations)
                        }
                    } else {
                        // no such transaction on remote, remove local transaction but only if it is older 5 minutes
                        // to prevent local data removal if server still didn't process just sent tx
                        if (System.currentTimeMillis() - it.timestamp >= TimeUnit.MINUTES.toMillis(5)) {
                            backing.deleteTransaction("0x" + it.idHex)
                        }
                    }
                } else {
                    return false
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error synchronizing ETH, ${e.localizedMessage}")
                return false
            }
        }
        return true
    }

    private fun subscribeOnBalanceUpdates(): Disposable {
        return ethBalanceService.balanceFlowable.subscribeOn(Schedulers.io()).subscribe({ balance ->
            accountContext.balance = balance
            accountListener?.balanceUpdated(this)
        }, {
            logger.log(Level.SEVERE, "Error synchronizing ETH, $it")
        })
    }

    private fun subscribeOnIncomingTx(): Disposable {
        return ethBalanceService.incomingTxsFlowable.subscribeOn(Schedulers.io()).subscribe({ tx ->
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, tx.hash,
                    tx.raw, tx.from, receivingAddress.addressString, valueOf(coinType, tx.value),
                    valueOf(coinType, tx.gasPrice * typicalEstimatedTransactionSize.toBigInteger()), 0, tx.nonce, tx.gas)
        }, {})
    }

    private fun renewSubscriptions() {
        updateClient()
        if (balanceDisposable.isDisposed) {
            balanceDisposable = subscribeOnBalanceUpdates()
        }
        if (incomingTxsDisposable.isDisposed) {
            incomingTxsDisposable = subscribeOnIncomingTx()
        }
    }

    //to avoid io.reactivex.exceptions.UndeliverableException (inside android.os.NetworkOnMainThreadException)
    //we have to stop subscriptions in another thread
    //but if we want to restart subscriptions we have to stop it synchronously before subscribe again
    fun stopSubscriptions(newThread: Boolean = true) {
        if (newThread) {
            thread {
                stopSubscriptions()
            }
        } else {
            stopSubscriptions()
        }
    }

    private fun stopSubscriptions() {
        client.shutdown()
        if (!balanceDisposable.isDisposed) {
            balanceDisposable.dispose()
        }
        if (!incomingTxsDisposable.isDisposed) {
            incomingTxsDisposable.dispose()
        }
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = ServerEndpoints(newEndpoints)
        updateClient()
        thread {
            stopSubscriptions(newThread = false)
            renewSubscriptions()
        }
    }

    fun fetchNonce(txid: String): BigInteger? {
        return try {
            val tx = client.ethGetTransactionByHash(txid).send()
            if (tx.result == null) {
                null
            } else {
                val nonce = tx.result.nonce
                backing.updateNonce(txid, nonce)
                nonce
            }
        } catch (e: Exception) {
            null
        }
    }
}


fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))
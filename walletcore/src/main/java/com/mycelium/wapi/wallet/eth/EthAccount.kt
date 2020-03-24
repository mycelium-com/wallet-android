package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.HttpsEndpoint
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
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class EthAccount(private val accountContext: EthAccountContext,
                 private val credentials: Credentials? = null,
                 private val backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 endpoints: List<HttpEndpoint>,
                 private val transactionServiceEndpoints: List<HttpsEndpoint>,
                 address: EthAddress? = null) : WalletAccount<EthAddress>, ServerEthListChangedListener {
    private var endpoints = ServerEndpoints(endpoints.toTypedArray())
    private val logger = Logger.getLogger(EthBalanceService::class.simpleName)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!
    lateinit var client: Web3j
    private var isSyncing = false

    init {
        updateClient()
    }

    private fun buildCurrentEndpoint() = Web3j.build(HttpService(endpoints.currentEndpoint.baseUrl))

    private fun updateClient() {
        client = buildCurrentEndpoint()
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    @Throws(GenericInsufficientFundsException::class, GenericBuildTransactionException::class)
    override fun createTx(toAddress: GenericAddress, value: Value, gasPrice: GenericFee, data: GenericTransactionData?): GenericTransaction {
        val gasPriceValue = (gasPrice as FeePerKbFee).feePerKb
        if (gasPriceValue.value <= BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value < BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Value should be positive"))
        }
        // check whether account has enough funds
        if (value > calculateMaxSpendableAmount(gasPriceValue, null)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPriceValue.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }

        try {
            val ethTxData = (data as? EthTransactionData)
            val nonce = ethTxData?.nonce ?: getNonce(receivingAddress)
            val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(typicalEstimatedTransactionSize.toLong())
            val inputData = ethTxData?.inputData ?: ""
            val rawTransaction = RawTransaction.createTransaction(nonce,
                    gasPrice.feePerKb.value, gasLimit, toAddress.toString(), value.value, inputData)
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
        val transactionManager = RawTransactionManager(client, credentials)
        val ethSendTransaction = transactionManager.signAndSend((tx as EthTransaction).rawTransaction)
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

    private val ethBalanceService = EthBalanceService(receivingAddress.toString(), coinType, client)

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

    fun deleteTransaction(txid: String) {
        backing.deleteTransaction(txid)
        updateBalanceCache()
    }

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
        isSyncing = true
        if (!selectEndpoint()) {
            isSyncing = false
            return false
        }

        if (!syncBlockHeight()) {
            isSyncing = false
            return false
        }

        syncTransactions()
        updateBalanceCache()
        isSyncing = false
        return true
    }

    private fun updateBalanceCache(): Boolean {
        ethBalanceService.updateBalanceCache()
        var newBalance = ethBalanceService.balance

        val pendingReceiving = getPendingReceiving()
        val pendingSending = getPendingSending()
        newBalance = Balance(valueOf(coinType, newBalance.confirmed.value - pendingSending),
                valueOf(coinType, pendingReceiving), valueOf(coinType, pendingSending), Value.zeroValue(coinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    private fun getPendingReceiving(): BigInteger {
        return backing.getUnconfirmedTransactions() .filter {
                    !it.from.equals(receiveAddress.addressString, true) && it.to.equals(receiveAddress.addressString, true)
                }
                .map { it.value.value }
                .fold(BigInteger.ZERO, BigInteger::add)
    }

    private fun getPendingSending(): BigInteger {
        return backing.getUnconfirmedTransactions().filter {
                    it.from.equals(receiveAddress.addressString, true) && !it.to.equals(receiveAddress.addressString, true)
                }
                .map { tx -> tx.value.value + tx.fee.value }
                .fold(BigInteger.ZERO, BigInteger::add) +
                backing.getUnconfirmedTransactions() .filter {
                            it.from.equals(receiveAddress.addressString, true) && it.to.equals(receiveAddress.addressString, true)
                        }
                        .map { tx -> tx.fee.value }
                        .fold(BigInteger.ZERO, BigInteger::add)
    }

    private fun selectEndpoint(): Boolean {
        val currentEndpointIndex = endpoints.currentEndpointIndex
        for (x in 0 until endpoints.size()) {
            val ethUtils = EthSyncChecker(client)
            try {
                if (ethUtils.isSynced) {
                    if (currentEndpointIndex != endpoints.currentEndpointIndex) {
                        ethBalanceService.client = client
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

    override fun isSyncing() = isSyncing

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

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

    private fun syncTransactions(): Boolean {
        val remoteTransactions = EthTransactionService(receiveAddress.addressString, transactionServiceEndpoints).getTransactions()
        remoteTransactions.forEach { tx ->
            backing.putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", tx.from, tx.to,
                    valueOf(coinType, tx.value), valueOf(coinType, tx.gasPrice * typicalEstimatedTransactionSize.toBigInteger()),
                    tx.confirmations.toInt(), tx.nonce, tx.gasLimit, tx.gasUsed)
        }
        return true
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = ServerEndpoints(newEndpoints)
        updateClient()
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
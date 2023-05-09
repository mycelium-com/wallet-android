package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.max
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.*

import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.lang.Exception
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class EthAccount(private val chainId: Byte,
                 private val accountContext: EthAccountContext,
                 credentials: Credentials? = null,
                 backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 blockchainService: EthBlockchainService,
                 address: EthAddress? = null) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, blockchainService, EthAccount::class.simpleName, address), SyncPausable {

    var enabledTokens: MutableList<String> = accountContext.enabledTokens?.toMutableList()
            ?: mutableListOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    fun removeEnabledToken(tokenName: String) {
        enabledTokens.remove(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun addEnabledToken(tokenName: String) {
        enabledTokens.add(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun isEnabledToken(tokenName: String) = enabledTokens.contains(tokenName)

    fun hasHadActivity(): Boolean =
            accountBalance.spendable.isPositive() || accountContext.nonce > BigInteger.ZERO

    @Throws(InsufficientFundsException::class, BuildTransactionException::class)
    override fun createTx(toAddress: Address, value: Value, fee: Fee, data: TransactionData?): Transaction {
        val ethTxData = data as? EthTransactionData
        val nonce = ethTxData?.nonce ?: accountContext.nonce
        val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(typicalEstimatedTransactionSize.toLong())
        val inputData = ethTxData?.inputData ?: ""
        val gasPrice = ethTxData?.suggestedGasPrice?.let { Value.valueOf(coinType, it) } ?: (fee as FeePerKbFee).feePerKb

        if (gasPrice.value <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value < BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Value should be positive"))
        }
        if (gasLimit < Transfer.GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must be at least ${Transfer.GAS_LIMIT}"))
        }
        if (value > calculateMaxSpendableAmount(gasPrice, null, ethTxData)) {
            throw InsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPrice.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }
        return EthTransaction(coinType, toAddress.toString(), value, gasPrice.value, nonce, gasLimit, inputData)
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val rawTransaction = (request as EthTransaction).run {
            RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, ethValue.value,
                    inputData)
        }
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.apply {
            signedHex = hexValue
            txHash = TransactionUtils.generateTransactionHash(rawTransaction, chainId, credentials)
            txBinary = TransactionEncoder.encode(rawTransaction)!!
        }
    }

    override fun signMessage(message: String, address: Address?): String {
        val msgBytes = message.toByteArray(StandardCharsets.UTF_8)
        val sig = Sign.signPrefixedMessage(msgBytes, credentials!!.ecKeyPair)
        return "${Numeric.toHexString(sig.r)}${Numeric.toHexString(sig.s).substring(2)}${HexUtils.toHex(sig.v)}"
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        try {
            val result = blockchainService.sendTransaction((tx as EthTransaction).signedHex!!)
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                    tx.signedHex!!, receivingAddress.addressString, tx.toAddress, tx.ethValue,
                    valueOf(coinType, tx.gasPrice * tx.gasLimit), 0, tx.nonce, tx.gasPrice, gasLimit = tx.gasLimit)
        } catch (e: IOException) {
            return BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
        }
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override val coinType
        get() = accountContext.currency

    override val basedOnCoinType
        get() = coinType

    override val accountBalance
        get() = accountContext.balance

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    @Synchronized
    override suspend fun doSynchronization(mode: SyncMode?): Boolean {
        val syncTx = syncTransactions()
        updateBalanceCache()
        return syncTx
    }

    override fun updateBalanceCache(): Boolean {
        val balResponse: BalanceResponse?
        try {
            balResponse = blockchainService.getBalance(receivingAddress.addressString)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Couldn't update eth balance:  ${e.javaClass} ${e.localizedMessage}. Using cached value.")
            return false
        }

        var pendingReceiving = BigInteger.ZERO
        var pendingSending = BigInteger.ZERO
        if (balResponse.unconfirmed <= BigInteger.ZERO) {
            pendingSending = balResponse.unconfirmed.negate()
        } else {
            pendingReceiving = balResponse.unconfirmed
        }

        val newBalance = Balance(valueOf(coinType, balResponse.confirmed - pendingSending),
                valueOf(coinType, pendingReceiving), valueOf(coinType, pendingSending), Value.zeroValue(coinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    override fun canSign() = credentials != null

    private suspend fun syncTransactions(): Boolean {
        try {
            val remoteTransactions = withContext(Dispatchers.IO) { blockchainService.getTransactions(receivingAddress.addressString) }
            backing.putTransactions(remoteTransactions, coinType, typicalEstimatedTransactionSize.toBigInteger())
            val localTxs = getUnconfirmedTransactions()
            // remove such transactions that are not on server anymore
            // this could happen if transaction was replaced by another e.g.
            val remoteTransactionsIds = remoteTransactions.map { it.txid }
            val toRemove = localTxs.filter { localTx ->
                !remoteTransactionsIds.contains("0x" + HexUtils.toHex(localTx.id))
                        && (System.currentTimeMillis() / 1000 - localTx.timestamp > TimeUnit.SECONDS.toSeconds(150))
            }
            toRemove.map { "0x" + HexUtils.toHex(it.id) }.forEach {
                backing.deleteTransaction(it)
            }
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            logger.log(Level.SEVERE, "Error retrieving ETH/ERC-20 transaction history: ${e.javaClass} ${e.localizedMessage}")
            return false
        }
    }

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        clearBacking()
        accountContext.balance = Balance.getZeroBalance(coinType)
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override val id: UUID
        get() = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(gasPrice: Value, ign: EthAddress?, txData: TransactionData?): Value {
        val gp =
            (txData as? EthTransactionData)?.suggestedGasPrice?.let { Value.valueOf(coinType, it) } ?: gasPrice
        val gl = (txData as? EthTransactionData)?.gasLimit ?: typicalEstimatedTransactionSize.toBigInteger()
        val spendable = accountBalance.spendable - gp * gl
        return max(spendable, Value.zeroValue(coinType))
    }

    override var label: String
        get() = accountContext.accountName
        set(value) {
            accountContext.accountName = value
        }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override val isArchived
        get() = accountContext.archived

    override val syncTotalRetrievedTransactions: Int = 0 // TODO implement after full transaction history implementation

    override val typicalEstimatedTransactionSize = Transfer.GAS_LIMIT.toInt()

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


fun ECKeyPair.toUUID(): UUID = UUID(
        BitUtils.uint64ToLong(publicKey.toByteArray(), 8),
        BitUtils.uint64ToLong(publicKey.toByteArray(), 16))

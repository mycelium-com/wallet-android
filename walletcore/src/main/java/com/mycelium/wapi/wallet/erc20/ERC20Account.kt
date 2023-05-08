package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.*
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.crypto.TransactionUtils
import org.web3j.tx.Transfer
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level


class ERC20Account(private val chainId: Byte,
                   private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   val ethAcc: EthAccount,
                   credentials: Credentials? = null,
                   backing: EthAccountBacking,
                   private val accountListener: AccountListener?,
                   blockchainService: EthBlockchainService,
                   address: EthAddress? = null) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, blockchainService, ERC20Account::class.simpleName, address), SyncPausable {

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        val ethTxData = (data as? EthTransactionData)
        val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT)
        val gasPrice = ethTxData?.suggestedGasPrice?.let {
            Value.valueOf(basedOnCoinType, it)
        } ?: (fee as FeePerKbFee).feePerKb
        val inputData = getInputData(address.toString(), amount.value)

        if (calculateMaxSpendableAmount(gasPrice, null, null) < amount) {
            throw InsufficientFundsException(Throwable("Insufficient funds"))
        }
        if (gasLimit < Transfer.GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must be at least ${Transfer.GAS_LIMIT}"))
        }
        if (ethAcc.accountBalance.spendable.value < gasPrice.value * gasLimit) {
            throw InsufficientFundsForFeeException(Throwable("Insufficient funds on eth account to pay for fee"))
        }

        return EthTransaction(basedOnCoinType, address.toString(), Value.zeroValue(basedOnCoinType),
            gasPrice.value, accountContext.nonce, gasLimit, inputData, amount)
    }

    private fun getInputData(address: String, value: BigInteger): String {
        val function = org.web3j.abi.datatypes.Function(
                StandardToken.FUNC_TRANSFER,
                listOf(org.web3j.abi.datatypes.Address(address),
                        Uint256(value)), emptyList())
        return FunctionEncoder.encode(function)
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val ethTx = request as EthTransaction
        val rawTransaction = RawTransaction.createTransaction(ethTx.nonce, ethTx.gasPrice, ethTx.gasLimit,
                token.contractAddress, ethTx.ethValue.value, ethTx.inputData)
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.apply {
            signedHex = hexValue
            txHash = TransactionUtils.generateTransactionHash(rawTransaction, chainId, credentials)
            txBinary = TransactionEncoder.encode(rawTransaction)!!
        }
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        try {
            val result = blockchainService.sendTransaction((tx as EthTransaction).signedHex!!)
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                    tx.signedHex!!, receivingAddress.addressString, tx.toAddress,
                    Value.valueOf(basedOnCoinType, tx.tokenValue!!.value), Value.valueOf(basedOnCoinType, tx.gasPrice * tx.gasLimit), 0,
                    accountContext.nonce, true, null, true, tx.gasLimit, tx.gasLimit)
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            return when (e) {
                is IOException -> BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
                else -> {
                    logger.log(Level.SEVERE, "Error sending ERC-20 transaction: ${e.localizedMessage}")
                    BroadcastResult(e.localizedMessage, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
                }
            }
        }
    }

    override val coinType: ERC20Token
        get() = token

    override val basedOnCoinType
        get() = accountContext.currency

    override val accountBalance: Balance
        get() = readBalance()

    override var label: String
        get() = accountContext.accountName
        set(value) {
            accountContext.accountName = value
        }

    @Synchronized
    override suspend fun doSynchronization(mode: SyncMode?): Boolean {
        val syncTx = syncTransactions()
        updateBalanceCache()
        return syncTx
    }

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override val isArchived: Boolean
        get() = accountContext.archived

    override val isActive: Boolean
        get() = !isArchived

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
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override val id: UUID
        get() = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: EthAddress?, txData: TransactionData?): Value =
            accountBalance.spendable

    override val syncTotalRetrievedTransactions = 0 // TODO implement after full transaction history implementation

    override val typicalEstimatedTransactionSize = TOKEN_TRANSFER_GAS_LIMIT.toInt()

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateBalanceCache(): Boolean {
        val pendingReceiving = getPendingReceiving()
        val pendingSending = getPendingSending()
        val newBalance = Balance(Value.valueOf(basedOnCoinType, getConfirmed() - pendingSending),
                Value.valueOf(basedOnCoinType, pendingReceiving), Value.valueOf(basedOnCoinType, pendingSending), Value.zeroValue(basedOnCoinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    override fun signMessage(message: String, address: Address?): String {
        TODO("Not yet implemented")
    }

    private fun getConfirmed(): BigInteger = getTransactionSummaries(0, Int.MAX_VALUE)
            .filter { it.confirmations > 0 }
            .map { it.transferred.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    private fun getPendingReceiving(): BigInteger = backing.getUnconfirmedTransactions(receivingAddress.addressString)
            .filter {
                !it.sender.addressString.equals(receiveAddress.addressString, true)
                        && it.receiver.addressString.equals(receiveAddress.addressString, true)
            }
            .map { it.value.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    private fun getPendingSending(): BigInteger = backing.getUnconfirmedTransactions(receivingAddress.addressString)
            .filter {
                it.sender.addressString.equals(receiveAddress.addressString, true)
                        && !it.receiver.addressString.equals(receiveAddress.addressString, true)
            }
            .map { it.value.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    private suspend fun syncTransactions():Boolean {
        try {
            val remoteTransactions = withContext(Dispatchers.IO) { blockchainService.getTransactions(receivingAddress.addressString, token.contractAddress) }
            //TODO convert backing.putTransaction to backing.putTransactions
            remoteTransactions.forEach { tx ->
                tx.getTokenTransfer(token.contractAddress, receivingAddress.addressString)?.also { tokenTransfer ->
                    backing.putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", tokenTransfer.from,
                            tokenTransfer.to, Value.valueOf(basedOnCoinType, tokenTransfer.value),
                            Value.valueOf(basedOnCoinType, tx.gasPrice * (tx.gasUsed
                                    ?: typicalEstimatedTransactionSize.toBigInteger())),
                            tx.confirmations.toInt(), tx.nonce, true,
                        null, tx.success, tx.gasLimit, tx.gasUsed)
                }
            }
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

    // the following two wrappers are needed because we can't store balance in db with ERC20 coin type
    // we only can store balance in db with coinType within cryptocurrencies list
    // (refer to com.mycelium.wapi.wallet.coins.COIN for the list)
    // but on different activities we need balance with ERC20 coin type, therefore we need to convert
    // coinType -> basedOnCoinType (which is ETH for ERC20 and it's in the cryptocurrencies list)
    // all the time we going to store it in db, and basedOnCoinType -> coinType when we read from db
    private fun saveBalance(balance: Balance) {
        accountContext.balance = Balance(Value.valueOf(basedOnCoinType, balance.confirmed.value),
                Value.valueOf(basedOnCoinType, balance.pendingReceiving.value),
                Value.valueOf(basedOnCoinType, balance.pendingSending.value),
                Value.valueOf(basedOnCoinType, 0))
    }

    private fun readBalance(): Balance {
        val balance = accountContext.balance
        return Balance(Value.valueOf(coinType, balance.confirmed.value),
                Value.valueOf(coinType, balance.pendingReceiving.value),
                Value.valueOf(coinType, balance.pendingSending.value),
                Value.valueOf(coinType, 0))
    }

    companion object {
        const val TOKEN_TRANSFER_GAS_LIMIT = 90_000L
        val AVG_TOKEN_TRANSFER_GAS = (Transfer.GAS_LIMIT.toLong() + TOKEN_TRANSFER_GAS_LIMIT) / 2
    }
}

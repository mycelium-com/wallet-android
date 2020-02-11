package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.max

class ERC20Account(private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   private val ethAcc: EthAccount,
                   private val credentials: Credentials? = null,
                   private val backing: EthAccountBacking,
                   endpoints: List<HttpEndpoint>) : WalletAccount<EthAddress> {
    private var endpoints = ServerEndpoints(endpoints.toTypedArray())
    private val logger = Logger.getLogger(ERC20Account::javaClass.name)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) }

    lateinit var client: Web3j

    init {
        updateClient()
    }

    private fun buildCurrentEndpoint() = Web3j.build(HttpService(endpoints.currentEndpoint.baseUrl))

    private fun updateClient() {
        client = buildCurrentEndpoint()
    }

    private var transfersDisposable: Disposable = subscribeOnTransferEvents()

    private fun subscribeOnTransferEvents(): Disposable {
        val contract = StandardToken.load(token.contractAddress, client, credentials, DefaultGasProvider())
        return contract.transferEventFlowable(DefaultBlockParameterNumber(accountContext.blockHeight.toLong()), DefaultBlockParameterName.PENDING)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    val txhash = it.log!!.transactionHash
                    val tx = client.ethGetTransactionByHash(txhash).send()
                    backing.putTransaction(-1, System.currentTimeMillis () / 1000, txhash,
                            "", it.from!!, it.to!!, transformValueForDb(Value.valueOf(token, it.value!!)),
                            Value.valueOf(basedOnCoinType, tx.result.gasPrice * tx.result.gas), 0,
                            tx.result.nonce, tx.result.gas, tx.result.gas)
                }
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    override fun createTx(address: GenericAddress, amount: Value, fee: GenericFee): GenericTransaction {
        if (calculateMaxSpendableAmount(null, null).equalZero()) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds"))
        }
        val gasPrice = (fee as FeePerKbFee).feePerKb.value
        val gasLimit = BigInteger.valueOf(90_000)
        if (ethAcc.accountBalance.spendable.value < (gasPrice * gasLimit)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds on eth account to pay for fee"))
        }
        return Erc20Transaction(coinType, address, amount, gasPrice, gasLimit)
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        (request as Erc20Transaction).txBinary = ByteArray(0)
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val erc20Tx = (tx as Erc20Transaction)
        try {
            accountContext.nonce = getNonce(receivingAddress!!)
            val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, StaticGasProvider(erc20Tx.gasPrice, erc20Tx.gasLimit))
            val result = erc20Contract.transfer(erc20Tx.toAddress.toString(), erc20Tx.value.value).send()
            if (!result.isStatusOK) {
                logger.log(Level.SEVERE, "Error sending ERC20 transaction, status not OK: ${result.status}")
                return BroadcastResult("Unable to send transaction.", BroadcastResultType.REJECTED)
            }
            tx.txHash = HexUtils.toBytes(result.transactionHash.substring(2))
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, result.transactionHash,
                    "", receivingAddress.addressString, tx.toAddress.toString(),
                    transformValueForDb(tx.value), Value.valueOf(basedOnCoinType, tx.gasPrice * result.gasUsed), 0,
                    accountContext.nonce, tx.gasLimit, result.gasUsed)
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error sending ERC20 transaction: ${e.localizedMessage}")
            return BroadcastResult("Unable to send transaction: ${e.localizedMessage}", BroadcastResultType.REJECTED)
        }
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getCoinType() = token

    override fun getBasedOnCoinType() = accountContext.currency

    override fun getAccountBalance() = readBalance()

    override fun isMineAddress(address: GenericAddress?) = address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary =
            backing.getTransactionSummary("0x" + HexUtils.toHex(transactionId), receivingAddress!!.addressString)!!

    override fun getTransactionSummaries(offset: Int, limit: Int) =
            backing.getTransactionSummaries(offset.toLong(), limit.toLong(), receivingAddress!!.addressString)

    override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> =
            mutableListOf()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel(): String = token.name

    override fun setLabel(label: String?) {
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
        return false
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        if (!syncBlockHeight()) {
            return false
        }
        if (!syncTransactions()) {
            return false
        }
        return updateBalanceCache()
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override fun canSpend() = credentials != null

    override fun isSyncing() = false // TODO implement

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
        thread {
            transfersDisposable.dispose()
        }
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
        transfersDisposable = subscribeOnTransferEvents()
    }

    override fun dropCachedData() {
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true // TODO implement

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: EthAddress?): Value =
            accountBalance.spendable

    override fun getSyncTotalRetrievedTransactions() = 0 // TODO implement

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

    private fun updateBalanceCache(): Boolean {
        return try {
            // https://github.com/web3j/web3j/blob/5001c05f6165d24a3df95760ea8ed8343faf46c4/core/src/main/java/org/web3j/tx/gas/DefaultGasProvider.java
            val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, DefaultGasProvider())
            val result = erc20Contract.balanceOf(receivingAddress!!.addressString).sendAsync()
            saveBalance(Balance(Value.valueOf(coinType, result.get()), Value.zeroValue(coinType), Value.zeroValue(coinType), Value.zeroValue(coinType)))
            true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ERC20 account: ${e.localizedMessage}")
            false
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

    /**
     * replace coinType -> basedOnCoinType before insert into db
     */
    private fun transformValueForDb(value: Value): Value {
        return Value.valueOf(basedOnCoinType, value.value)
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
        val localTransactions = backing.getTransactionSummaries(0, Long.MAX_VALUE,
                receivingAddress!!.addressString)
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
                                    val newFee = Value.valueOf(basedOnCoinType, remoteTx.result.gasPrice * txReceipt.result.gasUsed)
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
}
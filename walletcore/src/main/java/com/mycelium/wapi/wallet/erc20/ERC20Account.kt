package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.net.ServerEndpointType
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class ERC20Account(private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   private val credentials: Credentials? = null,
                   web3jServices: List<HttpService>) : WalletAccount<EthAddress> {
    private val endpoints = ServerEndpoints(web3jServices.toTypedArray()).apply {
        setAllowedEndpointTypes(ServerEndpointType.ALL)
    }
    private val logger = Logger.getLogger(ERC20Account::javaClass.name)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) }
    var client: Web3j = buildCurrentEndpoint()
    private fun buildCurrentEndpoint() = Web3j.build(endpoints.currentEndpoint)

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    override fun createTx(address: GenericAddress, amount: Value, fee: GenericFee): GenericTransaction {
        val gasPrice = (fee as FeePerKbFee).feePerKb.value
        val gasLimit = BigInteger.valueOf(500_000)
        return Erc20Transaction(coinType, address, amount, gasPrice, gasLimit)
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        (request as Erc20Transaction).txBinary = request.toString().toByteArray()
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val erc20Tx = (tx as Erc20Transaction)
        try {
            val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, erc20Tx.gasPrice, erc20Tx.gasLimit)
            val result = erc20Contract.transfer(erc20Tx.toAddress.toString(), erc20Tx.value.value).send()
            if (!result.isStatusOK) {
                return BroadcastResult("Unable to send transaction.", BroadcastResultType.REJECTED)
            }
            tx.txHash = result.transactionHash.toByteArray()
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

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

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
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
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
            // gasPrice and gasLimit constants are from https://github.com/web3j/web3j/blob/5001c05f6165d24a3df95760ea8ed8343faf46c4/core/src/main/java/org/web3j/tx/gas/DefaultGasProvider.java
            val erc20Contract = StandardToken.load(token.contractAddress, client, credentials, BigInteger.valueOf(4_100_000_000L), BigInteger.valueOf(9_000_000))
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
}
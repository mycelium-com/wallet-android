package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.HttpsEndpoint
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.*
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import org.web3j.crypto.Credentials
import org.web3j.tx.Transfer
import org.web3j.tx.gas.StaticGasProvider
import java.io.IOException
import java.math.BigInteger
import java.util.*
import java.util.logging.Level


class ERC20Account(private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   private val ethAcc: EthAccount,
                   credentials: Credentials,
                   backing: EthAccountBacking,
                   private val accountListener: AccountListener?,
                   web3jWrapper: Web3jWrapper,
                   private val transactionServiceEndpoints: List<HttpsEndpoint>) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, ERC20Account::class.simpleName, web3jWrapper) {
    private val balanceService = ERC20BalanceService(receivingAddress.addressString, token, basedOnCoinType, web3jWrapper, credentials)
    private var removed = false

    override fun createTx(address: GenericAddress, amount: Value, fee: GenericFee, data: GenericTransactionData?): GenericTransaction {
        val ethTxData = (data as? EthTransactionData)
        val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(90_000)
        val gasPrice = (fee as FeePerKbFee).feePerKb.value

        if (calculateMaxSpendableAmount(null, null) < amount) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds"))
        }
        if (gasLimit < typicalEstimatedTransactionSize.toBigInteger()) {
            throw GenericBuildTransactionException(Throwable("Gas limit must be at least 21000"))
        }
        if (ethAcc.accountBalance.spendable.value < gasPrice * gasLimit) {
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
            accountContext.nonce = getNewNonce(receivingAddress)
            val erc20Contract = web3jWrapper.loadContract(token.contractAddress,
                    credentials!!, StaticGasProvider(erc20Tx.gasPrice, erc20Tx.gasLimit))
            val result = erc20Contract.transfer(erc20Tx.toAddress.toString(), erc20Tx.value.value).send()
            tx.txHash = HexUtils.toBytes(result.transactionHash.substring(2))
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, result.transactionHash,
                    "", receivingAddress.addressString, tx.toAddress.toString(),
                    Value.valueOf(basedOnCoinType, tx.value.value), Value.valueOf(basedOnCoinType, tx.gasPrice * result.gasUsed), 0,
                    accountContext.nonce, tx.gasLimit, result.gasUsed)
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

    override fun getCoinType() = token

    override fun getBasedOnCoinType() = accountContext.currency

    override fun getAccountBalance() = readBalance()

    override fun getLabel(): String = accountContext.accountName

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    @Synchronized
    override fun doSynchronization(mode: SyncMode?): Boolean {
        if (removed || isArchived) {
            return false
        }
        syncTransactions()
        return updateBalanceCache()
    }

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

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
        clearBacking()
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: EthAddress?): Value =
            accountBalance.spendable

    override fun getSyncTotalRetrievedTransactions() = 0 // TODO implement after full transaction history implementation

    override fun getTypicalEstimatedTransactionSize() = Transfer.GAS_LIMIT.toInt()

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateBalanceCache(): Boolean {
        balanceService.updateBalanceCache()
        var newBalance = balanceService.balance

        val pendingReceiving = getPendingReceiving()
        val pendingSending = getPendingSending()
        newBalance = Balance(Value.valueOf(basedOnCoinType, newBalance.confirmed.value - pendingSending),
                Value.valueOf(basedOnCoinType, pendingReceiving), Value.valueOf(basedOnCoinType, pendingSending), Value.zeroValue(basedOnCoinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

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

    private fun syncTransactions() {
        try {
            val remoteTransactions = ERC20TransactionService(receiveAddress.addressString, transactionServiceEndpoints,
                    token.contractAddress).getTransactions()
            remoteTransactions.filter { tx -> tx.getTokenTransfer(token.contractAddress) != null }.forEach { tx ->
                val transfer = tx.getTokenTransfer(token.contractAddress)!!
                backing.putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", transfer.from,
                        transfer.to, Value.valueOf(basedOnCoinType, transfer.value),
                        Value.valueOf(basedOnCoinType, tx.gasPrice * (tx.gasUsed
                                ?: typicalEstimatedTransactionSize.toBigInteger())),
                        tx.confirmations.toInt(), tx.nonce, tx.gasLimit, tx.gasUsed)
            }
            val localTxs = getUnconfirmedTransactions()
            // remove such transactions that are not on server anymore
            // this could happen if transaction was replaced by another e.g.
            val toRemove = localTxs.filter { localTx ->
                !remoteTransactions.map { it.txid }.contains("0x" + HexUtils.toHex(localTx.id))
            }
            toRemove.map { "0x" + HexUtils.toHex(it.id) }.forEach {
                backing.deleteTransaction(it)
            }
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "Error retrieving ETH/ERC-20 transaction history, ${e.localizedMessage}")
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
package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

abstract class AbstractEthERC20Account(coinType: CryptoCurrency,
                                       protected val credentials: Credentials? = null,
                                       protected val backing: EthAccountBacking,
                                       className: String?,
                                       protected val web3jWrapper: Web3jWrapper,
                                       address: EthAddress? = null) : WalletAccount<EthAddress> {
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!
    protected val logger: Logger = Logger.getLogger(className)
    @Volatile
    protected var syncing = false

    protected fun getNonce(address: EthAddress): BigInteger {
        return try {
            val ethGetTransactionCount = web3jWrapper.ethGetTransactionCount(address.toString(),
                    DefaultBlockParameterName.PENDING)
                    .send()

            setNonce(ethGetTransactionCount.transactionCount)
            getNonce()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH/ERC20, ${e.localizedMessage}")
            getNonce()
        }
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        val synced = doSynchronization(mode)
        syncing = false
        return synced
    }

    abstract fun doSynchronization(mode: SyncMode?): Boolean
    abstract fun setNonce(nonce: BigInteger)
    abstract fun getNonce(): BigInteger
    abstract fun setBlockChainHeight(height: Int)

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?) = false

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getDummyAddress() = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String?): EthAddress = dummyAddress

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun isMineAddress(address: GenericAddress?) = address == receivingAddress

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

    override fun canSpend() = credentials != null

    override fun isSyncing() = syncing

    override fun isActive() = !isArchived

    /**
     * during this sync we update transactions confirmations number
     * and check for local transactions that have been created more than 5 minutes ago
     * but are missing on the server and remove them from local db
     * assuming the transactions haven't been propagated (transaction queueing for eth not supported yet)
     */
    protected fun syncTransactions(): Boolean {
        val localTransactions = backing.getTransactionSummaries(0, Long.MAX_VALUE,
                receivingAddress.addressString)
        getBlockHeight()
        localTransactions.forEach {
            try {
                val remoteTx = web3jWrapper.ethGetTransactionByHash("0x" + it.idHex).send()
                if (!remoteTx.hasError()) {
                    if (remoteTx.result != null) {
                        // blockNumber is not null when transaction is confirmed
                        // https://github.com/ethereum/wiki/wiki/JSON-RPC#returns-28
                        if (remoteTx.result.blockNumberRaw != null) {
                            // "it.height == -1" indicates that this is a newly created transaction
                            // and we haven't received any information about it's confirmation from the server yet
                            if (it.height == -1) { // update gasUsed only once when tx has just been confirmed
                                val txReceipt = web3jWrapper.ethGetTransactionReceipt("0x" + it.idHex).send()
                                if (!txReceipt.hasError()) {
                                    val newFee = Value.valueOf(basedOnCoinType, remoteTx.result.gasPrice * txReceipt.result.gasUsed)
                                    backing.updateGasUsed("0x" + it.idHex, txReceipt.result.gasUsed, newFee)
                                }
                            }

                            val confirmations = if (it.height != -1) getBlockHeight() - it.height
                            else max(0, getBlockHeight() - remoteTx.result.blockNumber.toInt())
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
                logger.log(Level.SEVERE, "Error synchronizing ETH/ERC20, ${e.localizedMessage}")
                return false
            }
        }
        return true
    }

    protected fun getBlockHeight(): Int {
        return try {
            val latestBlock = web3jWrapper.ethBlockNumber().send()

            blockChainHeight = latestBlock.blockNumber.toInt()
            blockChainHeight
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH/ERC20, ${e.localizedMessage}")
            blockChainHeight
        }
    }
}
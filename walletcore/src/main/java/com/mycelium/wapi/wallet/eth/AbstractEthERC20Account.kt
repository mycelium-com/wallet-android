package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.DefaultBlockParameterName
import java.io.IOException
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger

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

    @Throws(IOException::class)
    protected fun getNewNonce(address: EthAddress): BigInteger {
        val ethGetTransactionCount = web3jWrapper.ethGetTransactionCount(address.toString(),
                DefaultBlockParameterName.PENDING)
                .send()

        setNonce(ethGetTransactionCount.transactionCount)
        return getNonce()
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        syncing = true
        updateBlockHeight()
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

    private fun updateBlockHeight() {
        try {
            val latestBlock = web3jWrapper.ethBlockNumber().send()

            blockChainHeight = latestBlock.blockNumber.toInt()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH/ERC20, ${e.localizedMessage}")
        }
    }

    override fun canSign(): Boolean = false
}
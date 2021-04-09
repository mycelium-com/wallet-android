package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import org.web3j.crypto.Credentials
import java.io.IOException
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger

abstract class AbstractEthERC20Account(coinType: CryptoCurrency,
                                       protected val credentials: Credentials? = null,
                                       protected val backing: EthAccountBacking,
                                       protected val blockchainService: EthBlockchainService,
                                       className: String?,
                                       address: EthAddress? = null) : WalletAccount<EthAddress> {
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!
    protected val logger: Logger = Logger.getLogger(className)

    @Volatile
    protected var syncing = false

    fun clearBacking() {
        backing.deleteAllAccountTransactions()
    }

    fun getUnconfirmedTransactions() = backing.getUnconfirmedTransactions(receivingAddress.addressString)

    fun deleteTransaction(txid: String) {
        backing.deleteTransaction(txid)
        updateBalanceCache()
    }

    @Throws(IOException::class)
    protected fun getNewNonce(): BigInteger {
        val nonce = blockchainService.getNonce(receivingAddress.addressString)
        setNonce(nonce)
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
    abstract fun updateBalanceCache(): Boolean

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    override fun isSpendingUnconfirmed(tx: Transaction?) = false

    override fun queueTransaction(transaction: Transaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getDummyAddress() = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String?): EthAddress = dummyAddress

    override fun getDependentAccounts() = emptyList<WalletAccount<Address>>()

    override fun isMineAddress(address: Address?) = address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): Transaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): TransactionSummary =
            backing.getTransactionSummary("0x" + HexUtils.toHex(transactionId), receivingAddress.addressString)!!

    override fun getTransactionSummaries(offset: Int, limit: Int) =
            backing.getTransactionSummaries(offset.toLong(), limit.toLong(), receivingAddress.addressString)

    override fun getTransactionsSince(receivingSince: Long) =
            backing.getTransactionSummariesSince(receivingSince / 1000, receivingAddress.addressString)

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = credentials != null

    override fun isSyncing() = syncing

    override fun isActive() = !isArchived

    private fun updateBlockHeight() {
        try {
            val latestBlockHeight = blockchainService.getBlockHeight()

            blockChainHeight = latestBlockHeight.toInt()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH/ERC-20, ${e.localizedMessage}")
        }
    }

    override fun canSign(): Boolean = false
}
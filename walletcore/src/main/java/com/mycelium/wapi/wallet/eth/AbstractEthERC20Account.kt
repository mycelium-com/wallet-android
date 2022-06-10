package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                                       address: EthAddress? = null) : SyncPausableAccount(), WalletAccount<EthAddress> {
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
    protected suspend fun getNewNonce(): BigInteger {
        val nonce = withContext(Dispatchers.IO) { blockchainService.getNonce(receivingAddress.addressString) }
        setNonce(nonce)
        return getNonce()
    }

    override suspend fun synchronize(mode: SyncMode?): Boolean {
        if (isArchived) { return false }
        syncing = true
        val synced: Boolean
        try {
            if (!maySync) {
                return false
            }
            updateBlockHeight()
            if (!maySync) {
                return false
            }
            synced = doSynchronization(mode)
            if (!maySync) {
                return false
            }
            getNewNonce()
            if (synced) {
                lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
            }
        } finally {
            syncing = false
        }
        return synced
    }

    abstract suspend fun doSynchronization(mode: SyncMode?): Boolean
    abstract fun setNonce(nonce: BigInteger)
    abstract fun getNonce(): BigInteger
    abstract fun setBlockChainHeight(height: Int)
    abstract fun updateBalanceCache(): Boolean

    override fun setAllowZeroConfSpending(b: Boolean) {
        // TODO("not implemented")
    }

    override fun isSpendingUnconfirmed(tx: Transaction) = false

    override fun queueTransaction(transaction: Transaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val receiveAddress
        get() = receivingAddress

    override val dummyAddress = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String): EthAddress = dummyAddress

    override val dependentAccounts
        get() = emptyList<WalletAccount<Address>>()

    override fun isMineAddress(address: Address?) = address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray): Transaction? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray): TransactionSummary? =
            backing.getTransactionSummary("0x" + HexUtils.toHex(transactionId), receivingAddress.addressString)

    override fun getTransactionSummaries(offset: Int, limit: Int) =
            backing.getTransactionSummaries(offset.toLong(), limit.toLong(), receivingAddress.addressString)

    override fun getTransactionsSince(receivingSince: Long) =
            backing.getTransactionSummariesSince(receivingSince / 1000, receivingAddress.addressString)

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = credentials != null

    override fun isSyncing() = syncing

    override val isActive: Boolean
        get() = !isArchived

    private suspend fun updateBlockHeight() {
        try {
            val latestBlockHeight = withContext(Dispatchers.IO) { blockchainService.getBlockHeight() }

            setBlockChainHeight(latestBlockHeight.toInt())
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing ETH/ERC-20, ${e.localizedMessage}")
        }
    }

    override fun canSign(): Boolean = false
}
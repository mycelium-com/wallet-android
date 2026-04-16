package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
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
        val address = receivingAddress.addressString
        val serverNonce = withContext(Dispatchers.IO) { blockchainService.getNonce(address) }
        // Blockbook's `nonce` field is confirmed-only. Pending outgoing txs
        // (including stuck ones that haven't yet been dropped from mempools)
        // must push us past them, otherwise we collide. We must NOT simply add
        // the total unconfirmed count because that includes incoming txs.
        // Scan the full address tx list for unconfirmed outgoing and take the
        // highest nonce. Covers the shared-address case where pending USDT
        // contract calls live in the EthAccount's view but not the ERC20
        // account's local backing.
        val maxPendingOutgoingNonce = try {
            withContext(Dispatchers.IO) { blockchainService.getTransactions(address) }
                .filter { it.confirmations.signum() == 0 && it.from.equals(address, true) }
                .map { it.nonce }
                .maxOrNull()
        } catch (e: IOException) {
            null
        }
        val nonce = if (maxPendingOutgoingNonce != null && maxPendingOutgoingNonce >= serverNonce)
            maxPendingOutgoingNonce + BigInteger.ONE
        else
            serverNonce
        setNonce(nonce)
        return nonce
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

    // Advance local nonce past a just-broadcast tx. Prevents the next send
    // from reusing the same nonce before the server has indexed this one.
    fun bumpNonceAfterBroadcast(broadcastNonce: BigInteger) {
        val next = broadcastNonce + BigInteger.ONE
        if (next > getNonce()) {
            setNonce(next)
        }
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

    override fun hasHadActivity(): Boolean = getTransactionSummaries(0, 1).isNotEmpty()

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

    override fun createTx(
        outputs: List<Pair<Address, Value>>,
        fee: Fee,
        data: TransactionData?
    ): Transaction {
        TODO("Not yet implemented")
    }
}
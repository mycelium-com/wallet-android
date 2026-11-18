package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Fee
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.SyncPausableAccount
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import java.io.IOException
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Shared behaviour of [TronAccount] and [com.mycelium.wapi.wallet.trc20.TRC20Account]:
 * key/address handling, TAPOS block reference, sync skeleton and pending-tx resolution.
 */
abstract class AbstractTronAccount(
    coinType: CryptoCurrency,
    // Null for watch-only (address-only) accounts — they can observe but never sign.
    protected val credentials: Credentials? = null,
    protected val backing: TronAccountBacking,
    protected val blockchainService: TronBlockchainService,
    className: String?,
    address: TronAddress? = null,
) : SyncPausableAccount(), WalletAccount<TronAddress> {

    val receivingAddress: TronAddress =
        credentials?.let { TronAddress(coinType, TronKeys.addressOf(it.ecKeyPair)) } ?: address!!

    protected val logger: Logger = Logger.getLogger(className)

    @Volatile
    protected var syncing = false

    /** Latest chain block reference for TAPOS; refreshed by sync and before signing. */
    @Volatile
    private var latestBlockRef: TronBlockRef? = null
    @Volatile
    private var blockRefFetchedAtMs = 0L

    /**
     * Returns a block reference fresh enough for signing, fetching one when missing or stale.
     * A stale ref-block makes the node reject the transaction (TAPOS check). Blocking network
     * call — must not run on the main thread.
     */
    @Throws(IOException::class)
    fun ensureBlockRef(): TronBlockRef {
        val cached = latestBlockRef
        val now = System.currentTimeMillis()
        if (cached != null && now - blockRefFetchedAtMs < BLOCK_REF_MAX_AGE_MS) return cached
        return try {
            val fresh = blockchainService.getBlockRef()
            setBlockRef(fresh)
            fresh
        } catch (e: IOException) {
            // Keep a usable (if older) ref rather than failing the send outright
            cached ?: throw e
        }
    }

    protected open fun setBlockRef(ref: TronBlockRef) {
        latestBlockRef = ref
        blockRefFetchedAtMs = System.currentTimeMillis()
        setBlockChainHeight(ref.number.toInt())
    }

    fun clearBacking() {
        backing.deleteAllAccountTransactions()
    }

    fun getUnconfirmedTransactions(): List<TronTransactionSummary> =
        backing.getUnconfirmedTransactions(receivingAddress.addressString)

    /**
     * Node balance (sun / token base units) seen right before each local broadcast, by txid, in
     * broadcast order. TronGrid's account balance picks a tx up as soon as its block is produced
     * (~3 s) — usually before the history sync has marked our local record confirmed — so reserving
     * the record again took the amount off twice until the next sync.
     */
    private val balanceBeforeBroadcast = LinkedHashMap<String, BigInteger>()

    protected fun rememberBalanceBeforeBroadcast(txid: String) {
        val cached = accountBalance
        synchronized(balanceBeforeBroadcast) {
            // the cached balance subtracts the pending reservation: add it back to get the node's figure
            balanceBeforeBroadcast[txid] = cached.confirmed.value + cached.pendingSending.value
        }
    }

    /** Sum of the pending outgoing debits the node balance does not reflect yet, see [unreflectedPendingDebits]. */
    protected fun unreflectedPending(nodeBalance: BigInteger, pending: List<PendingDebit>): BigInteger =
        synchronized(balanceBeforeBroadcast) {
            balanceBeforeBroadcast.keys.retainAll(pending.map { it.txid }.toSet())
            unreflectedPendingDebits(balanceBeforeBroadcast, nodeBalance, pending)
        }

    /**
     * Broadcasts [signed] (whose id is [txid]). A plain [IOException] does not mean the tx failed:
     * the connection may have dropped after the node already accepted it. In that case the node is
     * asked a few times whether it knows the tx, and if not the very same signed bytes are re-sent
     * (same txid, so this can't double-spend; a node that has it answers DUP_TRANSACTION_ERROR).
     * Returns [BroadcastResultType.UNKNOWN_OUTCOME] when the node stays unreachable.
     * Blocking — call off the main thread.
     */
    protected fun broadcastSigned(signed: ByteArray, txid: String): BroadcastResult {
        val firstError = try {
            val result = blockchainService.broadcast(signed)
            return if (result.success) BroadcastResult(BroadcastResultType.SUCCESS)
            else BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        } catch (e: IOException) {
            e
        }
        logger.log(Level.WARNING, "Broadcast of $txid failed (${firstError.message}), checking whether the node got it")
        for (delayMs in BROADCAST_RECHECK_DELAYS_MS) {
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            try {
                if (blockchainService.isTransactionKnown(txid)) return BroadcastResult(BroadcastResultType.SUCCESS)
                val result = blockchainService.broadcast(signed)
                if (result.success) return BroadcastResult(BroadcastResultType.SUCCESS)
                // an explicit rejection of a tx the node already processed would be a false failure
                if (blockchainService.isTransactionKnown(txid)) return BroadcastResult(BroadcastResultType.SUCCESS)
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            } catch (e: IOException) {
                logger.log(Level.WARNING, "Re-check of $txid failed: ${e.message}")
            }
        }
        return BroadcastResult(firstError.message, BroadcastResultType.UNKNOWN_OUTCOME)
    }

    fun deleteTransaction(txid: String) {
        backing.deleteTransaction(txid)
        updateBalanceCache()
    }

    override suspend fun synchronize(mode: SyncMode?): Boolean {
        if (isArchived) return false
        syncing = true
        val synced: Boolean
        try {
            if (!maySync) return false
            updateBlockHeight()
            if (!maySync) return false
            synced = doSynchronization(mode)
            if (!maySync) return false
            if (synced) {
                lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
            }
        } finally {
            syncing = false
        }
        return synced
    }

    abstract suspend fun doSynchronization(mode: SyncMode?): Boolean
    abstract fun setBlockChainHeight(height: Int)
    abstract fun updateBalanceCache(): Boolean

    /**
     * Resolves locally stored pending transactions absent from the freshly fetched remote
     * page by checking each individually against the node. Confirmed → block recorded; mined
     * but failed on-chain → marked FAILED; unknown after the grace period → dropped/expired
     * and removed. Network errors keep the tx for the next sync.
     */
    protected suspend fun resolvePendingTransactions(remoteTxIds: Collection<String>) {
        val remoteIds = remoteTxIds.toSet()
        val now = System.currentTimeMillis()
        getUnconfirmedTransactions().forEach { pendingTx ->
            val txid = HexUtils.toHex(pendingTx.id)
            if (txid in remoteIds) return@forEach
            // Always ask the node: a tx it already mined must stop being "pending" right away,
            // otherwise the balance reserves it while the node balance already reflects it (double count).
            val withinGrace = now - pendingTx.timestamp * 1000 <= PENDING_TX_GRACE_MS
            val info = try {
                withContext(Dispatchers.IO) { blockchainService.getTransactionInfo(txid) }
            } catch (e: IOException) {
                logger.log(Level.WARNING, "Could not resolve pending tx $txid, keeping it: ${e.message}")
                return@forEach
            }
            if (info.isConfirmed) {
                val fee = info.fee.takeIf { it > 0 }?.let { Value.valueOf(basedOnCoinType, it) }
                backing.updateResources(txid, info)
                if (info.isSuccess) {
                    backing.confirmTransaction(txid, info.blockNumber.toInt(), info.blockTimestampMs / 1000, fee)
                } else {
                    logger.info("Pending tx $txid failed on-chain (${info.receiptResult})")
                    backing.failTransaction(txid, info.blockNumber.toInt(), fee)
                }
            } else if (!withinGrace) {
                logger.info("Pending tx $txid not found on-chain after grace period — removing")
                backing.deleteTransaction(txid)
            }
        }
    }

    private suspend fun updateBlockHeight() {
        try {
            val ref = withContext(Dispatchers.IO) { blockchainService.getBlockRef() }
            setBlockRef(ref)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error synchronizing TRON, ${e.localizedMessage}")
        }
    }

    override fun setAllowZeroConfSpending(b: Boolean) {}

    override fun isSpendingUnconfirmed(tx: Transaction) = false

    override fun queueTransaction(transaction: Transaction) {
        TODO("not implemented")
    }

    override fun removeAllQueuedTransactions() {
        TODO("not implemented")
    }

    override val receiveAddress
        get() = receivingAddress

    override val dummyAddress = TronAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String): TronAddress = dummyAddress

    override val dependentAccounts
        get() = emptyList<WalletAccount<Address>>()

    override fun isMineAddress(address: Address?) =
        address is TronAddress && address.addressString == receivingAddress.addressString

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray): Transaction? {
        TODO("not implemented")
    }

    override fun getTxSummary(transactionId: ByteArray): TransactionSummary? =
        backing.getTransactionSummary(HexUtils.toHex(transactionId), receivingAddress.addressString)

    override fun getTransactionSummaries(offset: Int, limit: Int): List<TransactionSummary> =
        backing.getTransactionSummaries(offset.toLong(), limit.toLong(), receivingAddress.addressString)

    /**
     * History page for the UI. With [transfersOnly] only TRX / TRC20 value transfers are returned
     * (staking, resource delegation, votes, other contract calls etc. are skipped in SQL, so
     * offset-based paging stays consistent). TRC20 sends — contract calls on [tokenContracts] —
     * stay visible on the TRX account, since they burn its TRX fee.
     */
    fun getTransactionSummaries(offset: Int, limit: Int, transfersOnly: Boolean,
                                tokenContracts: Collection<String> = emptyList()): List<TransactionSummary> =
        if (transfersOnly) {
            backing.getTransferSummaries(offset.toLong(), limit.toLong(), receivingAddress.addressString, tokenContracts)
        } else {
            getTransactionSummaries(offset, limit)
        }

    override fun getTransactionsSince(receivingSince: Long): List<TransactionSummary> =
        backing.getTransactionSummariesSince(receivingSince / 1000, receivingAddress.addressString)

    override fun getUnspentOutputViewModels(): MutableList<OutputViewModel> {
        TODO("not implemented")
    }

    override fun hasHadActivity(): Boolean = getTransactionSummaries(0, 1).isNotEmpty()

    override fun canSpend() = credentials != null

    override fun canSign() = credentials != null

    override fun isSyncing() = syncing

    override val isActive: Boolean
        get() = !isArchived

    /** TIP-191 signature (TronLink / TronWeb signMessageV2 format); "" for watch-only accounts. */
    override fun signMessage(message: String, address: Address?): String =
        credentials?.let { TronKeys.signMessage(message, it.ecKeyPair) } ?: ""

    override fun createTx(outputs: List<Pair<Address, Value>>, fee: Fee, data: TransactionData?): Transaction {
        TODO("Not yet implemented")
    }

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey? =
        credentials?.let { InMemoryPrivateKey(it.ecKeyPair.privateKey.toByteArray().let(::stripSign), true) }

    private fun stripSign(bytes: ByteArray): ByteArray = when {
        bytes.size == 33 && bytes[0] == 0.toByte() -> bytes.copyOfRange(1, 33)
        bytes.size < 32 -> ByteArray(32 - bytes.size) + bytes
        else -> bytes
    }

    override val syncTotalRetrievedTransactions: Int = 0

    override fun broadcastOutgoingTransactions() = true

    override fun isVisible() = true

    companion object {
        // Refresh the ref-block before signing if older than this
        private const val BLOCK_REF_MAX_AGE_MS = 60_000L

        // Pauses before asking the node again about a broadcast whose response was lost
        private val BROADCAST_RECHECK_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)

        // How long a broadcast tx may stay unseen by the node before it is treated as dropped
        private val PENDING_TX_GRACE_MS = TimeUnit.SECONDS.toMillis(150)

        // Incremental sync fetches txs from (newest confirmed timestamp - this overlap)
        const val SYNC_OVERLAP_MS = 600_000L
    }
}

/** An unconfirmed outgoing tx: [value] leaves the account, [debit] = value + estimated fee in the same unit. */
data class PendingDebit(val txid: String, val value: BigInteger, val debit: BigInteger)

/**
 * Pending debits to reserve on top of [nodeBalance]. Walking the txs in broadcast order, the drop of the
 * node balance since the first broadcast is consumed by the txs it already covers; only the value is
 * compared (the actual fee may be below the estimate). Txs broadcast before a restart have no recorded
 * balance and are always reserved.
 */
internal fun unreflectedPendingDebits(
    balanceBeforeBroadcast: LinkedHashMap<String, BigInteger>,
    nodeBalance: BigInteger,
    pending: List<PendingDebit>,
): BigInteger {
    val order = balanceBeforeBroadcast.keys.toList()
    var drop = balanceBeforeBroadcast.values.firstOrNull()?.let { it - nodeBalance } ?: BigInteger.ZERO
    var reserved = BigInteger.ZERO
    pending.sortedBy { order.indexOf(it.txid).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }.forEach {
        if (it.txid in balanceBeforeBroadcast && it.value.signum() > 0 && drop >= it.value) {
            drop -= it.debit.min(drop)
        } else {
            reserved += it.debit
        }
    }
    return reserved
}

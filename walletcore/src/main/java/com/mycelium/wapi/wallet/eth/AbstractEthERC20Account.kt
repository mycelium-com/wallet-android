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
import java.util.concurrent.TimeUnit
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

    /** PENDING-only subset, i.e. unconfirmed and not yet marked terminal. */
    fun getLivePendingTransactions(): List<EthTransactionSummary> =
            getUnconfirmedTransactions().filter { it.status == EthTxStatus.PENDING }

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

    /**
     * Decide the terminal lifecycle state for any local PENDING tx that the
     * account-level sync no longer sees, instead of deleting it.
     *
     * Resolution order (per local pending tx):
     *  1. A confirmed remote tx exists with the same `from + nonce` but a
     *     different hash — REPLACED (CANCELED if the replacement is a
     *     0-value self-transfer, the canonical RBF cancel pattern). This
     *     check runs unconditionally: it does not respect the grace period
     *     and does not skip txids in `knownAliveTxids`. A confirmed
     *     replacement is decisive — the original cannot also mine, no
     *     matter what blockbook's stale indexes still claim.
     *  2. Within grace period (and no replacement found) — leave as
     *     PENDING; mempools can be slow.
     *  3. Caller said this txid is alive in the current sync — leave as
     *     PENDING. Without a confirmed replacement we cannot tell whether
     *     it will eventually mine.
     *  4. Per-tx endpoint still returns it — leave as PENDING.
     *  5. Otherwise — DROPPED.
     *
     * @param fullAddressTxs full address tx history, used to detect
     *                       replacements. Pass `null` to fetch on demand.
     * @param knownAliveTxids txids that the caller has already verified are
     *                        live this sync (e.g. found in address?details=txids
     *                        or fetched via per-tx endpoint). These gate the
     *                        DROPPED branch only — they do NOT block
     *                        replacement detection, since blockbook can keep
     *                        listing a replaced tx in its txids cache for
     *                        long after a confirmed replacement has landed.
     */
    protected suspend fun reconcileLocalPending(
        fullAddressTxs: List<Tx>? = null,
        knownAliveTxids: Set<String> = emptySet()
    ) {
        val pending = getLivePendingTransactions()
        if (pending.isEmpty()) return

        val now = System.currentTimeMillis() / 1000
        val ownerAddress = receivingAddress.addressString

        val addressTxs = fullAddressTxs ?: try {
            withContext(Dispatchers.IO) { blockchainService.getTransactions(ownerAddress) }
        } catch (_: IOException) { emptyList() }

        var anyMarked = false
        for (localTx in pending) {
            val txid = "0x" + HexUtils.toHex(localTx.id)

            // (1) Replacement detection — strongest signal. A confirmed
            // remote tx with same from+nonce as our pending means the
            // original was bumped out of the mempool. This runs even within
            // the grace period and even when blockbook's address indexes
            // still list the original txid, because the original CANNOT
            // also mine at the same nonce.
            val localNonce = localTx.nonce
            val replacement = if (localNonce != null) {
                addressTxs.firstOrNull { remote ->
                    remote.txid != txid
                            && remote.confirmations.signum() > 0
                            && remote.from.equals(ownerAddress, true)
                            && remote.nonce == localNonce
                }
            } else null
            if (replacement != null) {
                val newStatus = if (replacement.value.signum() == 0
                        && (replacement.to?.equals(replacement.from, true) == true))
                    EthTxStatus.CANCELED
                else
                    EthTxStatus.REPLACED
                // Adopt the replacement's blockNumber/timestamp so the
                // resolved tx sorts next to its replacement in the history
                // list — otherwise it stays pinned at the top with the
                // pending-tx MAX_VALUE blockNumber.
                backing.markTransactionStatus(
                    txid,
                    newStatus,
                    blockNumber = replacement.blockHeight.toInt(),
                    timestamp = replacement.blockTime,
                )
                anyMarked = true
                continue
            }

            // No replacement seen. Below this point we only mark DROPPED,
            // and only when we're confident the tx is gone from mempools.
            if (now - localTx.timestamp < DROP_GRACE_SECONDS) continue
            if (txid in knownAliveTxids) continue
            try {
                withContext(Dispatchers.IO) { blockchainService.getTransaction(txid) }
                continue
            } catch (_: IOException) { /* truly unknown; fall through */ }

            // Use current tip + now as the DROPPED tx's effective slot —
            // there's no replacement to inherit from, but anchoring it to
            // "now" keeps it from sticking at the top of the list.
            backing.markTransactionStatus(
                txid,
                EthTxStatus.DROPPED,
                blockNumber = getBlockChainHeight(),
                timestamp = now,
            )
            anyMarked = true
        }

        if (anyMarked) updateBalanceCache()
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

    /**
     * After broadcasting a replacement, flip any local PENDING outgoing tx
     * sharing the same nonce (with a different hash) to REPLACED so the UI
     * reflects it immediately, without waiting for the next sync to spot the
     * confirmed replacement. The next reconcileLocalPending() will reaffirm
     * this once the new tx mines.
     *
     * Anchors the REPLACED row to (current tip, now) so it stops sorting at
     * the top with the pending-tx MAX_VALUE blockNumber and instead sits
     * next to the replacement in the history list. When the replacement
     * mines, reconcileLocalPending() will overwrite these placeholders with
     * the replacement's real blockNumber/timestamp.
     */
    fun markPendingReplacedByNonce(nonce: BigInteger, newTxid: String) {
        val sender = receivingAddress.addressString
        val replaced = getLivePendingTransactions()
            .asSequence()
            .filter { it.nonce == nonce }
            .filter { it.sender.addressString.equals(sender, true) }
            .map { "0x" + HexUtils.toHex(it.id) }
            .filter { it != newTxid }
            .toList()
        if (replaced.isEmpty()) return
        val now = System.currentTimeMillis() / 1000
        val tip = getBlockChainHeight()
        replaced.forEach {
            backing.markTransactionStatus(
                it,
                EthTxStatus.REPLACED,
                blockNumber = tip,
                timestamp = now,
            )
        }
        updateBalanceCache()
    }

    abstract suspend fun doSynchronization(mode: SyncMode?): Boolean
    abstract fun setNonce(nonce: BigInteger)
    abstract fun getNonce(): BigInteger
    abstract fun setBlockChainHeight(height: Int)
    abstract fun updateBalanceCache(): Boolean

    /**
     * ETH balance available to pay miner fees. For ERC20 this returns the
     * parent EthAccount's spendable; for EthAccount itself it returns its own.
     */
    protected abstract fun ethFeeSpendable(): Value

    /**
     * Build the parameters for a cancel transaction without signing or
     * broadcasting. Returns null if the supplied tx isn't a live pending
     * outgoing tx with a known nonce.
     *
     * The cancel tx itself is always a 0-value ETH self-transfer at the same
     * nonce, so its gas is paid in ETH regardless of whether the original was
     * an ERC20 contract call.
     *
     * Bump rule: max(oldGasPrice * 1.1, current network fastest gas price).
     * If network gas has spiked we use the network value; if it hasn't, we
     * use the +10% bump that mempools require for replacement.
     */
    suspend fun estimateCancelTx(record: EthTransactionSummary): CancelEstimate? {
        if (record.confirmations != 0) return null
        if (record.status != EthTxStatus.PENDING) return null
        val nonce = record.nonce ?: return null
        if (!record.sender.addressString.equals(receivingAddress.addressString, true)) return null

        val ethSpendable = ethFeeSpendable()
        val ethCoinType = ethSpendable.type
        // Blockbook returns the fee estimate as a BigDecimal in ether (e.g.
        // 0.00000002 == 20 Gwei). A naive `.toBigInteger()` truncates that to 0
        // for any realistic gas price, so we have to round-trip through
        // Value.parse to convert ether → wei the same way EthFeeProvider does.
        val networkGasPrice = try {
            withContext(Dispatchers.IO) {
                Value.parse(ethCoinType, blockchainService.feeEstimation(NETWORK_FASTEST_BLOCK).result).value
            }
        } catch (_: IOException) {
            BigInteger.ZERO
        }
        val bumpedOld = record.gasPrice * BUMP_NUMERATOR / BUMP_DENOMINATOR
        val newGasPrice = bumpedOld.max(networkGasPrice)
        // Over-provision gasLimit so an EIP-7702 delegate's fallback() has
        // budget to run; on a non-delegated EOA the unused gas is refunded.
        val cancelGasLimit = CANCEL_GAS_LIMIT
        val newFeeWei = newGasPrice * cancelGasLimit
        return CancelEstimate(
            nonce = nonce,
            oldGasPrice = record.gasPrice,
            newGasPrice = newGasPrice,
            gasLimit = cancelGasLimit,
            oldFee = Value.valueOf(ethCoinType, record.gasPrice * record.gasLimit),
            newFee = Value.valueOf(ethCoinType, newFeeWei),
            ethSpendable = ethSpendable,
            hasEnoughEth = ethSpendable.value >= newFeeWei,
        )
    }

    /**
     * Sign and broadcast a cancel tx (0-value ETH self-transfer at the same
     * nonce as [record]). On success marks [record] as CANCELED locally so the
     * UI flips immediately without waiting for the next sync.
     */
    abstract suspend fun cancelTransaction(record: EthTransactionSummary, estimate: CancelEstimate): BroadcastResult

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

    companion object {
        // How long a tx stays PENDING before we'll consider it terminal
        // when no server-side endpoint can find it. Generous on purpose:
        // mempools can hold low-fee txs for a long time before dropping
        // them, and we do not want to label something DROPPED while the
        // user could still see it land.
        val DROP_GRACE_SECONDS: Long = TimeUnit.MINUTES.toSeconds(30)

        // Mempools require a replacement to bid at least ~10% above the
        // original gasPrice to displace it.
        private val BUMP_NUMERATOR = BigInteger.valueOf(11)
        private val BUMP_DENOMINATOR = BigInteger.TEN
        private const val NETWORK_FASTEST_BLOCK = 1
        // 21000 = intrinsic ETH transfer. Add headroom for an EIP-7702
        // delegate's fallback if the sender is delegated; unused gas is
        // refunded on success, so this only costs more on a delegate revert.
        private val CANCEL_GAS_LIMIT: BigInteger = BigInteger.valueOf(50_000)
    }
}

/**
 * Snapshot of the parameters and balances needed to display a cancel-tx
 * confirmation and to subsequently broadcast it.
 */
data class CancelEstimate(
    val nonce: BigInteger,
    val oldGasPrice: BigInteger,
    val newGasPrice: BigInteger,
    val gasLimit: BigInteger,
    /** gas paid (or budgeted) by the original tx — gasPrice * gasLimit. */
    val oldFee: Value,
    /** gas that will be paid by the cancel tx — newGasPrice * 21000. */
    val newFee: Value,
    val ethSpendable: Value,
    val hasEnoughEth: Boolean,
)

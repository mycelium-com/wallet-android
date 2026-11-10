package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.providers.deriveFeeTiers
import com.mycelium.wapi.wallet.providers.deriveLegacyGasPrices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import java.io.IOException
import java.math.BigInteger
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLHandshakeException

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

    // Full address tx list captured by the current sync round's syncTransactions()
    // and reused by getNewNonce() so we don't paginate the same history twice.
    // Cleared in synchronize()'s finally block.
    @Volatile
    private var lastSyncTxs: List<Tx>? = null

    protected fun cacheSyncTxs(txs: List<Tx>) {
        lastSyncTxs = txs
    }

    /**
     * Confirmed txids stored locally for this account. ERC20 children read
     * this from their parent [EthAccount] to use the full-address-history
     * stop set (not the ERC20-only filtered set) when doing incremental
     * sync — see [com.mycelium.wapi.wallet.erc20.ERC20Account.syncTransactions].
     */
    fun confirmedTxidsSnapshot(): Set<String> = backing.getConfirmedTxids()

    fun clearBacking() {
        backing.deleteAllAccountTransactions()
    }

    /**
     * Resolve the EIP-1559 tip to pair with a chosen [maxFeePerGas] cap, or null
     * when the chain is on legacy fees ([feeCoin] not 1559-enabled) or no tip is
     * available — in which case the caller signs a legacy (type-0) tx. Prefers an
     * explicit tip, else recovers it from the cached fee snapshot.
     *
     * @param feeCoin the chain coin carrying the rollout flag (ETH coin for both
     *                ETH and ERC-20 sends, since gas is always paid in ETH).
     */
    /**
     * A user-chosen max fee below the chain's current base fee can never be included (EIP-1559
     * chains only, and only when a fee snapshot is known). Throws so the Send screen can show why.
     */
    @Throws(BuildTransactionException::class)
    protected fun checkGasPriceAgainstBaseFee(feeCoin: CryptoCurrency, maxFeePerGas: BigInteger) {
        if ((feeCoin as? EthCoin)?.supportsEip1559 != true) return
        val baseFee = blockchainService.lastEip1559Fees()?.baseFeePerGas ?: return
        if (maxFeePerGas < baseFee) {
            val gwei = BigInteger.TEN.pow(9).toBigDecimal()
            throw BuildTransactionException(Throwable("Gas price " +
                    maxFeePerGas.toBigDecimal().divide(gwei).stripTrailingZeros().toPlainString() +
                    " gwei is below the network base fee of " +
                    baseFee.toBigDecimal().divide(gwei).setScale(2, java.math.RoundingMode.UP).toPlainString() + " gwei"))
        }
    }

    protected fun eip1559Tip(
        feeCoin: CryptoCurrency,
        maxFeePerGas: BigInteger,
        explicitTip: BigInteger?,
    ): BigInteger? {
        if ((feeCoin as? EthCoin)?.supportsEip1559 != true) return null
        return explicitTip ?: blockchainService.priorityFeeFor(maxFeePerGas)
    }

    fun getUnconfirmedTransactions() = backing.getUnconfirmedTransactions(receivingAddress.addressString)

    /** PENDING-only subset, i.e. unconfirmed and not yet marked terminal. */
    fun getLivePendingTransactions(): List<EthTransactionSummary> =
            getUnconfirmedTransactions().filter { it.status == EthTxStatus.PENDING }

    fun deleteTransaction(txid: String) {
        backing.deleteTransaction(txid)
        updateBalanceCache()
    }

    /**
     * Sends the signed raw tx [signedHex] (hash [txid]) and survives a lost response: see
     * [broadcastSignedEthTx]. Never re-signs, so a re-send carries the same hash and cannot pay twice.
     * Blocking — call off the main thread.
     */
    protected fun sendSignedTx(signedHex: String, txid: String): BroadcastResult =
        broadcastSignedEthTx(
            txid,
            send = { blockchainService.sendTransaction(signedHex) },
            // blockbook answers /tx/<hash> for mempool txs too; anything but a 200 throws
            isKnown = { blockchainService.getTransaction(txid); true },
            delaysMs = broadcastRecheckDelaysMs,
            logger = logger,
        )

    /** Pauses of [sendSignedTx]'s re-checks; tests set zeros. */
    internal var broadcastRecheckDelaysMs: LongArray = ETH_BROADCAST_RECHECK_DELAYS_MS

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
        val cached = lastSyncTxs
        val maxPendingOutgoingNonce = if (cached != null) {
            cached.filter { it.confirmations.signum() == 0 && it.from.equals(address, true) }
                .map { it.nonce }
                .maxOrNull()
        } else try {
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
     *     0-value self-transfer, the canonical RBF cancel pattern). `from`
     *     is the local tx's own sender, NOT this account's address: for an
     *     incoming pending tx the nonce belongs to the counterparty, and
     *     matching it against our own outgoing nonces (which start at 0
     *     just like theirs) falsely flags freshly received txs as REPLACED.
     *     This check runs unconditionally: it does not respect the grace
     *     period and does not skip txids in `knownAliveTxids`. A confirmed
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
            // also mine at the same nonce. The nonce is only comparable
            // within one sender's tx stream — for incoming pending txs the
            // sender is the counterparty, so match against ITS confirmed
            // txs, never against this account's own outgoing history.
            val localNonce = localTx.nonce
            val localSender = localTx.sender.addressString
            val replacement = if (localNonce != null) {
                addressTxs.firstOrNull { remote ->
                    remote.txid != txid
                            && remote.confirmations.signum() > 0
                            && remote.from.equals(localSender, true)
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
            lastSyncTxs = null
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
        // Replacement must bid >=10% over the stuck tx to displace it.
        // record.gasPrice holds the old cap (maxFeePerGas) for type-2 txs and
        // the gas price for legacy ones.
        val bumpedOld = record.gasPrice * BUMP_NUMERATOR / BUMP_DENOMINATOR
        // Over-provision gasLimit so an EIP-7702 delegate's fallback() has
        // budget to run; on a non-delegated EOA the unused gas is refunded.
        val cancelGasLimit = CANCEL_GAS_LIMIT

        if ((ethCoinType as? EthCoin)?.supportsEip1559 == true) {
            // type-2 cancel. We don't persist the stuck tx's tip, so to guarantee
            // a valid replacement we bump off the known old cap: since the old tip
            // can never exceed the old cap, a new tip >= oldCap*1.1 also clears
            // oldTip*1.1. Use the current fastest tier as a floor so a cancel
            // issued during a fee spike still lands; fall back to the legacy
            // network estimate when no snapshot is cached.
            val fastest = freshEip1559Fees(networkGasPrice)?.tier(EthFeeLevel.HIGH)
            val networkCap = fastest?.maxFeePerGas ?: networkGasPrice
            val networkTip = fastest?.maxPriorityFeePerGas ?: networkGasPrice
            val (newCap, newTip) = type2CancelFees(bumpedOld, networkCap, networkTip)
            val newFeeWei = newCap * cancelGasLimit
            return CancelEstimate(
                nonce = nonce,
                oldGasPrice = record.gasPrice,
                newGasPrice = newCap,
                gasLimit = cancelGasLimit,
                oldFee = Value.valueOf(ethCoinType, record.gasPrice * record.gasLimit),
                newFee = Value.valueOf(ethCoinType, newFeeWei),
                ethSpendable = ethSpendable,
                hasEnoughEth = ethSpendable.value >= newFeeWei,
                newMaxFeePerGas = newCap,
                newMaxPriorityFeePerGas = newTip,
            )
        }

        // networkGasPrice is the bare base fee (see currentBaseFee): a cancel priced at it gets
        // stuck on the first base-fee uptick — 2026-10-26 every cancel of 0x065A…86E3 went out
        // at the base fee of its moment and stuck minutes later. Price it like the "high" tier.
        val networkLegacyPrice = if (networkGasPrice.signum() > 0)
            deriveLegacyGasPrices(networkGasPrice).last() else networkGasPrice
        val newGasPrice = bumpedOld.max(networkLegacyPrice)
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
     * EIP-1559 tiers fetched now for a cancel. The snapshot cached by the fee provider is only as
     * fresh as the last time a send screen was open — hours, possibly, and a cancel priced at an
     * old market gets stuck itself. Falls back to tiers derived from [baseFee] (Blockbook's
     * estimate), and only then to the cached snapshot.
     */
    private suspend fun freshEip1559Fees(baseFee: BigInteger): Eip1559Snapshot? {
        try {
            return withContext(Dispatchers.IO) { blockchainService.eip1559FeeEstimation() }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "EIP-1559 estimator failed for cancel, deriving from base fee", e)
        }
        if (baseFee.signum() > 0) return Eip1559Snapshot(baseFee, deriveFeeTiers(baseFee))
        return blockchainService.lastEip1559Fees()
    }

    /**
     * Blockbook's Ethereum `estimatefee` ignores the block target and answers the current base
     * fee + 1000 wei: a tx priced below it cannot be mined right now.
     */
    private suspend fun currentBaseFee(): BigInteger? = try {
        withContext(Dispatchers.IO) {
            Value.parse(basedOnCoinType, blockchainService.feeEstimation(NETWORK_FASTEST_BLOCK).result).value
        }
    } catch (e: Exception) {
        null
    }

    /**
     * The earliest of this address's pending txs priced under the current base fee — it cannot be
     * mined, and an account's txs mine strictly in nonce order, so it holds up every later one.
     * Null when nothing is stuck. With [beforeNonce], only txs ahead of that slot count.
     *
     * 2026-10-26, 0x065A…86E3: a cancel at nonce 26 priced at a base fee that had since risen
     * fourfold; the user cancelled 27, 28 and 29, none of which could mine before it.
     *
     * Uses the server's whole-address view: cancels and token sends share one nonce sequence but
     * live in different local accounts. Of several txs in one slot the dearest is the one the
     * mempool kept.
     */
    suspend fun findStuckQueueTx(beforeNonce: BigInteger? = null): QueueBlocker? {
        val address = receivingAddress.addressString
        val txs = try {
            withContext(Dispatchers.IO) { blockchainService.getTransactions(address) }
        } catch (e: Exception) {
            return null
        }
        if (txs.none { it.confirmations.signum() == 0 && it.from.equals(address, true) }) return null
        val baseFee = currentBaseFee() ?: return null
        return pickStuckQueueTx(txs, address, beforeNonce, baseFee)
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

    /**
     * Signs a raw 32-byte hash with the account key, no message prefix — for the EIP-7702
     * authorization and the ERC-4337 UserOperation of a token-paid send.
     */
    fun signHash(hash: ByteArray): Sign.SignatureData =
        Sign.signMessage(hash, credentials?.ecKeyPair ?: throw IllegalStateException("Watch-only account cannot sign"), false)

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

/** Pauses before asking the node again about a broadcast whose response was lost. */
internal val ETH_BROADCAST_RECHECK_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)

/** No new re-check starts after this long; each request still has its own OkHttp timeouts. */
private const val ETH_BROADCAST_RECOVERY_BUDGET_MS = 60_000L

/**
 * Broadcast of an already signed tx, the network side of [AbstractEthERC20Account.sendSignedTx].
 *
 * An [IOException] from [send] does not mean the tx failed: the connection may have dropped after
 * the node got it. Then the node is asked (after each of [delaysMs]) whether it knows [txid], and if
 * not the same signed bytes are sent again — same hash, so this can't create a second payment.
 *  - known, accepted on re-send, or a re-send answered "already known" / "nonce too low" → SUCCESS
 *  - a re-send explicitly rejected for another reason (and still unknown) → REJECT_INVALID_TX_PARAMS
 *  - no answer at all → UNKNOWN_OUTCOME, or NO_SERVER_CONNECTION when no attempt ever got as far
 *    as a connection (DNS / connect / TLS handshake failed), i.e. the tx provably never left.
 * The "already known" mapping only applies to re-sends: on the first send "nonce too low" is a
 * genuine failure (a stale nonce).
 *
 * [isKnown] returns true when the node has the tx and throws [IOException] otherwise.
 */
internal fun broadcastSignedEthTx(
    txid: String,
    send: () -> EthBlockchainService.SendResult,
    isKnown: () -> Boolean,
    sleep: (Long) -> Unit = { Thread.sleep(it) },
    delaysMs: LongArray = ETH_BROADCAST_RECHECK_DELAYS_MS,
    logger: Logger? = null,
): BroadcastResult {
    val firstError = try {
        val result = send()
        // "already known": this very tx (same hash) is already in the node's mempool, e.g. the
        // retry of a send whose outcome was unknown. "nonce too low" stays a rejection here: on a
        // first send it can mean a different tx took the nonce.
        return if (result.success || isAlreadyInMempool(result.message)) BroadcastResult(BroadcastResultType.SUCCESS)
        else BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
    } catch (e: IOException) {
        e
    }
    logger?.log(Level.WARNING, "Broadcast of $txid failed (${firstError.message}), checking whether the node got it")
    var maybeSent = !isProvablyNotSent(firstError)
    val deadline = System.currentTimeMillis() + ETH_BROADCAST_RECOVERY_BUDGET_MS
    fun knownNow(): Boolean = try {
        isKnown()
    } catch (e: IOException) {
        false
    }
    for (delayMs in delaysMs) {
        if (System.currentTimeMillis() > deadline) break
        try {
            sleep(delayMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            break
        }
        if (knownNow()) return BroadcastResult(BroadcastResultType.SUCCESS)
        val resent = try {
            send()
        } catch (e: IOException) {
            if (!isProvablyNotSent(e)) maybeSent = true
            logger?.log(Level.WARNING, "Re-send of $txid failed: ${e.message}")
            continue
        }
        if (resent.success || isAlreadySubmitted(resent.message)) return BroadcastResult(BroadcastResultType.SUCCESS)
        // an explicit rejection of a tx the node already holds would be a false failure
        if (knownNow()) return BroadcastResult(BroadcastResultType.SUCCESS)
        return BroadcastResult(resent.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
    }
    return if (maybeSent) BroadcastResult(firstError.message, BroadcastResultType.UNKNOWN_OUTCOME)
    else BroadcastResult(firstError.message, BroadcastResultType.NO_SERVER_CONNECTION)
}

/** The request never reached the server: no connection was established. */
internal fun isProvablyNotSent(e: IOException): Boolean =
    e is UnknownHostException || e is ConnectException || e is NoRouteToHostException || e is SSLHandshakeException

/**
 * A node's answer to a re-send of a tx it already has (geth/erigon/nethermind wording): in its
 * mempool ("already known", "known transaction") or already mined ("nonce too low").
 */
internal fun isAlreadyInMempool(message: String?): Boolean {
    val m = message?.lowercase() ?: return false
    return "already known" in m || "known transaction" in m || "alreadyknown" in m || "already_exists" in m
}

internal fun isAlreadySubmitted(message: String?): Boolean {
    val m = message?.lowercase() ?: return false
    return "already known" in m || "known transaction" in m || "alreadyknown" in m ||
            "already imported" in m || "nonce too low" in m
}

/** The selection behind [AbstractEthERC20Account.findStuckQueueTx], apart from the network. */
internal fun pickStuckQueueTx(
    txs: List<Tx>,
    address: String,
    beforeNonce: BigInteger?,
    baseFee: BigInteger,
): QueueBlocker? = txs
    .filter { it.confirmations.signum() == 0 && it.from.equals(address, true) }
    .filter { beforeNonce == null || it.nonce < beforeNonce }
    .groupBy { it.nonce }.toSortedMap().values
    .map { slot -> slot.maxByOrNull { it.gasPrice }!! }
    .firstOrNull { it.gasPrice < baseFee }
    ?.let { QueueBlocker(it.txid, it.nonce, it.gasPrice) }

/** A pending tx of ours that cannot be mined at the current base fee (see findStuckQueueTx). */
data class QueueBlocker(
    val txid: String,
    val nonce: BigInteger,
    val gasPrice: BigInteger,
)

/**
 * Snapshot of the parameters and balances needed to display a cancel-tx
 * confirmation and to subsequently broadcast it.
 */
data class CancelEstimate(
    val nonce: BigInteger,
    val oldGasPrice: BigInteger,
    /** Effective per-gas price for fee accounting/display — the cap for a type-2 cancel. */
    val newGasPrice: BigInteger,
    val gasLimit: BigInteger,
    /** gas paid (or budgeted) by the original tx — gasPrice * gasLimit. */
    val oldFee: Value,
    /** gas that will be paid by the cancel tx — newGasPrice * gasLimit. */
    val newFee: Value,
    val ethSpendable: Value,
    val hasEnoughEth: Boolean,
    /** type-2 cancel cap; null → sign the cancel as legacy. */
    val newMaxFeePerGas: BigInteger? = null,
    /** type-2 cancel tip; null → sign the cancel as legacy. */
    val newMaxPriorityFeePerGas: BigInteger? = null,
) {
    val isEip1559: Boolean
        get() = newMaxFeePerGas != null && newMaxPriorityFeePerGas != null
}

/**
 * Type-2 replacement (cancel/RBF) fee bump. Given the already-10%-bumped old cap
 * and the current network fastest cap/tip, returns `(newMaxFeePerGas,
 * newMaxPriorityFeePerGas)` guaranteeing:
 *  - cap  >= oldCap * 1.1   (clears the replaced tx's cap)
 *  - tip  >= oldCap * 1.1   (and since oldTip <= oldCap, also clears oldTip*1.1
 *                            without needing the stuck tx's persisted tip)
 *  - cap  >= tip            (required by EIP-1559)
 * with the network values as a floor so a cancel during a fee spike still lands.
 */
internal fun type2CancelFees(
    bumpedOldCap: BigInteger,
    networkCap: BigInteger,
    networkTip: BigInteger,
): Pair<BigInteger, BigInteger> {
    val newTip = bumpedOldCap.max(networkTip)
    val newCap = bumpedOldCap.max(networkCap).max(newTip)
    return newCap to newTip
}

package com.mycelium.wapi.wallet.tron.energy

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.tron.TronAddress
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/** Persists the marker of an order handed to the partner but not yet seen terminal. */
interface EnergyOrderStore {
    fun getPendingOrder(accountId: UUID): EnergyOrderInfo?
    fun setPendingOrder(order: EnergyOrderInfo)
    fun clearPendingOrder(accountId: UUID)
}

/**
 * Orchestrates "pay the TRC20 fee in the token" through the energy service partner:
 * health/availability → quote → two signed transfers (fee → partner wallet, main → recipient)
 * handed to the partner → poll the order → record both txs locally.
 *
 * All methods are blocking (network) — call from a background thread.
 */
class EnergyServiceManager(
    val client: EnergyServiceClient,
    private val orderStore: EnergyOrderStore,
) {
    private val logger = Logger.getLogger(EnergyServiceManager::class.simpleName)

    @Volatile
    private var cachedHealth: EnergyServiceHealth? = null
    @Volatile
    private var healthFetchedAt = 0L

    val isConfigured: Boolean get() = client.isConfigured

    /** Partner is up, funded and sponsors this account's token. Any failure → false. */
    fun isAvailableFor(account: TRC20Account): Boolean {
        if (!client.isConfigured) return false
        return try {
            health().supports(account.contractAddress)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "energy service health check failed: ${e.message}")
            false
        }
    }

    @Throws(IOException::class, EnergyServiceException::class)
    private fun health(): EnergyServiceHealth {
        val now = System.currentTimeMillis()
        cachedHealth?.let { if (now - healthFetchedAt < HEALTH_CACHE_MS) return it }
        return client.health().also { cachedHealth = it; healthFetchedAt = now }
    }

    /**
     * @param amount amount to send in the token; when zero/null a probe amount is quoted so the
     *        fee is known before the user typed one
     */
    @Throws(IOException::class, EnergyServiceException::class)
    fun getFeeQuote(account: TRC20Account, recipient: TronAddress, amount: Value?): EnergyFeeQuote {
        val human = amount?.takeIf { it.isPositive() }?.valueAsBigDecimal ?: PROBE_AMOUNT
        return client.feeInfo(account.receivingAddress.addressString, recipient.addressString, account.contractAddress, human)
    }

    /** Fee of [quote] in this account's token units. */
    fun feeInAccountUnits(account: TRC20Account, quote: EnergyFeeQuote): Value =
        Value.valueOf(account.coinType, quote.feeInWalletUnits(account.coinType.unitExponent))

    /** Whether [quote] stays within the fee the user approved in [approved]; see [EnergyFeeQuote.isWithinApproval]. */
    fun isWithinApproval(account: TRC20Account, quote: EnergyFeeQuote, approved: EnergyFeeQuote): Boolean =
        quote.isWithinApproval(approved, account.coinType.unitExponent)

    /**
     * Signs the fee payment and the main transfer, submits both to the partner and waits for the
     * order to become terminal. On success the two txs are stored in the account's history.
     *
     * The fee tx is broadcast first by the partner: if amount + fee exceed the balance the fee
     * lands and the transfer reverts, so callers must check `amount + fee <= spendable` upfront.
     *
     * @param quote the partner's quote the transfer is signed against
     * @param approved the quote whose fee the user saw and confirmed. Nothing is signed unless
     *        [quote] pays the same fee wallet and a fee within [EnergyFeeQuote.FEE_TOLERANCE_PERCENT]
     *        of it — otherwise [EnergyTransferResult.FeeChanged] is returned for re-approval.
     */
    fun execute(
        account: TRC20Account,
        quote: EnergyFeeQuote,
        approved: EnergyFeeQuote,
        recipient: TronAddress,
        amount: Value,
        feeLimit: BigInteger,
        keyCipher: KeyCipher,
        onStatus: (String) -> Unit = {},
    ): EnergyTransferResult {
        if (quote.isExpired()) return EnergyTransferResult.QuoteExpired
        val feeWallet = quote.feeWallet ?: return EnergyTransferResult.Error("No fee wallet address in quote")
        if (!isWithinApproval(account, quote, approved)) {
            logger.log(Level.WARNING, "energy service: fee re-quoted beyond what was approved " +
                    "(${quote.feeAmount} to $feeWallet vs ${approved.feeAmount} to ${approved.feeWallet}) — not signing")
            return EnergyTransferResult.FeeChanged(quote)
        }
        if (orderStore.getPendingOrder(account.id) != null) {
            return EnergyTransferResult.Error("A previous transfer is still processing")
        }
        val fee = feeInAccountUnits(account, quote)
        if ((amount + fee) > account.accountBalance.spendable) return EnergyTransferResult.InsufficientBalance

        val expirationMs = ENERGY_TX_EXPIRATION_MS
        val feeTx: com.mycelium.wapi.wallet.tron.TronTransaction
        val mainTx: com.mycelium.wapi.wallet.tron.TronTransaction
        try {
            feeTx = account.createEnergyServiceTransferTx(TronAddress(account.basedOnCoinType, feeWallet), fee, feeLimit, expirationMs)
            mainTx = account.createEnergyServiceTransferTx(recipient, amount, feeLimit, expirationMs)
            // signTx fetches a fresh TAPOS reference block itself
            account.signTx(feeTx, keyCipher)
            account.signTx(mainTx, keyCipher)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "energy service: building/signing failed", e)
            return EnergyTransferResult.Error(e.message ?: "Could not build transaction", e)
        }
        val feeHex = HexUtils.toHex(feeTx.signingOutput!!)
        val mainHex = HexUtils.toHex(mainTx.signingOutput!!)
        val mainTxId = HexUtils.toHex(mainTx.txHash!!)

        onStatus("submitting")
        val response = try {
            client.execute(quote.transactionId, feeHex, mainHex)
        } catch (e: EnergyServiceException) {
            return if (e.isInsufficientBalance) EnergyTransferResult.InsufficientBalance
            else EnergyTransferResult.Error(e.message, e)
        } catch (e: IOException) {
            return EnergyTransferResult.Error(e.message ?: "Network error", e)
        }
        if (!response.success) return EnergyTransferResult.Error(response.message.ifEmpty { "Unknown error" })
        if (response.orderId.isEmpty()) return EnergyTransferResult.Error("No order id in response")

        // From here on the partner may broadcast at any time — remember the order so the user is
        // not offered a second send before this one is resolved.
        orderStore.setPendingOrder(EnergyOrderInfo(response.orderId, account.id, System.currentTimeMillis(),
            mainTxId, mainTx.signedExpirationMs))
        onStatus(response.status)

        val result = client.pollOrderUntilTerminal(response.orderId, onStatus = { onStatus(it.status) })
        when (result) {
            is EnergyTransferResult.Success -> {
                orderStore.clearPendingOrder(account.id)
                // Both hashes are ours to begin with — we signed the txs — so when the partner
                // reports something that isn't a tx hash, fall back to what we computed rather
                // than storing it: the history decodes every txid it reads back.
                account.saveEnergyServiceTransaction(
                    txHash = asTxid(result.txHash) ?: mainTxId, recipient = recipient.addressString,
                    amount = amount.value, feeAmount = fee.value,
                    feeTxHash = asTxid(result.feeTxHash) ?: HexUtils.toHex(feeTx.txHash!!), feeWallet = feeWallet,
                )
            }
            is EnergyTransferResult.Error -> orderStore.clearPendingOrder(account.id)
            else -> {} // Pending: keep the marker
        }
        return result
    }

    /**
     * Resolves a marker left by a previous session: polls the order again; when the signed main
     * tx has expired, checks the chain and drops the marker unless the network is unreachable
     * ("unknown" is not "absent"). Returns true when a marker still blocks new sends.
     */
    fun resumePendingOrder(account: TRC20Account, onStatus: (String) -> Unit = {}): Boolean {
        val order = orderStore.getPendingOrder(account.id) ?: return false
        val now = System.currentTimeMillis()
        if (now - order.createdAtMs > PENDING_ORDER_MAX_AGE_MS) {
            orderStore.clearPendingOrder(account.id)
            return false
        }
        val expiration = order.mainTxExpirationMs ?: (order.createdAtMs + LEGACY_TX_LIFETIME_MS)
        if (now >= expiration + ORDER_EXPIRATION_MARGIN_MS) {
            // The partner can no longer broadcast the signed tx: either it landed or it never will.
            val txId = order.mainTxId ?: try {
                client.orderStatus(order.orderId).txHash
            } catch (e: EnergyServiceException) {
                if (e.isNotFound) { orderStore.clearPendingOrder(account.id); return false }
                return true
            } catch (e: IOException) {
                return true
            }
            if (txId == null) { orderStore.clearPendingOrder(account.id); return false }
            return try {
                account.isTransactionConfirmed(txId) // either way the marker is stale
                orderStore.clearPendingOrder(account.id)
                false
            } catch (e: IOException) {
                true
            }
        }
        val result = client.pollOrderUntilTerminal(order.orderId, onStatus = { onStatus(it.status) }, timeoutMs = RESUME_POLL_TIMEOUT_MS)
        return when (result) {
            is EnergyTransferResult.Success -> {
                orderStore.clearPendingOrder(account.id)
                // Rows may already exist from the original session; putTransaction is an upsert.
                asTxid(order.mainTxId)?.let { mainId ->
                    if (account.getTxSummary(HexUtils.toBytes(mainId)) == null) {
                        account.saveEnergyServiceTransaction(asTxid(result.txHash) ?: mainId, "", BigInteger.ZERO, BigInteger.ZERO, null, null)
                    }
                }
                false
            }
            is EnergyTransferResult.Error -> { orderStore.clearPendingOrder(account.id); false }
            else -> true
        }
    }

    fun pendingOrder(account: TRC20Account): EnergyOrderInfo? = orderStore.getPendingOrder(account.id)

    companion object {
        /**
         * [txid] as a TRON transaction hash, or null when it is not one. Everything the partner
         * reports goes through here before it is stored: a txid is decoded again on every read of
         * the history, so an unparseable one would keep failing long after the transfer is over.
         */
        internal fun asTxid(txid: String?): String? {
            val hex = txid?.trim()?.removePrefix("0x") ?: return null
            if (hex.length != TXID_HEX_LENGTH) return null
            return hex.lowercase().takeIf { it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
        }

        private const val TXID_HEX_LENGTH = 64

        /** Signed txs stay valid this long — bounds how long the partner can hold them. */
        const val ENERGY_TX_EXPIRATION_MS = 30 * 60 * 1000L
        private const val HEALTH_CACHE_MS = 60_000L
        private val PROBE_AMOUNT = BigDecimal("0.00001")
        private const val PENDING_ORDER_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        private const val ORDER_EXPIRATION_MARGIN_MS = 10 * 60 * 1000L
        private const val LEGACY_TX_LIFETIME_MS = 10 * 60 * 60 * 1000L
        private const val RESUME_POLL_TIMEOUT_MS = 30_000L
    }
}

package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.tron.AbstractTronAccount
import com.mycelium.wapi.wallet.tron.TronAccount
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronFeeEstimation
import com.mycelium.wapi.wallet.tron.TronFeeEstimator
import com.mycelium.wapi.wallet.tron.TronTransactionData
import com.mycelium.wapi.wallet.tron.coins.TronCoin
import com.mycelium.wapi.wallet.tron.energy.EnergyFeeQuote
import com.mycelium.wapi.wallet.tron.energy.EnergyServiceException
import com.mycelium.wapi.wallet.tron.energy.EnergyServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.Date

/** How the network fee of a TRC20 transfer is paid. */
enum class TronFeeMode {
    /** Burn TRX from the parent TRON account (standard). */
    TRX,
    /** Pay the fee in the token via the energy service partner (two transactions). */
    ENERGY_SERVICE
}

/**
 * TRON has no fee market: the fee is determined by the resources the tx consumes (bandwidth,
 * energy) and the account's free/staked quota. The model asks [TronFeeEstimator] whenever the
 * recipient (or, for TRC20, the amount) changes and feeds the result into the tx as
 * [TronTransactionData]. For TRC20 accounts it additionally offers the energy service: the
 * fee is then quoted and paid in the token itself.
 */
class SendTronModel(application: Application,
                    account: WalletAccount<*>,
                    intent: Intent)
    : SendCoinsModel(application, account, intent) {

    val parentAccount: TronAccount? = (account as? TRC20Account)?.tronAcc
    val isTrc20: Boolean = account is TRC20Account
    private val feeCoin = account.basedOnCoinType
    private val estimator: TronFeeEstimator = mbwManager.tronFeeEstimator
    private val energyService: EnergyServiceManager? = mbwManager.energyServiceManager

    /** Latest resource estimation; null while loading / when none. */
    val tronFee: MutableLiveData<TronFeeEstimation?> = MutableLiveData(null)
    val feeDetails: MutableLiveData<String> = MutableLiveData("")
    val estimating: MutableLiveData<Boolean> = MutableLiveData(false)
    /** TRC20 in TRX mode: parent TRX account cannot cover the fee limit. */
    val notEnoughTrx: MutableLiveData<Boolean> = MutableLiveData(false)

    // ---- energy service ("pay with USDT")
    val feeMode: MutableLiveData<TronFeeMode> = MutableLiveData(TronFeeMode.TRX)
    val energyServiceAvailable: MutableLiveData<Boolean> = MutableLiveData(false)
    /** The parent TRON account holds some TRX — the user gets to choose the fee mode. */
    val hasTrxForFee: MutableLiveData<Boolean> = MutableLiveData(false)
    val energyQuote: MutableLiveData<EnergyFeeQuote?> = MutableLiveData(null)
    val energyFeeText: MutableLiveData<String> = MutableLiveData("")
    val energyEstimatedTime: MutableLiveData<String> = MutableLiveData("")
    val energyLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val energyError: MutableLiveData<String> = MutableLiveData("")
    /** An earlier energy-service order of this account is still being processed by the partner. */
    val energyOrderPending: MutableLiveData<Boolean> = MutableLiveData(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var estimateJob: Job? = null
    private var quoteJob: Job? = null
    @Volatile
    private var lastTrxEstimation: TronFeeEstimation? = null
    // Read by the tx builder on a background thread — kept apart from the LiveData (postValue is async)
    @Volatile
    private var currentQuote: EnergyFeeQuote? = null
    @Volatile
    private var currentFeeMode: TronFeeMode = TronFeeMode.TRX

    /** Recipient [currentQuote] was fetched for; the fee is priced per recipient, not per amount. */
    @Volatile
    private var quotedFor: String? = null
    /** The current amount came from the amount screen's Max button. */
    @Volatile
    private var amountIsMax = false
    /** Amount this model reduced itself to; anything else is the user's own entry. */
    @Volatile
    private var reducedAmount: Value? = null
    @Volatile
    private var amountReductionsLeft = MAX_AMOUNT_REDUCTIONS

    private val addressObserver = { _: Address? ->
        amountReductionsLeft = MAX_AMOUNT_REDUCTIONS
        onInputsChanged(recipientChanged = true)
    }
    private val amountObserver = { value: Value? ->
        if (isTrc20) {
            // A reduction of our own must not hand itself a fresh allowance to reduce again.
            if (value != reducedAmount) amountReductionsLeft = MAX_AMOUNT_REDUCTIONS
            onInputsChanged()
        }
    }

    init {
        // Reasonable default until the estimator answers: worst-case bandwidth cost.
        val defaultFee = Value.valueOf(feeCoin, if (isTrc20) TronFeeEstimator.MIN_TRC20_FEE_LIMIT
                                                else TronFeeEstimator.TRX_TRANSFER_BANDWIDTH * TronFeeEstimator.SUN_PER_BANDWIDTH)
        applyTrxFee(defaultFee, null)
        // Resources may have been consumed by a send since the last estimate — start fresh
        estimator.invalidateCache((account as AbstractTronAccount).receivingAddress.addressString)
        hasTrxForFee.value = parentAccount?.accountBalance?.spendable?.isPositive() ?: true
        receivingAddress.observeForever(addressObserver)
        amount.observeForever(amountObserver)
        scheduleEstimate()
        if (isTrc20 && energyService != null) checkEnergyServiceAvailability()
    }

    private fun onInputsChanged(recipientChanged: Boolean = false) {
        scheduleEstimate()
        if (currentFeeMode == TronFeeMode.ENERGY_SERVICE) scheduleQuote(refresh = recipientChanged)
    }

    // ------------------------------------------------------------------ TRX fee (burn)

    private fun scheduleEstimate() {
        estimateJob?.cancel()
        estimateJob = scope.launch {
            delay(400) // debounce typing
            estimating.postValue(true)
            try {
                val to = receivingAddress.value?.toString()
                val estimation = if (isTrc20) {
                    val trc20 = account as TRC20Account
                    // Simulate with an amount the sender can actually cover; with no tokens at all
                    // the simulation would only revert, so the estimator falls back to a typical figure.
                    val balance = trc20.accountBalance.spendable.value
                    val requested = amount.value?.takeIf { it.type == account.coinType && it.isPositive() }?.value
                    val amountValue = when {
                        balance.signum() <= 0 -> null
                        requested == null || requested > balance -> balance
                        else -> requested
                    }
                    estimator.estimateTrc20TransferFee(trc20.receivingAddress.addressString, trc20.contractAddress,
                        to ?: trc20.receivingAddress.addressString, amountValue, feeCoin)
                } else {
                    val tron = account as TronAccount
                    estimator.estimateTrxTransferFee(tron.receivingAddress.addressString, feeCoin, to)
                }
                lastTrxEstimation = estimation
                if (currentFeeMode != TronFeeMode.ENERGY_SERVICE) applyTrxFee(estimation.feeValue, estimation)
            } finally {
                estimating.postValue(false)
            }
        }
    }

    private fun applyTrxFee(fee: Value, estimation: TronFeeEstimation?) {
        feeEstimation = FeeEstimationsGeneric(fee, fee, fee, fee, Date().time)
        showStaleWarning.postValue(false)
        tronFee.postValue(estimation)
        feeDetails.postValue(describe(estimation))
        notEnoughTrx.postValue(isTrc20 && parentAccount != null &&
                parentAccount.accountBalance.spendable.value < (estimation?.recommendedFeeLimit ?: fee.value))
        // TransactionData carries bandwidth/energy/feeLimit into createTx; the fee value drives display.
        transactionData.postValue(estimation?.toTransactionData())
        selectedFee.postValue(fee)
    }

    private fun describe(estimation: TronFeeEstimation?): String {
        if (estimation == null) return ""
        val parts = mutableListOf<String>()
        parts += context.getString(R.string.tron_fee_bandwidth, estimation.bandwidth,
            if (estimation.useFreeBandwidth) context.getString(R.string.tron_fee_free) else "")
        if (estimation.energy > 0) {
            parts += context.getString(R.string.tron_fee_energy, estimation.energy,
                if (estimation.useStakedEnergy) context.getString(R.string.tron_fee_free) else "")
            parts += context.getString(R.string.tron_fee_limit,
                Value.valueOf(feeCoin, estimation.recommendedFeeLimit).toStringWithUnit())
        }
        if (estimation.recipientNeedsActivation) {
            parts += context.getString(R.string.tron_fee_activation)
        }
        return parts.joinToString(" · ") { it.trim() }
    }

    // ------------------------------------------------------------------ energy service

    private fun checkEnergyServiceAvailability() {
        val manager = energyService ?: return
        val trc20 = account as TRC20Account
        scope.launch {
            var available = manager.isAvailableFor(trc20)
            // the partner may be unreachable only for now (e.g. no network when the screen opened)
            while (!available) {
                delay(ENERGY_SERVICE_RECHECK_MS)
                available = manager.isAvailableFor(trc20)
            }
            energyServiceAvailable.postValue(available)
            if (available) {
                // Like the KMP wallet: default to paying in the token when the partner is up
                setFeeMode(TronFeeMode.ENERGY_SERVICE)
                // Resolve an order left over from a previous session before allowing a new one
                val stillPending = manager.resumePendingOrder(trc20)
                energyOrderPending.postValue(stillPending)
                if (stillPending) energyError.postValue(context.getString(R.string.error_energy_order_in_flight))
            }
        }
    }

    fun setFeeMode(mode: TronFeeMode) {
        if (currentFeeMode == mode) return
        currentFeeMode = mode
        feeMode.postValue(mode)
        if (mode == TronFeeMode.ENERGY_SERVICE) {
            energyError.postValue("")
            scheduleQuote()
        } else {
            quoteJob?.cancel()
            energyLoading.postValue(false)
            val est = lastTrxEstimation
            applyTrxFee(est?.feeValue ?: selectedFee.value ?: Value.zeroValue(feeCoin), est)
        }
    }

    /**
     * Fetches the energy-service fee for the current recipient. The fee does not depend on the
     * amount: the partner prices the energy and bandwidth the transfer needs — which follow from
     * the recipient, not from how much is sent — at its current rates. So one quote serves the
     * whole entry and is kept until the recipient changes or it nears expiry; re-asking on every
     * keystroke only made the fee, the spendable maximum and everything derived from them jitter,
     * and re-priced the fee under an amount that was computed from the previous one.
     *
     * @param refresh fetch even when the quote in hand would still do (the recipient changed)
     */
    private fun scheduleQuote(refresh: Boolean = false) {
        val manager = energyService ?: return
        val trc20 = account as? TRC20Account ?: return
        val to = receivingAddress.value as? TronAddress
        if (to == null) {
            quoteJob?.cancel()
            quotedFor = null
            applyEnergyQuote(null)
            return
        }
        // Re-applied even when nothing is fetched: switching back from the TRX fee leaves the
        // screen (and the tx data behind it) describing that other mode until a quote lands on it.
        if (!refresh) {
            usableQuote(to)?.let {
                applyEnergyQuote(it)
                return
            }
        }
        quoteJob?.cancel()
        quoteJob = scope.launch {
            delay(QUOTE_DEBOUNCE_MS)
            energyLoading.postValue(true)
            energyError.postValue("")
            try {
                fetchQuote(manager, trc20, to)
            } catch (e: EnergyServiceException) {
                applyEnergyQuote(null)
                energyError.postValue(if (e.isInsufficientBalance) context.getString(R.string.error_energy_service_insufficient_balance, account.coinType.symbol) else e.message)
            } catch (e: Exception) {
                applyEnergyQuote(null)
                energyError.postValue(context.getString(R.string.error_energy_service_unavailable))
                energyLoading.postValue(false)
                // e.g. no network right now: ask again later instead of waiting for the next input change
                delay(ENERGY_SERVICE_RECHECK_MS)
                if (currentFeeMode == TronFeeMode.ENERGY_SERVICE) scheduleQuote(refresh = true)
            } finally {
                energyLoading.postValue(false)
            }
        }
    }

    private fun fetchQuote(manager: EnergyServiceManager, trc20: TRC20Account, to: TronAddress): EnergyFeeQuote {
        // Quoted for a token amount the account can always cover, since the answer wanted here is
        // the fee alone: whether the entered amount fits is ours to check (see validateExtra).
        val quote = manager.getFeeQuote(trc20, to, null)
        quotedFor = to.addressString
        applyEnergyQuote(quote)
        return quote
    }

    /** The quote in hand when it is for [to] and still good for a while, null otherwise. */
    private fun usableQuote(to: TronAddress): EnergyFeeQuote? = currentQuote
        ?.takeIf { quotedFor == to.addressString && !it.isExpired(System.currentTimeMillis() + QUOTE_REFRESH_MARGIN_MS) }

    /**
     * The quote whose fee is on screen for the current recipient — what the user approves by
     * confirming the send. Null when there is none for this recipient.
     */
    fun approvedEnergyQuote(): EnergyFeeQuote? {
        val to = receivingAddress.value as? TronAddress ?: return null
        return currentQuote?.takeIf { quotedFor == to.addressString }
    }

    /** Puts a re-priced [quote] (see [EnergyTransferResult.FeeChanged]) on screen for re-approval. */
    fun showRepricedQuote(quote: EnergyFeeQuote) {
        val to = receivingAddress.value as? TronAddress ?: return
        quotedFor = to.addressString
        applyEnergyQuote(quote)
    }

    /** A quote and the amount it is bound to; the partner rejects a transfer of any other amount. */
    data class EnergySendQuote(val quote: EnergyFeeQuote, val amount: Value)

    /**
     * Quote to sign the transfer with. The one on screen is priced for a token amount the account
     * can always cover — the fee is the same either way — but the partner ties each quote to the
     * amount it was asked about and rejects a transfer of any other (`TX1_AMOUNT_MISMATCH`), so
     * the transfer about to be signed is quoted here for exactly what is being sent.
     *
     * When that lands on a fee that no longer leaves room for a "max", the amount is refitted and
     * quoted again rather than failing in the user's face — but only by a fee within what the user
     * approved in [approved]; a bigger one is handed back unchanged so that
     * [EnergyServiceManager.execute] refuses it for re-approval. Blocking (network) — call off the
     * main thread; null when the partner cannot be reached or refuses the amount.
     */
    fun quoteForSend(approved: EnergyFeeQuote): EnergySendQuote? {
        val manager = energyService ?: return null
        val trc20 = account as? TRC20Account ?: return null
        val to = receivingAddress.value as? TronAddress ?: return null
        var toSend = amount.value?.takeIf { it.type == account.coinType && it.isPositive() } ?: return null
        repeat(MAX_AMOUNT_REDUCTIONS + 1) {
            try {
                // Deliberately not applied to the screen: it prices the same fee, and the entry is
                // done — the amount must not start moving under a send that is already running.
                return EnergySendQuote(manager.getFeeQuote(trc20, to, toSend), toSend)
            } catch (e: EnergyServiceException) {
                if (!e.isInsufficientBalance) return null
                val probe = try {
                    manager.getFeeQuote(trc20, to, null)
                } catch (e2: Exception) {
                    return null
                }
                // A fee the user did not approve must not decide how much reaches the recipient.
                if (!manager.isWithinApproval(trc20, probe, approved)) return EnergySendQuote(probe, toSend)
                val fee = manager.feeInAccountUnits(trc20, probe)
                val max = trc20.accountBalance.spendable - fee
                if (!amountIsMax || !max.isPositive() || max >= toSend) return null
                toSend = max
                reducedAmount = max
                amount.postValue(max)
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    /**
     * The partner re-prices the fee on every quote, so "max" — the spendable balance minus the fee
     * as it was a moment ago — stops fitting as soon as the fee ticks up by a fraction. Rather
     * than dead-ending on an error the user cannot act on, retry with the largest amount [fee]
     * leaves room for.
     *
     * Only a max is refitted: a figure the user typed is theirs, and if it no longer fits they get
     * told so instead of finding a different number in the field. It only ever shrinks, only while
     * the amount alone would fit the balance, and only a bounded number of times per entry, so a
     * fee that keeps moving cannot turn into a loop.
     *
     * With one quote per recipient this is down to the rare case: a max entered against one
     * recipient's fee and then sent to another whose transfer costs more.
     */
    private fun reduceAmountToFit(fee: Value, trc20: TRC20Account): Boolean {
        if (!amountIsMax || amountReductionsLeft <= 0) return false
        val entered = amount.value?.takeIf { it.type == account.coinType && it.isPositive() } ?: return false
        val spendable = trc20.accountBalance.spendable
        if (entered > spendable) return false
        val max = spendable - fee
        if (!max.isPositive() || entered <= max) return false
        amountReductionsLeft--
        reducedAmount = max
        amount.postValue(max)
        return true
    }

    private fun applyEnergyQuote(quote: EnergyFeeQuote?) {
        val manager = energyService
        val trc20 = account as TRC20Account
        currentQuote = quote
        energyQuote.postValue(quote)
        val tokenFee = if (quote != null && manager != null) manager.feeInAccountUnits(trc20, quote) else null
        if (tokenFee == null) {
            energyFeeText.postValue("")
            energyEstimatedTime.postValue("")
        } else {
            energyFeeText.postValue(tokenFee.toStringWithUnit())
            energyEstimatedTime.postValue(quote!!.estimatedTimeSec?.let { context.getString(R.string.tron_energy_estimated_time, formatSeconds(it)) } ?: "")
        }
        // No TRX is burned by this account: fee 0 for display/max-spendable, TRX balance not required.
        // The fee is taken in the token instead, so it travels in the tx data and keeps "max" from
        // offering the full balance (the partner sends the fee first — amount + fee must fit).
        val feeLimit = energyFeeLimit()
        feeEstimation = FeeEstimationsGeneric(Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Date().time)
        notEnoughTrx.postValue(false)
        transactionData.postValue(TronTransactionData(feeLimit = feeLimit, estimatedFee = BigInteger.ZERO, checkFeeBalance = false,
            expirationMs = EnergyServiceManager.ENERGY_TX_EXPIRATION_MS, tokenFee = tokenFee?.value))
        selectedFee.postValue(Value.zeroValue(feeCoin))
        // the quote changes what is spendable — rebuild
        txRebuildPublisher.onNext(Unit)
        // A new recipient can carry a bigger fee than the one a "max" was computed from.
        if (tokenFee != null) reduceAmountToFit(tokenFee, trc20)
    }

    private fun formatSeconds(sec: Int): String = when {
        sec < 60 -> "~${sec}s"
        sec < 3600 -> "~${(sec + 59) / 60} min"
        else -> "~${sec / 3600}h"
    }

    /** Fee of the current energy quote in token units, or null when there is no quote. */
    fun energyFeeValue(): Value? {
        val quote = currentQuote ?: return null
        return energyService?.feeInAccountUnits(account as TRC20Account, quote)
    }

    /** Fee limit for the signed transactions (recommended by the estimator, never below the minimum). */
    fun energyFeeLimit(): BigInteger =
        lastTrxEstimation?.recommendedFeeLimit ?: BigInteger.valueOf(TronFeeEstimator.MIN_TRC20_FEE_LIMIT)

    fun isEnergyMode() = isTrc20 && currentFeeMode == TronFeeMode.ENERGY_SERVICE

    override fun validateExtra(toSend: Value): TransactionStatus? {
        if (!isEnergyMode()) return null
        val fee = energyFeeValue() ?: return TransactionStatus.MISSING_ARGUMENTS
        // The partner broadcasts the fee first: if amount + fee don't fit, the fee lands and the
        // transfer reverts — refuse upfront.
        if ((toSend + fee) > account.accountBalance.spendable) return TransactionStatus.INSUFFICIENT_FUNDS
        return null
    }

    override fun isZeroFeeAllowed() = true

    override fun onAmountEntered(isMax: Boolean) {
        super.onAmountEntered(isMax)
        amountIsMax = isMax
    }

    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        throw IllegalStateException("Tron does not support payment requests")
    }

    override fun getFeeLvlItems(): List<FeeLvlItem> =
        MinerFee.values().map { fee ->
            val duration = Utils.formatBlockcountAsApproxDuration(mbwManager, 1, TronCoin.BLOCK_TIME_IN_SECONDS)
            FeeLvlItem(fee, "~$duration", SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM)
        }

    override fun getRequestedAmountFormatted(): String =
        if (amount.value == null) {
            ""
        } else if (transactionStatus.value == TransactionStatus.OUTPUT_TOO_SMALL
            || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS
            || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE
        ) {
            getValueInAccountCurrency().toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            formatValue(amount.value)
        }

    override fun formatValue(value: Value?): String =
        if (value == null) {
            ""
        } else if (value.type == account.coinType) {
            value.toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            "~ ${value.toStringWithUnit()}"
        }

    fun release() {
        receivingAddress.removeObserver(addressObserver)
        amount.removeObserver(amountObserver)
        estimateJob?.cancel()
        quoteJob?.cancel()
    }

    companion object {
        /** How often one entered amount may be fitted to a re-priced fee before giving up. */
        private const val MAX_AMOUNT_REDUCTIONS = 2
        /** Lets a recipient being typed settle before the partner is asked about it. */
        private const val QUOTE_DEBOUNCE_MS = 500L
        /** A quote this close to expiry is refetched rather than signed with. */
        private const val QUOTE_REFRESH_MARGIN_MS = 60_000L
        /** How often an unavailable energy service is asked again while the Send screen is open. */
        private const val ENERGY_SERVICE_RECHECK_MS = 15_000L
    }
}

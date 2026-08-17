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

    private val addressObserver = { _: Address? -> onInputsChanged() }
    private val amountObserver = { _: Value? -> if (isTrc20) onInputsChanged() }

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

    private fun onInputsChanged() {
        scheduleEstimate()
        if (currentFeeMode == TronFeeMode.ENERGY_SERVICE) scheduleQuote()
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
            val available = manager.isAvailableFor(trc20)
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

    private fun scheduleQuote() {
        val manager = energyService ?: return
        val trc20 = account as? TRC20Account ?: return
        quoteJob?.cancel()
        quoteJob = scope.launch {
            delay(500)
            val to = receivingAddress.value as? TronAddress
            if (to == null) {
                applyEnergyQuote(null)
                return@launch
            }
            energyLoading.postValue(true)
            energyError.postValue("")
            try {
                val quote = manager.getFeeQuote(trc20, to, amount.value?.takeIf { it.type == account.coinType })
                applyEnergyQuote(quote)
                if (!quote.balanceOk && !quote.balanceMessage.isNullOrBlank()) {
                    energyError.postValue(quote.balanceMessage)
                }
            } catch (e: EnergyServiceException) {
                applyEnergyQuote(null)
                energyError.postValue(if (e.isInsufficientBalance) context.getString(R.string.error_energy_service_insufficient_balance, account.coinType.symbol) else e.message)
            } catch (e: Exception) {
                applyEnergyQuote(null)
                energyError.postValue(context.getString(R.string.error_energy_service_unavailable))
            } finally {
                energyLoading.postValue(false)
            }
        }
    }

    private fun applyEnergyQuote(quote: EnergyFeeQuote?) {
        val manager = energyService
        val trc20 = account as TRC20Account
        currentQuote = quote
        energyQuote.postValue(quote)
        if (quote == null || manager == null) {
            energyFeeText.postValue("")
            energyEstimatedTime.postValue("")
        } else {
            energyFeeText.postValue(manager.feeInAccountUnits(trc20, quote).toStringWithUnit())
            energyEstimatedTime.postValue(quote.estimatedTimeSec?.let { context.getString(R.string.tron_energy_estimated_time, formatSeconds(it)) } ?: "")
        }
        // No TRX is burned by this account: fee 0 for display/max-spendable, TRX balance not required.
        val feeLimit = energyFeeLimit()
        feeEstimation = FeeEstimationsGeneric(Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Value.zeroValue(feeCoin), Date().time)
        notEnoughTrx.postValue(false)
        transactionData.postValue(TronTransactionData(feeLimit = feeLimit, estimatedFee = BigInteger.ZERO, checkFeeBalance = false,
            expirationMs = EnergyServiceManager.ENERGY_TX_EXPIRATION_MS))
        selectedFee.postValue(Value.zeroValue(feeCoin))
        // the quote changes what is spendable — rebuild
        txRebuildPublisher.onNext(Unit)
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

    /** Current quote (for the send flow). */
    fun currentEnergyQuote(): EnergyFeeQuote? = currentQuote

    override fun validateExtra(toSend: Value): TransactionStatus? {
        if (!isEnergyMode()) return null
        val fee = energyFeeValue() ?: return TransactionStatus.MISSING_ARGUMENTS
        // The partner broadcasts the fee first: if amount + fee don't fit, the fee lands and the
        // transfer reverts — refuse upfront.
        if ((toSend + fee) > account.accountBalance.spendable) return TransactionStatus.INSUFFICIENT_FUNDS
        return null
    }

    override fun isZeroFeeAllowed() = true

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
}

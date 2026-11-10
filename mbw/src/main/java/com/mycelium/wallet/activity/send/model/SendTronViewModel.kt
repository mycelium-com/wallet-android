package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.content.Intent
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wallet.activity.util.get
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.tron.TronUri
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.tron.TronAccount
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronTransaction
import com.mycelium.wapi.wallet.tron.energy.EnergyFeeQuote
import com.mycelium.wapi.wallet.tron.energy.EnergyTransferResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.regex.Pattern

open class SendTronViewModel(application: Application) : SendCoinsViewModel(application) {
    override val uriPattern = Pattern.compile("[a-zA-Z0-9]+")!!
    val toaster = Toaster(application)
    private val energyScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun init(account: WalletAccount<*>, intent: Intent) {
        super.init(account, intent)
        model = SendTronModel(getApplication(), account, intent)
    }

    override fun processAssetUri(uri: AssetUri) {
        val tronUri = uri as TronUri
        if (tronUri.asset != null) {
            val token = mbwManager.supportedTRC20Tokens.values.firstOrNull { it.contractAddress.equals(tronUri.asset, true) }
            if (token != null) {
                if (getAccount() !is TRC20Account || token.id != getAccount().coinType.id) {
                    toaster.toast(context.getString(R.string.payment_uri_wrong_account_erc20, token.name), false)
                } else {
                    setParams(uri)
                }
            } else {
                toaster.toast(R.string.payment_uri_unsupported_token, false)
            }
        } else {
            if (getAccount() !is TronAccount) {
                toaster.toast(R.string.payment_uri_wrong_account_trx, false)
            } else {
                setParams(uri)
            }
        }
    }

    private fun setParams(uri: AssetUri) {
        model.receivingAddress.value = uri.address
        model.transactionLabel.value = uri.label
        if (uri.value?.isPositive() == true) {
            if (!Value.isNullOrZero(model.amount.value)) {
                toaster.toast(R.string.amount_changed, false)
            }
            model.amount.value = uri.value
        }
    }

    private val tronModel get() = model as SendTronModel

    fun getParentAccount() = tronModel.parentAccount
    fun isTrc20() = tronModel.isTrc20
    fun getTronFee() = tronModel.tronFee
    fun getFeeDetails() = tronModel.feeDetails
    fun getEstimating() = tronModel.estimating
    fun getNotEnoughTrx() = tronModel.notEnoughTrx
    fun getDenomination() = mbwManager.getDenomination(model.account.basedOnCoinType)

    // energy service
    fun getFeeMode() = tronModel.feeMode
    fun getEnergyServiceAvailable() = tronModel.energyServiceAvailable
    fun getHasTrxForFee() = tronModel.hasTrxForFee
    fun getEnergyFeeText() = tronModel.energyFeeText
    fun getEnergyEstimatedTime() = tronModel.energyEstimatedTime
    fun getEnergyLoading() = tronModel.energyLoading
    fun getEnergyError() = tronModel.energyError
    fun tokenSymbol(): String = model.account.coinType.symbol
    fun selectTrxFee() = tronModel.setFeeMode(TronFeeMode.TRX)
    fun selectEnergyFee() = tronModel.setFeeMode(TronFeeMode.ENERGY_SERVICE)

    fun fee(value: Value?): String = value?.toStringWithUnit(getDenomination()) ?: ""

    fun feeFiat(value: Value?): String {
        val rate = mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(model.account.basedOnCoinType))?.toStringWithUnit()
        return if (rate != null) "~$rate" else ""
    }

    fun energyFeeFiat(): String {
        val fee = tronModel.energyFeeValue() ?: return ""
        val rate = mbwManager.exchangeRateManager.get(fee, mbwManager.getFiatCurrency(model.account.coinType))?.toStringWithUnit()
        return if (rate != null) "~$rate" else ""
    }

    fun parentBalance(): String = getParentAccount()?.accountBalance?.spendable?.toStringFriendlyWithUnit(getDenomination()) ?: ""

    fun feeInfoClickListener() {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setMessage(if (isTrc20()) R.string.tron_fee_trc20_info else R.string.tron_fee_info)
            .setTitle(R.string.tron_fee_info_title)
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    fun energyServiceInfoClickListener() {
        val symbol = tokenSymbol()
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setTitle(context.getString(R.string.energy_service_info_dialog_title, symbol))
            .setMessage(context.getString(R.string.energy_service_info_dialog_message, symbol, symbol, symbol))
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    fun energyFeeInfoClickListener() {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setTitle(R.string.energy_service_fee_info_dialog_title)
            .setMessage(R.string.energy_service_fee_info_dialog_message)
            .setPositiveButton(R.string.button_ok, null)
            .create()
            .show()
    }

    override fun sendTransaction(activity: Activity) {
        if (tronModel.isEnergyMode()) {
            // The fee on screen right now is what the user confirms; capture it before the PIN.
            val approved = tronModel.approvedEnergyQuote()
            if (approved == null) {
                toaster.toast(R.string.error_energy_quote_expired, false)
                return
            }
            if (isColdStorage()) {
                sendViaEnergyService(activity, approved)
            } else {
                mbwManager.runPinProtectedFunction(activity) { sendViaEnergyService(activity, approved) }
            }
            return
        }
        if (isColdStorage() || model.account is HDAccountExternalSignature) {
            model.signTransaction(activity)
        } else {
            mbwManager.runPinProtectedFunction(activity) {
                model.signTransaction(activity)
            }
        }
    }

    /**
     * Energy service flow: two token transfers (fee → partner, amount → recipient) are signed
     * here and handed to the partner, which sponsors the energy and broadcasts them. Progress is
     * shown until the partner reports the order terminal.
     */
    private fun sendViaEnergyService(activity: Activity, approved: EnergyFeeQuote) {
        val account = model.account as? TRC20Account ?: return
        val manager = mbwManager.energyServiceManager ?: return
        if (tronModel.energyOrderPending.value == true) {
            toaster.toast(R.string.error_energy_order_in_flight, false)
            return
        }
        val recipient = model.receivingAddress.value as? TronAddress ?: return
        val feeLimit = tronModel.energyFeeLimit()

        val progress = ProgressDialog(activity).apply {
            setCancelable(false)
            setMessage(context.getString(R.string.energy_processing_signing))
            show()
        }
        energyScope.launch {
            // The quote on screen prices the fee; the partner binds a quote to the amount it was
            // asked about, so the transfer is quoted again here for exactly what is being sent.
            val sendQuote = withContext(Dispatchers.IO) { tronModel.quoteForSend(approved) }
            if (sendQuote == null) {
                progress.dismiss()
                toaster.toast(R.string.error_energy_quote_expired, false)
                return@launch
            }
            val (quote, amount) = sendQuote
            val result = withContext(Dispatchers.IO) {
                manager.execute(account, quote, approved, recipient, amount, feeLimit, AesKeyCipher.defaultKeyCipher()) { status ->
                    energyScope.launch { progress.setMessage(statusText(status)) }
                }
            }
            progress.dismiss()
            mbwManager.tronFeeEstimator.invalidateCache(account.receivingAddress.addressString)
            when (result) {
                is EnergyTransferResult.Success -> {
                    mbwManager.getWalletManager(false).startSynchronization(SyncMode.NORMAL_FORCED, listOf(account, account.tronAcc))
                    finishAsSent(activity, result.txHash)
                }
                is EnergyTransferResult.Pending -> {
                    AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
                        .setMessage(R.string.error_energy_order_still_processing)
                        .setPositiveButton(R.string.button_ok) { _, _ ->
                            activity.setResult(Activity.RESULT_CANCELED)
                            activity.finish()
                        }
                        .setCancelable(false)
                        .create().show()
                }
                is EnergyTransferResult.QuoteExpired -> showError(activity, context.getString(R.string.error_energy_quote_expired))
                is EnergyTransferResult.FeeChanged -> {
                    tronModel.showRepricedQuote(result.quote)
                    showError(activity, context.getString(R.string.error_energy_fee_changed,
                        manager.feeInAccountUnits(account, result.quote).toStringWithUnit()))
                }
                is EnergyTransferResult.InsufficientBalance -> showError(activity, context.getString(R.string.error_energy_service_insufficient_balance, tokenSymbol()))
                is EnergyTransferResult.Error -> showError(activity, context.getString(R.string.tron_energy_service_error, result.message))
            }
        }
    }

    private fun statusText(status: String): String = when (status) {
        "submitting" -> context.getString(R.string.energy_processing_submitting)
        "energy_purchasing", "energy_purchased" -> context.getString(R.string.energy_processing_energy_purchasing)
        "broadcasting", "broadcast" -> context.getString(R.string.energy_processing_broadcasting)
        else -> context.getString(R.string.energy_processing_waiting)
    }

    private fun showError(activity: Activity, message: String) {
        AlertDialog.Builder(activity, R.style.MyceliumModern_Dialog)
            .setTitle(R.string.tron_energy_service_title)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok, null)
            .create().show()
    }

    /** Hands over to the activity's normal "sent" path so labels/results are handled as for any send. */
    private fun finishAsSent(activity: Activity, txHash: String) {
        val account = model.account
        val done = TronTransaction(account.basedOnCoinType, model.receivingAddress.value.toString(),
            Value.zeroValue(account.basedOnCoinType), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)
        done.txHash = com.mrd.bitlib.util.HexUtils.toBytes(txHash)
        model.signedTransaction = done
        (activity as? SendCoinsActivity)?.broadcastResult(BroadcastResult(BroadcastResultType.SUCCESS))
    }

    override fun getFeeFormatter() = EthFeeFormatter()

    override fun onCleared() {
        (model as? SendTronModel)?.release()
        super.onCleared()
    }
}

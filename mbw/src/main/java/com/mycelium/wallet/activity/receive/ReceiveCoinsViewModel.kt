package com.mycelium.wallet.activity.receive

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue

abstract class ReceiveCoinsViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: ReceiveCoinsModel
    protected lateinit var account: WalletAccount
    var hasPrivateKey: Boolean = false

    open fun init(account: WalletAccount, hasPrivateKey: Boolean, showIncomingUtxo: Boolean = false) {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        this.account = account
        this.hasPrivateKey = hasPrivateKey
    }

    open fun loadInstance(savedInstanceState: Bundle) {
        model.loadInstance(savedInstanceState)
    }

    open fun saveInstance(outState: Bundle) {
        model.saveInstance(outState)
    }

    abstract fun getHint(): String

    abstract fun getFormattedValue(sum: CurrencyValue): String

    abstract fun getCurrencyName(): String

    override fun onCleared() = model.onCleared()

    fun isInitialized() = ::model.isInitialized

    fun isReceivingAmountWrong() = model.receivingAmountWrong

    fun getCurrentlyReceivingFormatted() = Transformations.map(model.receivingAmount) {
        getFormattedValue(it ?: ExactBitcoinValue.ZERO)
    }

    fun getCurrentlyReceivingAmount() = model.receivingAmount

    fun getRequestedAmount() = model.amountData

    fun getReceivingAddress() = model.receivingAddress

    fun getRequestedAmountFormatted() = Transformations.map(model.amountData) {
        if (!CurrencyValue.isNullOrZero(it)) {
            getFormattedValue(it!!)
        } else {
            ""
        }
    }

    fun getRequestedAmountAlternative() = model.alternativeAmountData

    fun getRequestedAmountAlternativeFormatted() = Transformations.map(model.alternativeAmountData) {
        if (!CurrencyValue.isNullOrZero(it)) {
            "~ " + getFormattedValue(it!!)
        } else {
            ""
        }
    }

    fun isNfcAvailable() = model.nfc?.isNdefPushEnabled == true

    fun getNfc() = model.nfc

    fun getPaymentUri() = model.getPaymentUri()

    fun shareRequest() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val titleId = if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bitcoin_address_title))
            intent.putExtra(Intent.EXTRA_TEXT, model.receivingAddress.value.toString())
            R.string.share_bitcoin_address
        } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.payment_request))
            intent.putExtra(Intent.EXTRA_TEXT, getPaymentUri())
            R.string.share_payment_request
        }
        context.startActivity(Intent.createChooser(intent, context.getString(titleId))
                .addFlags(FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyToClipboard() {
        val text = if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            model.receivingAddress.value.toString()
        } else {
            getPaymentUri()
        }
        Utils.setClipboardString(text, context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun setAmount(amount: CurrencyValue) {
        model.setAmount(amount)
    }

    fun onEnterClick(activity: AppCompatActivity) {
        val amount = model.amountData
        val amountToReceive = if (CurrencyValue.isNullOrZero(amount.value)) {
            ExactCurrencyValue.from(null, mbwManager.selectedAccount.accountDefaultCurrency)
        } else {
            // call the amountData activity with the exact amountData, so that the user sees the same amountData he had entered
            // it in non-BTC
            amount.value!!.exactValueIfPossible
        }
        GetAmountActivity.callMeToReceive(activity, amountToReceive,
                GET_AMOUNT_RESULT_CODE, AccountDisplayType.getAccountType(model.account))
    }

    companion object {
        const val GET_AMOUNT_RESULT_CODE = 1
    }
}
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
import com.mycelium.wapi.wallet.coins.Value

abstract class ReceiveCoinsViewModel(val context: Application) : AndroidViewModel(context) {
    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: ReceiveCoinsModel
    protected lateinit var account: WalletAccount<*,*>
    var hasPrivateKey: Boolean = false

    open fun init(account: WalletAccount<*,*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean = false) {
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

    abstract fun getFormattedValue(sum: Value): String

    abstract fun getTitle(): String

    abstract fun getCurrencyName(): String

    override fun onCleared() = model.onCleared()

    fun isInitialized() = ::model.isInitialized

    fun isReceivingAmountWrong() = model.receivingAmountWrong

    fun getCurrentlyReceivingFormatted() = Transformations.map(model.receivingAmount) {
        if (it != null) {
            getFormattedValue(it)
        } else {
            getFormattedValue(Value.zeroValue(mbwManager.selectedAccount.coinType))
        }
    }

    fun getCurrentlyReceivingAmount() = model.receivingAmount

    fun getRequestedAmount() = model.amountData

    fun getReceivingAddress() = model.receivingAddress

    fun getRequestedAmountFormatted() = Transformations.map(model.amountData) {
        if (!Value.isNullOrZero(it)) {
            getFormattedValue(it!!)
        } else {
            ""
        }
    }

    fun getRequestedAmountAlternative() = model.alternativeAmountData

    fun getRequestedAmountAlternativeFormatted() = Transformations.map(model.alternativeAmountData) {
        if (!Value.isNullOrZero(it)) {
            "~ " + getFormattedValue(it!!)
        } else {
            ""
        }
    }

    fun isNfcAvailable() = model.nfc?.isNdefPushEnabled == true

    fun getNfc() = model.nfc

    fun getPaymentUri() = model.getPaymentUri()

    fun shareRequest() {
        val s = Intent(Intent.ACTION_SEND)
        s.type = "text/plain"
        if (Value.isNullOrZero(model.amountData.value)) {
            s.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bitcoin_address_title))
            s.putExtra(Intent.EXTRA_TEXT, model.receivingAddress.value.toString())
            context.startActivity(Intent.createChooser(s, context.getString(R.string.share_bitcoin_address))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        } else {
            s.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.payment_request))
            s.putExtra(Intent.EXTRA_TEXT, getPaymentUri())
            context.startActivity(Intent.createChooser(s, context.getString(R.string.share_payment_request))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun copyToClipboard() {
        val text = if (Value.isNullOrZero(model.amountData.value)) {
            model.receivingAddress.value.toString()
        } else {
            getPaymentUri()
        }
        Utils.setClipboardString(text, context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun setAmount(amount: Value) {
        model.setAmount(amount)
    }

    fun onEnterClick(activity: AppCompatActivity) {
        val amount = model.amountData
        if (Value.isNullOrZero(amount.value)) {
            GetAmountActivity.callMeToReceive(activity, Value.zeroValue(mbwManager.selectedAccount.coinType),
                    GET_AMOUNT_RESULT_CODE, AccountDisplayType.getAccountType(model.account))
        } else {
            // call the amountData activity with the exact amountData, so that the user sees the same amountData he had entered
            // it in non-BTC
            GetAmountActivity.callMeToReceive(activity, amount.value,
                    GET_AMOUNT_RESULT_CODE, AccountDisplayType.getAccountType(model.account))
        }
    }

    companion object {
        const val GET_AMOUNT_RESULT_CODE = 1
    }
}
package com.mycelium.wallet.activity.receive

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.mrd.bitlib.model.Address
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
    var receivingAddress: MutableLiveData<Address> = MutableLiveData()
    var hasPrivateKey: Boolean = false

    open fun init(account: WalletAccount, hasPrivateKey: Boolean, showIncomingUtxo: Boolean = false) {
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }
        this.account = account
        this.receivingAddress.value = account.receivingAddress.get()
        this.hasPrivateKey = hasPrivateKey
    }

    abstract fun getHint(): String

    abstract fun getFormattedValue(sum: CurrencyValue): String

    abstract fun getTitle(): String

    abstract fun getCurrencyName(): String

    override fun onCleared() = model.onCleared()

    fun isInitialized() = ::model.isInitialized

    fun isReceivingAmountWrong() = model.receivingAmountWrong

    fun getCurrentlyReceivingFormatted() = Transformations.map(model.receivingAmount) {
        if (it != null) {
            getFormattedValue(it)
        } else {
            getFormattedValue(ExactBitcoinValue.ZERO)
        }
    }

    fun getCurrentlyReceivingAmount() = model.receivingAmount

    fun getRequestedAmount() = model.amountData

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

    @Suppress("unused") // used for data binding
    fun shareRequest() {
        val s = Intent(Intent.ACTION_SEND)
        s.type = "text/plain"
        if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            s.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bitcoin_address_title))
            s.putExtra(Intent.EXTRA_TEXT, receivingAddress.value.toString())
            context.startActivity(Intent.createChooser(s, context.getString(R.string.share_bitcoin_address))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        } else {
            s.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.payment_request))
            s.putExtra(Intent.EXTRA_TEXT, getPaymentUri())
            context.startActivity(Intent.createChooser(s, context.getString(R.string.share_payment_request))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        }
    }

    @Suppress("unused") // used for data binding
    fun copyToClipboard() {
        val text = if (CurrencyValue.isNullOrZero(model.amountData.value)) {
            receivingAddress.value.toString()
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
        if (CurrencyValue.isNullOrZero(amount.value)) {
            GetAmountActivity.callMeToReceive(activity, ExactCurrencyValue.from(null,
                    mbwManager.selectedAccount.accountDefaultCurrency),
                    GET_AMOUNT_RESULT_CODE, AccountDisplayType.getAccountType(model.account))
        } else {
            // call the amountData activity with the exact amountData, so that the user sees the same amountData he had entered
            // it in non-BTC
            GetAmountActivity.callMeToReceive(activity, amount.value!!.exactValueIfPossible,
                    GET_AMOUNT_RESULT_CODE, AccountDisplayType.getAccountType(model.account))
        }
    }

    companion object {
        const val GET_AMOUNT_RESULT_CODE = 1
    }
}
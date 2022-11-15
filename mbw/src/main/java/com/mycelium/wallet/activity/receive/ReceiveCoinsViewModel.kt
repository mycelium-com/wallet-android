package com.mycelium.wallet.activity.receive

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.fio.requests.FioRequestCreateActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity.Companion.MANUAL_ENTRY_RESULT_CODE
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioModule
import java.util.*
import java.util.concurrent.TimeUnit

abstract class ReceiveCoinsViewModel(application: Application) : AndroidViewModel(application) {
    protected val mbwManager = MbwManager.getInstance(application)
    protected lateinit var model: ReceiveCoinsModel
    protected lateinit var account: WalletAccount<*>
    protected val context: Context = application
    var hasPrivateKey: Boolean = false
    val isNfcAvailable = MutableLiveData<Boolean>()

    open fun init(account: WalletAccount<*>, hasPrivateKey: Boolean, showIncomingUtxo: Boolean = false) {
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

    open fun getHint(): String = context.getString(R.string.amount_hint_denomination,
            mbwManager.getDenomination(account.coinType).getUnicodeString(account.coinType.symbol))

    abstract fun getFormattedValue(sum: Value): String

    abstract fun getTitle(): String

    abstract fun getCurrencyName(): String

    fun getCurrencySymbol():String = account.coinType.symbol

    override fun onCleared() = model.onCleared()

    fun isInitialized() = ::model.isInitialized

    fun isReceivingAmountWrong() = model.receivingAmountWrong

    fun getCurrentlyReceivingFormatted() = Transformations.map(model.receivingAmount) {
        getFormattedValue(it ?: Value.zeroValue(mbwManager.selectedAccount.coinType))
    }

    fun getCurrentlyReceivingAmount() = model.receivingAmount

    fun getRequestedAmount() = model.amount

    fun getReceivingAddress() = model.receivingAddress

    fun getFioNameList() = model.fioNameList

    fun getRequestedAmountFormatted() = Transformations.map(model.amount) {
        if (!Value.isNullOrZero(it)) {
            it?.toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            ""
        }
    }

    fun getRequestedAmountAlternative() = model.alternativeAmountData

    fun getCourseOutdated() = model.alternativeAmountWarning

    fun getRequestedAmountAlternativeFormatted() = Transformations.map(model.alternativeAmountData) {
        if (!Value.isNullOrZero(it)) {
            "~ " + it?.toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            ""
        }
    }

    fun checkNfcAvailable() {
        isNfcAvailable.value = model.nfc?.isEnabled == true
    }

    fun getNfc() = model.nfc

    fun getPaymentUri() = model.getPaymentUri()

    fun shareRequest() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        if (Value.isNullOrZero(model.amount.value)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bitcoin_address_title))
            intent.putExtra(Intent.EXTRA_TEXT, model.receivingAddress.value.toString())
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_bitcoin_address))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.payment_request))
            intent.putExtra(Intent.EXTRA_TEXT, getPaymentUri())
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_payment_request))
                    .addFlags(FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun shareFioNameRequest() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"

        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.fio_name))
        intent.putExtra(Intent.EXTRA_TEXT, model.receivingFioName.value.toString())
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_fio_name))
                .addFlags(FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyToClipboard() {
        val text = if (Value.isNullOrZero(model.amount.value)) {
            model.receivingAddress.value.toString()
        } else {
            getPaymentUri()
        }
        Utils.setClipboardString(text, context)
        Toaster(context).toast(R.string.copied_to_clipboard, true)
    }

    fun copyFioNameToClipboard() {
        val text = model.receivingFioName.value.toString()
        Utils.setClipboardString(text, context)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    fun onFioNameSelected(value: Any?) {
        model.receivingFioName.value = value?.toString()
    }

    fun setAmount(amount: Value) {
        if (amount.type == account.coinType) {
            model.setAmount(amount)
            val rate = mbwManager.exchangeRateManager.getRate(amount, mbwManager.getFiatCurrency(account.coinType))
            model.setAlternativeAmount(rate.getValue(amount, mbwManager.getFiatCurrency(account.coinType))  
                    ?: Value.zeroValue(account.coinType))
            model.setAlternativeAmountWarning(rate.isRateOld)
        } else {
            val rate = mbwManager.exchangeRateManager.getRate(amount, account.coinType)
            model.setAmount(rate.getValue(amount, account.coinType)
                    ?: Value.zeroValue(account.coinType))
            model.setAlternativeAmount(amount)
            model.setAlternativeAmountWarning(rate.isRateOld)
        }
    }

    fun onEnterClick(activity: AppCompatActivity) {
        val amount = model.amount
        if (Value.isNullOrZero(amount.value)) {
            GetAmountActivity.callMeToReceive(activity, Value.zeroValue(mbwManager.selectedAccount.coinType),
                    GET_AMOUNT_RESULT_CODE, model.account.coinType)
        } else {
            // call the amount activity with the exact amount, so that the user sees the same amount he had entered
            // it in non-BTC
            GetAmountActivity.callMeToReceive(activity, amount.value,
                    GET_AMOUNT_RESULT_CODE, model.account.coinType)
        }
    }

    var fioAddressForRequest = ""
    var addressResult: Address? = null

    private val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule

    val hasFioAccounts = fioModule.getAccounts().isNotEmpty()

    open fun processReceivedResults(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GET_AMOUNT_RESULT_CODE) {
                // Get result from address chooser (may be null)
                val amount = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as Value?
                amount?.let {
                    setAmount(amount)
                }
            } else if (requestCode == MANUAL_ENTRY_RESULT_CODE && !data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO).isNullOrBlank()) {
                fioAddressForRequest = data?.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO)!!
                addressResult = data.getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME)!! as Address
                val value = getRequestedAmount().value
                if (fioModule.getFIONames(account).isNotEmpty()) {
                    FioRequestCreateActivity.start(activity, value, model.receivingFioName.value!!,
                            fioAddressForRequest, addressResult, mbwManager.selectedAccount.id)
                    activity.finish()
                } else {
                    AccountMappingActivity.startForMapping(activity, account, ReceiveCoinsActivity.REQUEST_CODE_FIO_NAME_MAPPING)
                }
            } else if (requestCode == ReceiveCoinsActivity.REQUEST_CODE_FIO_NAME_MAPPING) {
                if (fioModule.getFIONames(account).isNotEmpty()) {
                    FioRequestCreateActivity.start(activity, getRequestedAmount().value,
                            model.receivingFioName.value!!, fioAddressForRequest, addressResult, mbwManager.selectedAccount.id)
                }
            }
        }
    }

    fun createFioRequest(activity: Activity) {
        val intent = Intent(activity, ManualAddressEntry::class.java)
                .putExtra(ManualAddressEntry.FOR_FIO_REQUEST, true)
        activity.startActivityForResult(intent, MANUAL_ENTRY_RESULT_CODE)
    }

    companion object {
        const val GET_AMOUNT_RESULT_CODE = 1
    }
}

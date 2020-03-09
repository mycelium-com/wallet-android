package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.mrd.bitlib.crypto.HdKeyNode
import com.mycelium.paymentrequest.PaymentRequestException
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.GetAmountActivity
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.activity.modern.AddressBookFragment
import com.mycelium.wallet.activity.pop.PopActivity
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.VerifyPaymentRequestActivity
import com.mycelium.wallet.activity.util.*
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.content.GenericAssetUriParser
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import com.mycelium.wapi.wallet.coins.Value
import com.squareup.otto.Subscribe
import org.bitcoin.protocols.payments.PaymentACK
import java.util.*
import java.util.regex.Pattern

abstract class SendCoinsViewModel(val context: Application) : AndroidViewModel(context) {
    var activityResultDialog: DialogFragment? = null
    var activity: Activity? = null
    lateinit var amountHint: String
        private set

    protected val mbwManager = MbwManager.getInstance(context)!!
    protected lateinit var model: SendCoinsModel
    protected var progressDialog: ProgressDialog? = null

    abstract val uriPattern: Pattern
    private var receivingAcc: UUID? = null
    private var xpubSyncing: Boolean = false

    // As ottobus does not support inheritance listener should be incapsulated into an object
    private val eventListener = object : Any() {
        @Subscribe
        fun syncStopped(event: SyncStopped) {
            if (xpubSyncing) {
                xpubSyncing = false
                val account = mbwManager.getWalletManager(true).getAccount(receivingAcc!!)
                model.receivingAddress.value = account!!.receiveAddress
                progressDialog?.dismiss()
            }
        }

        @Subscribe
        fun syncFailed(event: SyncFailed) {
            progressDialog?.dismiss()
            makeText(activity, R.string.warning_sync_failed_reusing_first, LENGTH_LONG).show()
        }

        @Subscribe
        fun paymentRequestException(ex: PaymentRequestException) {
            //todo: maybe hint the user, that the merchant might broadcast the transaction later anyhow
            // and we should move funds to a new address to circumvent it
            Utils.showSimpleMessageDialog(activity,
                    String.format(context.getString(R.string.payment_request_error_while_getting_ack), ex.message))
        }

        @Subscribe
        fun paymentRequestAck(paymentACK: PaymentACK?) {
            if (paymentACK != null) {
                activityResultDialog = BroadcastDialog.create(model.account,
                        model.isColdStorage, model.signedTransaction!!)
            }
        }
    }


    open fun init(account: WalletAccount<*>,
                  intent: Intent) {

        amountHint = context.getString(R.string.amount_hint_denomination,
                mbwManager.getDenomination(account.coinType).getUnicodeString(account.coinType.symbol))
        if (::model.isInitialized) {
            throw IllegalStateException("This method should be called only once.")
        }

        MbwManager.getEventBus().register(eventListener)
    }

    abstract fun sendTransaction(activity: Activity)

    abstract fun getFeeFormatter(): FeeFormatter

    fun getSelectedFee() = model.selectedFee

    fun getFeeLvl() = model.feeLvl

    fun getTransactionStatus() = model.transactionStatus

    fun isSendScrollDefault() = model.sendScrollDefault

    fun isSpendingUnconfirmed() = model.spendingUnconfirmed

    fun canSpend() = model.account.canSpend()

    fun getFeeDataset() = model.feeDataset

    fun getFeeLvlItems() = model.getFeeLvlItems()

    fun getClipboardUri() = model.clipboardUri

    fun getErrorText() = model.errorText

    fun getAmount() = model.amount

    fun getAccount() = model.account

    fun isColdStorage() = model.isColdStorage

    fun getReceivingAddress() = model.receivingAddress

    fun getReceivingAddressText() = model.receivingAddressText

    fun getReceivingAddressAdditional() = model.receivingAddressAdditional

    fun getReceivingLabel() = model.receivingLabel

    fun getHeapWarning() = model.heapWarning

    fun getFeeWarning() = model.feeWarning

    fun getTransactionLabel() = model.transactionLabel

    fun hasPaymentRequestHandlerTransformer(): LiveData<Boolean> = Transformations.map(model.paymentRequestHandler,
            this::hasPaymentRequestHandler)

    fun hasPaymentRequestAmountTransformer(): LiveData<Boolean> = Transformations.map(model.paymentRequestHandler,
            this::hasPaymentRequestAmount)


    fun hasPaymentRequestHandler() = hasPaymentRequestHandler(model.paymentRequestHandler.value)

    private fun hasPaymentRequestHandler(paymentRequestHandler: PaymentRequestHandler?) =
            paymentRequestHandler != null

    fun hasPaymentRequestAmount() = hasPaymentRequestAmount(model.paymentRequestHandler.value)

    private fun hasPaymentRequestAmount(paymentRequestHandler: PaymentRequestHandler?) =
            paymentRequestHandler?.paymentRequestInformation?.hasAmount() ?: false


    fun isInitialized() = ::model.isInitialized

    fun getRequestedAmountFormatted() = model.amountFormatted

    fun getRequestedAmountAlternativeFormatted() = model.alternativeAmountFormatted

    fun showStaleWarning() = model.showStaleWarning

    fun getSignedTransaction() = model.signedTransaction

    fun getGenericUri() = model.genericUri

    fun getFiatValue(): String? {
        val fiat = mbwManager.exchangeRateManager.get(model.amount.value,
                mbwManager.currencySwitcher.getCurrentFiatCurrency(model.account.coinType))
        return fiat?.toStringWithUnit()
    }

    override fun onCleared() {
        MbwManager.getEventBus().unregister(eventListener)
        model.onCleared()
        super.onCleared()
    }

    fun setSendScrollDefault(default: Boolean) {
        model.sendScrollDefault = default
    }

    open fun loadInstance(savedInstanceState: Bundle) {
        model.loadInstance(savedInstanceState)
    }

    fun updateClipboardUri() {
        val string = Utils.getClipboardString(context)
                .trim()

        model.clipboardUri.value = if (uriPattern.matcher(string).matches()) {
            // Raw format
            val address = getAccount().coinType.parseAddress(string)
            if (address != null) {
                GenericAssetUriParser.createUriByCoinType(model.account.coinType, address, null, null, null)
            } else {
                null
            }
        } else {
            val uri = mbwManager.contentResolver.resolveUri(string)
            if (uri?.address?.coinType == model.account.coinType) {
                uri
            } else {
                null
            }
        }
    }

    fun onClickClipboard() {
        val uri = model.clipboardUri.value ?: return
        makeText(activity, context.getString(R.string.using_address_from_clipboard), LENGTH_SHORT).show()
        model.receivingAddress.value = uri.address
        if (uri.value != null && !uri.value!!.isNegative()) {
            model.amount.value = uri.value
        }
    }

    open fun processReceivedResults(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (requestCode == SendCoinsActivity.GET_AMOUNT_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            // Get result from AmountEntry
            val enteredAmount = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as Value?
            model.apply {
                amount.value = enteredAmount
                updateAlternativeAmount(enteredAmount)
            }
        } else if (requestCode == SendCoinsActivity.SCAN_RESULT_CODE) {
            handleScanResults(resultCode, data, activity)
        } else if (requestCode == SendCoinsActivity.ADDRESS_BOOK_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            handleAddressBookResults(data)
        } else if (requestCode == SendCoinsActivity.MANUAL_ENTRY_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            model.receivingAddress.value =
                    data!!.getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME) as GenericAddress
        } else if (requestCode == SendCoinsActivity.SIGN_TRANSACTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            model.signedTransaction =
                    (data!!.getSerializableExtra(SendCoinsActivity.SIGNED_TRANSACTION)) as GenericTransaction
            activityResultDialog = BroadcastDialog.create(model.account, model.isColdStorage, model.signedTransaction!!)
        } else if (requestCode == SendCoinsActivity.REQUEST_PAYMENT_HANDLER) {
            if (resultCode == Activity.RESULT_OK) {
                model.paymentRequestHandlerUUID = data!!.getStringExtra("REQUEST_PAYMENT_HANDLER_ID")!!
                val requestHandler = mbwManager.backgroundObjectsCache
                        .getIfPresent(model.paymentRequestHandlerUUID!!) as PaymentRequestHandler
                model.paymentRequestHandler.postValue(requestHandler)
            } else {
                // user canceled - also leave this activity
                activity.setResult(Activity.RESULT_CANCELED)
                activity.finish()
            }
        }
    }

    private fun handleScanResults(resultCode: Int, data: Intent?, activity: Activity) {
        if (resultCode != Activity.RESULT_OK) {
            val error = data?.getStringExtra(StringHandlerActivity.RESULT_ERROR)
            if (error != null) {
                makeText(activity, error, LENGTH_LONG).show()
            }
        } else {
            when (data?.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType) {
                ResultType.PRIVATE_KEY -> {
                    throw NotImplementedError("Private key must be implemented per currency")
                }
                ResultType.ADDRESS -> {
                    if (data.getAddress().coinType == getAccount().coinType) {
                        model.receivingAddress.value = data.getAddress()
                    } else {
                        makeText(activity, context.getString(R.string.not_correct_address_type), LENGTH_LONG).show()
                    }
                }
                ResultType.ASSET_URI -> {
                    val uri = data.getAssetUri()
                    if (uri.address?.coinType == getAccount().coinType) {
                        model.receivingAddress.value = uri.address
                        model.transactionLabel.value = uri.label
                        if (uri.value != null && uri.value!!.isPositive()) {
                            //we set the amount to the one contained in the qr code, even if another one was entered previously
                            if (!Value.isNullOrZero(model.amount.value)) {
                                makeText(activity, R.string.amount_changed, LENGTH_LONG).show()
                            }
                            model.amount.value = uri.value
                        }
                    } else {
                        makeText(activity, context.getString(R.string.not_correct_address_type), LENGTH_LONG).show()
                    }
                }
                ResultType.HD_NODE -> {
                    setReceivingAddressFromKeynode(data.getHdKeyNode(), activity)
                }
                ResultType.POP_REQUEST -> {
                    val popRequest = data.getPopRequest()
                    activity.startActivity(Intent(activity, PopActivity::class.java)
                            .putExtra("popRequest", popRequest)
                            .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT))
                }
                else -> {
                    throw IllegalStateException("Unexpected result type from scan: " +
                            data.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY).toString())
                }
            }
        }
    }

    private fun handleAddressBookResults(data: Intent?) {
        // Get result from address chooser
        val address = data?.getSerializableExtra(AddressBookFragment.ADDRESS_RESULT_NAME) as GenericAddress?
                ?: return
        model.receivingAddress.value = address
        if (data?.extras!!.containsKey(AddressBookFragment.ADDRESS_RESULT_LABEL)) {
            model.receivingLabel.postValue(data.getStringExtra(AddressBookFragment.ADDRESS_RESULT_LABEL))
        }
        // this is where colusend is calling tryCreateUnsigned
        // why is amountToSend not set ?
    }

    open fun saveInstance(outState: Bundle) {
        model.saveInstance(outState)
    }

    fun verifyPaymentRequest(rawPr: ByteArray, activity: Activity) {
        val intent = VerifyPaymentRequestActivity.getIntent(activity, rawPr)
        activity.startActivityForResult(intent, SendCoinsActivity.REQUEST_PAYMENT_HANDLER)
    }

    fun verifyPaymentRequest(uri: GenericAssetUri, activity: Activity) {
        val intent = VerifyPaymentRequestActivity.getIntent(activity, uri)
        activity.startActivityForResult(intent, SendCoinsActivity.REQUEST_PAYMENT_HANDLER)
    }

    fun setReceivingAddressFromKeynode(hdKeyNode: HdKeyNode, activity: Activity) {
        progressDialog = ProgressDialog.show(activity, "", context.getString(R.string.retrieving_pubkey_address), true)
        receivingAcc = mbwManager.getWalletManager(true)
                .createAccounts(UnrelatedHDAccountConfig(listOf(hdKeyNode)))[0]
        xpubSyncing = true
        if (!mbwManager.getWalletManager(true).startSynchronization(receivingAcc!!)) {
            MbwManager.getEventBus().post(SyncFailed())
        }
    }
}
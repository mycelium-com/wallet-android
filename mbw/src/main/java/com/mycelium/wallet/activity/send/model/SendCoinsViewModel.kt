package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.mrd.bitlib.crypto.HdKeyNode
import com.mycelium.paymentrequest.PaymentRequestException
import com.mycelium.view.Denomination
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
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.content.AssetUriParser
import com.mycelium.wapi.content.btc.BitcoinUri
import com.mycelium.wapi.content.btcv.BitcoinVaultUri
import com.mycelium.wapi.content.colu.mss.MSSUri
import com.mycelium.wapi.content.colu.mt.MTUri
import com.mycelium.wapi.content.colu.rmc.RMCUri
import com.mycelium.wapi.content.eth.EthUri
import com.mycelium.wapi.content.fio.FIOUri
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.fio.RecordObtData
import com.mycelium.wapi.wallet.fio.getActiveFioAccount
import com.squareup.otto.Subscribe
import org.bitcoin.protocols.payments.PaymentACK
import java.util.*
import java.util.regex.Pattern

abstract class SendCoinsViewModel(application: Application) : AndroidViewModel(application) {
    val context: Context = application
    var activityResultDialog: DialogFragment? = null
    var activity: Activity? = null
    lateinit var amountHint: String
        private set

    protected val mbwManager = MbwManager.getInstance(context)
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
            activity?.let {
                makeText(it, R.string.warning_sync_failed_reusing_first, LENGTH_LONG).show()
            }
        }

        @Subscribe
        fun paymentRequestException(ex: PaymentRequestException) {
            //todo: maybe hint the user, that the merchant might broadcast the transaction later anyhow
            // and we should move funds to a new address to circumvent it
            activity?.let {
                Utils.showSimpleMessageDialog(it,
                        String.format(context.getString(R.string.payment_request_error_while_getting_ack), ex.message))
            }
        }

        @Subscribe
        fun paymentRequestAck(paymentACK: PaymentACK?) {
            if (paymentACK != null) {
                activityResultDialog = BroadcastDialog.create(model.account,
                        model.isColdStorage, model.signedTransaction!!)
            }
        }
    }

    init {
        mbwManager.obtDataRecordCache = null
    }

    open fun init(account: WalletAccount<*>, intent: Intent) {
        amountHint = context.getString(R.string.amount_hint_denomination,
                mbwManager.getDenomination(account.coinType).getUnicodeString(account.coinType.symbol))

        MbwManager.getEventBus().register(eventListener)
    }

    abstract fun sendTransaction(activity: Activity)

    protected fun sendFioObtData() {
        // TODO: 10/7/20 redesign the whole process to have the viewModel around until after the
        //       transaction was sent.
        // We can't send it yet as the transaction is not finalized yet and as that will happen
        // after this ViewModel is disposed, we need to put that data somewhere so we can broadcast
        // that fio obt record after broadcasting the transaction.
        mbwManager
                .getWalletManager(false)
                .getActiveFioAccount(payerFioName.value ?: return)
                // If there is no FioAccount, we are done here.
                ?: return
        val tokenCode = getAccount().coinType.symbol.toUpperCase(Locale.US)
        val chainCode = if (getAccount() is ERC20Account) "ETH" else tokenCode

        if (payeeFioName.value != null) {
            mbwManager.obtDataRecordCache = RecordObtData(
                    payerFioName.value!!,
                    payeeFioName.value!!,
                    "", // TODO: fix
                    getReceivingAddress().value?.toString() ?: "no address provided",
                    getAmount().value?.toString(Denomination.UNIT)!!.toDouble(),
                    chainCode,
                    tokenCode,
                    "will be filled in after signing",
                    fioMemo.value ?: ""
            )
        }
    }

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

    val payerFioName get() = model.payerFioName

    val payeeFioName get() = model.payeeFioName

    val fioMemo get() = model.fioMemo

    fun getRecipientRepresentation() = model.recipientRepresentation

    enum class RecipientRepresentation {
        ASK, COIN, FIO
    }

    fun getReceivingAddressText() = model.receivingAddressText

    fun getReceivingAddressAdditional() = model.receivingAddressAdditional

    fun getReceivingLabel() = model.receivingLabel

    fun getHeapWarning() = model.heapWarning

    fun getFeeWarning() = model.feeWarning

    fun getTransactionLabel() = model.transactionLabel

    fun getTransactionData() = model.transactionData

    fun getTransactionDataStatus() = model.transactionDataStatus

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

    fun getCourseOutdated() = model.alternativeAmountWarning

    fun showStaleWarning() = model.showStaleWarning

    fun getTransaction() = model.transaction
    fun getSignedTransaction() = model.signedTransaction

    fun getGenericUri() = model.genericUri

    fun getFiatValue(): String? {
        val fiat = mbwManager.exchangeRateManager.get(model.amount.value,
                mbwManager.currencySwitcher.getCurrentFiatCurrency(model.account.coinType))
        return fiat?.toStringWithUnit()
    }

    override fun onCleared() {
        activity = null
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
                AssetUriParser.createUriByCoinType(model.account.coinType, address, null, null, null)
            } else {
                null
            }
        } else {
            val uri = mbwManager.contentResolver.resolveUri(string)
            if (uri != null && isUriMatchAccountCoinType(uri, model.account.coinType)) {
                uri
            } else {
                null
            }
        }
    }

    private fun isUriMatchAccountCoinType(uri: AssetUri, coinType: CryptoCurrency): Boolean {
        return when (uri) {
            is BitcoinUri -> coinType == Utils.getBtcCoinType()
            is BitcoinVaultUri -> coinType == Utils.getBtcvCoinType()
            is MTUri -> coinType == Utils.getMtCoinType()
            is MSSUri -> coinType == Utils.getMassCoinType()
            is RMCUri -> coinType == Utils.getRmcCoinType()
            is EthUri -> {
                if (uri.asset == null) {
                    coinType is EthCoin
                } else {
                    coinType is ERC20Token && uri.asset.equals(coinType.contractAddress, true)
                }
            }
            is FIOUri -> coinType == Utils.getFIOCoinType()
            else -> false
        }
    }

    fun onClickClipboard() {
        val uri = model.clipboardUri.value ?: return
        activity?.let {
            makeText(it, context.getString(R.string.using_address_from_clipboard), LENGTH_SHORT).show()
        }
        model.receivingAddress.value = uri.address
        if (uri.value != null && !uri.value!!.isNegative()) {
            model.amount.value = uri.value
        }
    }

    open fun processReceivedResults(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        if (requestCode == SendCoinsActivity.GET_AMOUNT_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            if (data?.getBooleanExtra(GetAmountActivity.EXIT_TO_MAIN_SCREEN, false) == true) {
                activity.setResult(Activity.RESULT_CANCELED)
                activity.finish()
            } else {
                // Get result from AmountEntry
                val enteredAmount = data?.getSerializableExtra(GetAmountActivity.AMOUNT) as Value?
                model.amount.value = enteredAmount ?: Value.zeroValue(model.account.coinType)
            }
        } else if (requestCode == SendCoinsActivity.SCAN_RESULT_CODE) {
            handleScanResults(resultCode, data, activity)
        } else if (requestCode == SendCoinsActivity.ADDRESS_BOOK_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            handleAddressBookResults(data)
        } else if (requestCode == SendCoinsActivity.
                MANUAL_ENTRY_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            model.receivingAddress.value =
                    data!!.getSerializableExtra(ManualAddressEntry.ADDRESS_RESULT_NAME) as Address
            model.payeeFioName.value = data.getStringExtra(ManualAddressEntry.ADDRESS_RESULT_FIO)
        } else if (requestCode == SendCoinsActivity.SIGN_TRANSACTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            model.signedTransaction =
                    (data!!.getSerializableExtra(SendCoinsActivity.SIGNED_TRANSACTION)) as Transaction
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
                    if (data.getAddress().coinType == getAccount().basedOnCoinType) {
                        model.receivingAddress.value = data.getAddress()
                    } else {
                        makeText(activity, context.getString(R.string.not_correct_address_type), LENGTH_LONG).show()
                    }
                }
                ResultType.ASSET_URI -> {
                    val uri = data.getAssetUri()
                    if (uri.address?.coinType == getAccount().basedOnCoinType) {
                        processAssetUri(uri)
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

    protected open fun processAssetUri(uri: AssetUri) {
        model.receivingAddress.value = uri.address
        model.transactionLabel.value = uri.label
        if (uri.value?.isPositive() == true) {
            //we set the amount to the one contained in the qr code, even if another one was entered previously
            if (!Value.isNullOrZero(model.amount.value)) {
                activity?.let {
                    makeText(it, R.string.amount_changed, LENGTH_LONG).show()
                }
            }
            model.amount.value = uri.value
        }
    }

    private fun handleAddressBookResults(data: Intent?) {
        // Get result from address chooser
        val address = data?.getSerializableExtra(AddressBookFragment.ADDRESS_RESULT_NAME) as Address?
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

    fun verifyPaymentRequest(uri: AssetUri, activity: Activity) {
        val intent = VerifyPaymentRequestActivity.getIntent(activity, uri)
        activity.startActivityForResult(intent, SendCoinsActivity.REQUEST_PAYMENT_HANDLER)
    }

    fun setReceivingAddressFromKeynode(hdKeyNode: HdKeyNode, activity: Activity) {
        progressDialog = ProgressDialog.show(activity, "", context.getString(R.string.retrieving_pubkey_address), true)
        receivingAcc = mbwManager.getWalletManager(true)
                .createAccounts(UnrelatedHDAccountConfig(listOf(hdKeyNode)))[0]
        xpubSyncing = true
        if (!mbwManager.getWalletManager(true).startSynchronization(receivingAcc!!)) {
            MbwManager.getEventBus().post(SyncFailed(receivingAcc))
        }
    }
}


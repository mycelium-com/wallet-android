package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.mrd.bitlib.TransactionUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.activity.send.helper.FeeItemsBuilder
import com.mycelium.wallet.activity.util.get
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler
import com.mycelium.wapi.content.AssetUri
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException
import com.squareup.otto.Subscribe
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

abstract class SendCoinsModel(
        val context: Context,
        val account: WalletAccount<*>,
        intent: Intent
) {
    val spendingUnconfirmed: MutableLiveData<Boolean> = MutableLiveData()
    val transactionLabel: MutableLiveData<String> = MutableLiveData()
    val receivingAddressText: MutableLiveData<String> = MutableLiveData()
    val receivingAddressAdditional: MutableLiveData<String> = MutableLiveData()
    val receivingLabel: MutableLiveData<String> = MutableLiveData()
    val feeDataset: MutableLiveData<List<FeeItem>> = MutableLiveData()
    val clipboardUri: MutableLiveData<AssetUri?> = MutableLiveData()
    val errorText: MutableLiveData<String> = MutableLiveData()
    val genericUri: MutableLiveData<AssetUri?> = MutableLiveData()
    val paymentFetched: MutableLiveData<Boolean> = MutableLiveData()
//    val amountFormatted: MutableLiveData<String> = MutableLiveData()
//    val alternativeAmountFormatted: MutableLiveData<String> = MutableLiveData()
    val heapWarning: MutableLiveData<CharSequence> = MutableLiveData()
    val feeWarning: MutableLiveData<CharSequence> = MutableLiveData()
    val showStaleWarning: MutableLiveData<Boolean> = MutableLiveData()
    val isColdStorage = intent.getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false)
    val recipientRepresentation = MutableLiveData(SendCoinsViewModel.RecipientRepresentation.ASK)
    val alternativeAmountWarning: MutableLiveData<Boolean> = MutableLiveData()
    val fioMemo: MutableLiveData<String?> = MutableLiveData()
    val payerFioName: MutableLiveData<String?> = MutableLiveData()

    val transactionData: MutableLiveData<TransactionData?> = object : MutableLiveData<TransactionData?>() {
        override fun setValue(value: TransactionData?) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    val transactionDataStatus: MutableLiveData<TransactionDataStatus> = object : MutableLiveData<TransactionDataStatus>() {
        override fun setValue(value: TransactionDataStatus) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    val receivingAddress: MutableLiveData<Address?> = object : MutableLiveData<Address?>() {
        override fun setValue(value: Address?) {
            if (value != this.value) {
                super.setValue(value)
                receiverChanged.onNext(Unit)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }


    val payeeFioName: MutableLiveData<String?> = object : MutableLiveData<String?>() {
        override fun setValue(value: String?) {
            if (value != this.value) {
                super.setValue(value)
                receiverChanged.onNext(Unit)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    val amount: MutableLiveData<Value> = object : MutableLiveData<Value>() {
        override fun setValue(value: Value) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
                amountUpdatePublisher.onNext(Unit)
            }
        }
    }

    val transactionStatus: MutableLiveData<TransactionStatus> = object : MutableLiveData<TransactionStatus>() {
        override fun setValue(value: TransactionStatus) {
            if (value != this.value) {
                super.setValue(value)
                amountUpdatePublisher.onNext(Unit)
            }
        }
    }
    val amountFormatted: LiveData<String> = Transformations.map(amount) {
        getRequestedAmountFormatted()
    }

    private val alternativeAmount: MutableLiveData<Value> = object : MutableLiveData<Value>() {
        override fun setValue(value: Value?) {
            if (value != this.value) {
                super.setValue(value ?: Value.zeroValue(account.coinType))
//                alternativeAmountFormatted.postValue(getRequestedAmountAlternativeFormatted())
            }
        }
    }
    val alternativeAmountFormatted: LiveData<String> = Transformations.map(alternativeAmount) {
        getRequestedAmountAlternativeFormatted()
    }

    val selectedFee = object : MutableLiveData<Value>() {
        override fun setValue(value: Value) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    val feeLvl: MutableLiveData<MinerFee> = object : MutableLiveData<MinerFee>() {
        override fun setValue(value: MinerFee) {
            if (value != this.value) {
                super.setValue(value)
                feeUpdatePublisher.onNext(Unit)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    val paymentRequestHandler: MutableLiveData<PaymentRequestHandler?> = object : MutableLiveData<PaymentRequestHandler?>() {
        override fun setValue(value: PaymentRequestHandler?) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
            }
        }
    }

    var transaction: Transaction? = null
    var signedTransaction: Transaction? = null

    // This is the list of subscriptions that must be destroyed before exiting
    val listToDispose = ArrayList<Disposable>()
    val feeUpdatePublisher: PublishSubject<Unit> = PublishSubject.create()

    var sendScrollDefault = true

    protected val mbwManager = MbwManager.getInstance(context)
    protected var feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation

    var paymentRequestHandlerUUID: String? = null
    val txRebuildPublisher: PublishSubject<Unit> = PublishSubject.create()
    private val feeItemsBuilder = FeeItemsBuilder(mbwManager.exchangeRateManager, mbwManager.getFiatCurrency(account.coinType))
    private val amountUpdatePublisher: PublishSubject<Unit> = PublishSubject.create()
    private val receiverChanged: PublishSubject<Unit> = PublishSubject.create()

    // As ottobus does not support inheritance listener should be incapsulated into an object
    private val eventListener = object : Any() {
        @Subscribe
        fun exchangeRatesRefreshed(event: ExchangeRatesRefreshed) {
            updateAlternativeAmount(amount.value)
        }

        @Subscribe
        fun selectedCurrencyChanged(event: SelectedCurrencyChanged) {
            updateAlternativeAmount(amount.value)
        }
    }

    init {
        feeLvl.value = mbwManager.getMinerFee(account.basedOnCoinType.name)
        selectedFee.value = Value.valueOf(account.basedOnCoinType, getFeeItemList()[getFeeItemList().size / 2].feePerKb)
        transactionStatus.value = TransactionStatus.MISSING_ARGUMENTS
        spendingUnconfirmed.value = false
        errorText.value = ""
        receivingAddressText.value = ""
        receivingAddressAdditional.value = ""
        receivingLabel.value = ""
//        amountFormatted.value = ""
//        alternativeAmountFormatted.value = ""
        feeWarning.value = ""
        heapWarning.value = ""

        showStaleWarning.value = feeEstimation.lastCheck < System.currentTimeMillis() - FEE_EXPIRATION_TIME
        MbwManager.getEventBus().register(eventListener)

        /**
         * This observes different events, which causes tx being rebuilt.
         * All events are merged, and only last event/result is used.
         */
        listToDispose.add(txRebuildPublisher.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .flatMap {
                    transactionStatus.postValue(TransactionStatus.BUILDING)
                    updateTransactionStatus()
                    Flowable.just(Unit)
                }
                .flatMap {
                    feeUpdatePublisher.onNext(Unit)
                    Flowable.just(Unit)
                }
                .subscribe())

        /**
         * This subscriber reacts to requirement to update fee dataset.
         */
        listToDispose.add(feeUpdatePublisher.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .switchMapCompletable {
                    feeDataset.postValue(updateFeeDataset())
                    if (selectedFee.value!!.equalZero()) {
                        feeWarning.postValue(Html.fromHtml(context.getString(R.string.fee_is_zero)))
                    }
                    Completable.complete()
                }
                .subscribe())

        /**
         * This subscriber reacts to balance and status change to correctly format amount.
         */
        listToDispose.add(amountUpdatePublisher.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .switchMapCompletable {
//                    amountFormatted.postValue(getRequestedAmountFormatted())
                    updateAlternativeAmount(amount.value)
                    Completable.complete()
                }
                .subscribe())

        /**
         * This subscriber reacts on change of receiving address.
         */
        listToDispose.add(receiverChanged.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .switchMapCompletable {
                    // See if the address is in the address book or one of our accounts
                    val receivingAddress = this.receivingAddress.value
                    val label = when {
                        receivingLabel.value?.isNotEmpty() ?: false -> receivingLabel.value!!
                        receivingAddress != null -> getAddressLabel(receivingAddress)
                        else -> ""
                    }
                    receivingLabel.postValue(label)
                    val hasPaymentRequest = paymentRequestHandler.value?.hasValidPaymentRequest() ?: false

                    updateReceiverAddressText(hasPaymentRequest)
                    updateAdditionalReceiverInfo(hasPaymentRequest)

                    val walletManager = mbwManager.getWalletManager(false)
                    heapWarning.postValue(if (receivingAddress != null && walletManager.isMyAddress(receivingAddress)) {
                        val warning = context.getString(if (walletManager.hasPrivateKey(receivingAddress)) {
                            R.string.my_own_address_warning
                        } else {
                            R.string.read_only_warning
                        })
                        Html.fromHtml(warning)
                    } else {
                        ""
                    })
                    recipientRepresentation.postValue(when {
                        payeeFioName.value != null -> SendCoinsViewModel.RecipientRepresentation.FIO
                        receivingAddress != null || hasPaymentRequest -> SendCoinsViewModel.RecipientRepresentation.COIN
                        else -> SendCoinsViewModel.RecipientRepresentation.ASK
                    })
                    Completable.complete()
                }
                .subscribe())

        alternativeAmount.value = Value.zeroValue(mbwManager.getFiatCurrency(account.coinType))
        amount.value = intent.getSerializableExtra(SendCoinsActivity.AMOUNT) as Value?
                ?: Value.zeroValue(account.coinType)
        transactionLabel.value = intent.getStringExtra(SendCoinsActivity.TRANSACTION_LABEL) ?: ""
        receivingAddress.value = intent.getSerializableExtra(SendCoinsActivity.RECEIVING_ADDRESS) as Address?
        genericUri.value = intent.getSerializableExtra(SendCoinsActivity.ASSET_URI) as AssetUri?

        this.feeDataset.value = updateFeeDataset()
    }

    abstract fun handlePaymentRequest(toSend: Value): TransactionStatus

    abstract fun getFeeLvlItems(): List<FeeLvlItem>

    open fun signTransaction(activity: Activity) {
        SignTransactionActivity.callMe(activity, account.id, isColdStorage, transaction,
                SendCoinsActivity.SIGN_TRANSACTION_REQUEST_CODE)
        receivingAddress.value?.let { address ->
            mbwManager.getWalletManager(false).getAccountByAddress(address)?.let { receivingAccount ->
                MbwManager.getEventBus().post(AccountChanged(receivingAccount))
            }
        }
    }

    open fun loadInstance(savedInstanceState: Bundle) {
        with(savedInstanceState) {
            selectedFee.value = getSerializable(SELECTED_FEE) as Value
            feeLvl.value = getSerializable(FEE_LVL) as MinerFee
            // get the payment request handler from the BackgroundObject cache - if the application
            // has restarted since it was cached, the user gets queried again
            paymentRequestHandlerUUID = getString(PAYMENT_REQUEST_HANDLER_ID)
            if (paymentRequestHandlerUUID != null) {
                paymentRequestHandler.value = mbwManager.backgroundObjectsCache
                        .getIfPresent(paymentRequestHandlerUUID) as PaymentRequestHandler
            }

            amount.value = getSerializable(SendCoinsActivity.AMOUNT) as Value
            receivingAddress.value = getSerializable(SendCoinsActivity.RECEIVING_ADDRESS) as Address?
            transactionLabel.value = getString(SendCoinsActivity.TRANSACTION_LABEL)
            genericUri.value = getSerializable(SendCoinsActivity.ASSET_URI) as AssetUri?
            signedTransaction = getSerializable(SendCoinsActivity.SIGNED_TRANSACTION) as Transaction?
        }
    }

    open fun saveInstance(outState: Bundle) {
        with(outState) {
            putSerializable(SELECTED_FEE, selectedFee.value)
            putSerializable(FEE_LVL, feeLvl.value)
            putString(PAYMENT_REQUEST_HANDLER_ID, paymentRequestHandlerUUID)

            putSerializable(SendCoinsActivity.AMOUNT, amount.value)
            putSerializable(SendCoinsActivity.RECEIVING_ADDRESS, receivingAddress.value)
            putString(SendCoinsActivity.TRANSACTION_LABEL, transactionLabel.value)
            putSerializable(SendCoinsActivity.ASSET_URI, genericUri.value)
            putSerializable(SendCoinsActivity.SIGNED_TRANSACTION, signedTransaction)
        }
    }

    fun setAlternativeAmountWarning(show: Boolean) {
        alternativeAmountWarning.value = show
    }

    /**
     * This function called on viewModel destroy to unsubscribe
     */
    fun onCleared() {
        MbwManager.getEventBus().unregister(eventListener)
        listToDispose.forEach(Disposable::dispose)
    }

    fun updateAlternativeAmount(enteredAmount: Value?) {
        val exchangeTo = if (account.coinType == enteredAmount?.type) {
            mbwManager.getFiatCurrency(account.coinType)
        } else {
            account.coinType
        }
        alternativeAmount.postValue(mbwManager.exchangeRateManager.get(mbwManager.getWalletManager(false), enteredAmount!!, exchangeTo))
        val rate = mbwManager.exchangeRateManager.getRate(enteredAmount, exchangeTo)
        alternativeAmountWarning.postValue(rate.isRateOld)
    }

    private fun updateReceiverAddressText(hasPaymentRequest: Boolean) {
        if (hasPaymentRequest) {
            val paymentRequestInformation = paymentRequestHandler.value!!.paymentRequestInformation
            val addressText = if (paymentRequestInformation.hasValidSignature()) {
                paymentRequestInformation.pkiVerificationData.displayName
            } else {
                context.getString(R.string.label_unverified_recipient)
            }
            receivingAddressText.postValue(addressText)
        } else {
            if (receivingAddress.value != null) {
                receivingAddressText.postValue(AddressUtils.toDoubleLineString(
                        this.receivingAddress.value.toString()))
            }
        }
    }

    /**
     * Show address (if available - some PRs might have more than one address or a not decodeable input)
     */
    private fun updateAdditionalReceiverInfo(hasPaymentRequest: Boolean) {
        if (hasPaymentRequest && this.receivingAddress.value != null) {
            receivingAddressAdditional.postValue(
                    AddressUtils.toDoubleLineString(this.receivingAddress.value.toString()))
        }
    }

    protected open fun estimateTxSize() = transaction?.estimatedTransactionSize ?: account.typicalEstimatedTransactionSize

    /**
     * Recalculate the transaction based on the current choices.
     */
    private fun updateTransactionStatus() {
        val txStatus = getTransactionStatus()
        transactionStatus.postValue(txStatus)
        updateErrorMessage(txStatus)
    }

    protected open fun updateErrorMessage(transactionStatus: TransactionStatus) {
        errorText.postValue(when (transactionStatus) {
            TransactionStatus.OUTPUT_TOO_SMALL -> {
                // Amount too small
                if (!Value.isNullOrZero(amount.value)) {
                    // TODO refactor: now it's btc specific but "Amount too small" status for now is possible only for btc in the app
                    context.getString(R.string.amount_too_small_short, Value.valueOf(account.coinType,
                        TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit())
                } else {
                    ""
                }
            }
            TransactionStatus.INSUFFICIENT_FUNDS -> context.getString(R.string.insufficient_funds)
            TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE -> context.getString(R.string.insufficient_funds_for_fee)
            TransactionStatus.BUILD_ERROR -> context.getString(R.string.tx_build_error)
            else -> ""
        })
    }

    private fun getRequestedAmountFormatted(): String {
        return if (Value.isNullOrZero(amount.value)) {
            ""
        } else if (transactionStatus.value == TransactionStatus.OUTPUT_TOO_SMALL
                || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS
                || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE) {
            getValueInAccountCurrency().toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            formatValue(amount.value)
        }
    }

    private fun getRequestedAmountAlternativeFormatted(): String {
        return if (transactionStatus.value == TransactionStatus.OUTPUT_TOO_SMALL
                || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS
                || transactionStatus.value == TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE) {
            ""
        } else {
            formatValue(alternativeAmount.value)
        }
    }

    private fun formatValue(value: Value?): String {
        return if (Value.isNullOrZero(value)) {
            ""
        } else {
            if (value!!.type == account.coinType) {
                value.toStringWithUnit(mbwManager.getDenomination(account.coinType))
            } else {
                "~ ${value.toStringWithUnit()}"
            }
        }
    }

    private fun getValueInAccountCurrency(): Value {
        return if (amount.value?.type == account.coinType) {
            amount.value!!
        } else {
            alternativeAmount.value!!
        }
    }

    private fun getTransactionStatus(): TransactionStatus {
        val toSend = mbwManager.exchangeRateManager.get(mbwManager.getWalletManager(false), amount.value!!, account.coinType) ?: amount.value!!

        try {
            return when {
                paymentRequestHandler.value?.hasValidPaymentRequest() == true -> {
                    handlePaymentRequest(toSend)
                }
                receivingAddress.value != null && !toSend.isZero() && transactionDataStatus.value != TransactionDataStatus.TYPING -> {
                    // createTx potentially takes long, if server interaction is involved
                    transaction = account.createTx(receivingAddress.value!!, toSend, FeePerKbFee(selectedFee.value!!), transactionData.value)
                    spendingUnconfirmed.postValue(account.isSpendingUnconfirmed(transaction!!))
                    TransactionStatus.OK
                }
                else -> TransactionStatus.MISSING_ARGUMENTS
            }
        } catch (ex: BuildTransactionException) {
            return TransactionStatus.MISSING_ARGUMENTS
        } catch (ex: OutputTooSmallException) {
            return TransactionStatus.OUTPUT_TOO_SMALL
        } catch (ex: InsufficientFundsForFeeException) {
            return TransactionStatus.INSUFFICIENT_FUNDS_FOR_FEE
        } catch (ex: InsufficientFundsException) {
            return TransactionStatus.INSUFFICIENT_FUNDS
        } catch (ex: Exception) {
            return TransactionStatus.BUILD_ERROR
        }
    }

    private fun updateFeeDataset(): List<FeeItem> {
        val feeItemList = getFeeItemList()
        if (!isInRange(feeItemList, selectedFee.value!!)) {
            selectedFee.postValue(Value.valueOf(account.basedOnCoinType, feeItemList[feeItemList.size / 2].feePerKb))
        }
        return feeItemList
    }

    private fun getFeeItemList(): List<FeeItem> {
        Logger.getLogger(SendCoinsModel::class.java.simpleName).log(Level.INFO,"Estimation send coin economy = ${feeEstimation?.economy?.toStringWithUnit()} normal = ${feeEstimation?.normal?.toStringWithUnit()}")
        return feeItemsBuilder.getFeeItemList(account.basedOnCoinType,
                feeEstimation, feeLvl.value, estimateTxSize())
    }

    private fun isInRange(feeItems: List<FeeItem>, fee: Value) =
            (feeItems.first().feePerKb <= fee.valueAsLong && fee.valueAsLong <= feeItems.last().feePerKb)

    private fun getAddressLabel(address: Address): String {
        val accountId = mbwManager.getAccountId(address, account.coinType).orNull()
        return if (accountId != null) {
            // Get the name of the account
            mbwManager.metadataStorage.getLabelByAccount(accountId)
        } else {
            // We don't have it in our accounts, look in address book, returns empty string by default
            mbwManager.metadataStorage.getLabelByAddress(address)
        }
    }

    enum class TransactionStatus {
        BUILDING, MISSING_ARGUMENTS, OUTPUT_TOO_SMALL, INSUFFICIENT_FUNDS, INSUFFICIENT_FUNDS_FOR_FEE, BUILD_ERROR, OK
    }

    enum class TransactionDataStatus {
        READY, TYPING
    }

    companion object {
        private const val SELECTED_FEE = "selectedFee"
        private const val FEE_LVL = "feeLvl"
        private const val PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId"
        // Alert the user of old fee estimations
        private val FEE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(5)
    }
}

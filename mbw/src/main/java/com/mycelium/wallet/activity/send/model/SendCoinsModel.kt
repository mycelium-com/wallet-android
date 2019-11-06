package com.mycelium.wallet.activity.send.model

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.activity.send.helper.FeeItemsBuilder
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.event.ExchangeRatesRefreshed
import com.mycelium.wallet.event.SelectedCurrencyChanged
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler
import com.mycelium.wapi.content.GenericAssetUri
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException
import com.squareup.otto.Subscribe
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.IOException
import java.util.concurrent.TimeUnit

abstract class SendCoinsModel(
        val context: Application,
        val account: WalletAccount<*>,
        intent: Intent
) {
    val spendingUnconfirmed: MutableLiveData<Boolean> = MutableLiveData()
    val transactionLabel: MutableLiveData<String> = MutableLiveData()
    val receivingAddressText: MutableLiveData<String> = MutableLiveData()
    val receivingAddressAdditional: MutableLiveData<String> = MutableLiveData()
    val receivingLabel: MutableLiveData<String> = MutableLiveData()
    val feeDataset: MutableLiveData<List<FeeItem>> = MutableLiveData()
    val clipboardUri: MutableLiveData<GenericAssetUri?> = MutableLiveData()
    val errorText: MutableLiveData<String> = MutableLiveData()
    val genericUri: MutableLiveData<GenericAssetUri?> = MutableLiveData()
    val paymentFetched: MutableLiveData<Boolean> = MutableLiveData()
    val amountFormatted: MutableLiveData<String> = MutableLiveData()
    val alternativeAmountFormatted: MutableLiveData<String> = MutableLiveData()
    val heapWarning: MutableLiveData<CharSequence> = MutableLiveData()
    val feeWarning: MutableLiveData<CharSequence> = MutableLiveData()
    val showStaleWarning: MutableLiveData<Boolean> = MutableLiveData()
    val isColdStorage = intent.getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false)


    val receivingAddress: MutableLiveData<GenericAddress?> = object : MutableLiveData<GenericAddress?>() {
        override fun setValue(value: GenericAddress?) {
            if (value != this.value) {
                super.setValue(value)
                receiverChanged.onNext(Unit)
                txRebuildPublisher.onNext(Unit)
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

    val amount: MutableLiveData<Value> = object : MutableLiveData<Value>() {
        override fun setValue(value: Value) {
            if (value != this.value) {
                super.setValue(value)
                txRebuildPublisher.onNext(Unit)
                amountUpdatePublisher.onNext(Unit)
            }
        }
    }

    val alternativeAmount: MutableLiveData<Value> = object : MutableLiveData<Value>() {
        override fun setValue(value: Value?) {
            if (value != this.value) {
                super.setValue(value ?: Value.zeroValue(account.coinType))
                alternativeAmountFormatted.postValue(getRequestedAmountAlternativeFormatted())
            }
        }
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

    var transaction: GenericTransaction? = null
    var signedTransaction: GenericTransaction? = null

    // This is the list of subscriptions that must be destroyed before exiting
    val listToDispose = ArrayList<Disposable>()
    val feeUpdatePublisher: PublishSubject<Unit> = PublishSubject.create()

    var sendScrollDefault = true

    protected val mbwManager = MbwManager.getInstance(context)!!
    private var feeEstimation = mbwManager.getFeeProvider(account.coinType).estimation

    var paymentRequestHandlerUUID: String? = null
    private val feeItemsBuilder = FeeItemsBuilder(mbwManager.exchangeRateManager, mbwManager.getFiatCurrency(account.coinType))
    private val txRebuildPublisher: PublishSubject<Unit> = PublishSubject.create()
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
        selectedFee.value = getCurrentFeeEstimation()
        feeLvl.value = mbwManager.minerFee
        transactionStatus.value = TransactionStatus.MissingArguments
        spendingUnconfirmed.value = false
        errorText.value = ""
        receivingAddressText.value = ""
        receivingAddressAdditional.value = ""
        receivingLabel.value = ""
        amountFormatted.value = ""
        alternativeAmountFormatted.value = ""
        feeWarning.value = ""
        heapWarning.value = ""
        alternativeAmount.value = Value.zeroValue(mbwManager.getFiatCurrency(account.coinType))
        amount.value = intent.getSerializableExtra(SendCoinsActivity.AMOUNT) as Value?
                ?: Value.zeroValue(account.coinType)
        showStaleWarning.value = feeEstimation.lastCheck < System.currentTimeMillis() - FEE_EXPIRATION_TIME
        MbwManager.getEventBus().register(eventListener)

        /**
         * This observes different events, which causes tx being rebuilt.
         * All events are merged, and only last event/result is used.
         */
        listToDispose.add(txRebuildPublisher.toFlowable(BackpressureStrategy.LATEST)
                .observeOn(Schedulers.computation())
                .flatMap {
                    transactionStatus.postValue(TransactionStatus.Building)
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
                    if (selectedFee.value!!.value == 0L) {
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
                    amountFormatted.postValue(getRequestedAmountFormatted())
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
                    if (receivingAddress != null && walletManager.isMyAddress(receivingAddress)) {
                        val warning = if (walletManager.hasPrivateKey(receivingAddress)) {
                            context.getString(R.string.my_own_address_warning)
                        } else {
                            context.getString(R.string.read_only_warning)
                        }
                        heapWarning.postValue(Html.fromHtml(warning))
                    }
                    Completable.complete()
                }
                .subscribe())


        transactionLabel.value = intent.getStringExtra(SendCoinsActivity.TRANSACTION_LABEL) ?: ""
        receivingAddress.value = intent.getSerializableExtra(SendCoinsActivity.RECEIVING_ADDRESS) as GenericAddress?
        genericUri.value = intent.getSerializableExtra(SendCoinsActivity.ASSET_URI) as GenericAssetUri?

        this.feeDataset.value = updateFeeDataset()
    }

    abstract fun handlePaymentRequest(toSend: Value): TransactionStatus

    abstract fun getFeeLvlItems(): List<FeeLvlItem>

    open fun signTransaction(activity: Activity) {
        SignTransactionActivity.callMe(activity, account.id, isColdStorage, transaction,
                SendCoinsActivity.SIGN_TRANSACTION_REQUEST_CODE)
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
            receivingAddress.value = getSerializable(SendCoinsActivity.RECEIVING_ADDRESS) as GenericAddress?
            transactionLabel.value = getString(SendCoinsActivity.TRANSACTION_LABEL)
            genericUri.value = getSerializable(SendCoinsActivity.ASSET_URI) as GenericAssetUri?
            signedTransaction = getSerializable(SendCoinsActivity.SIGNED_TRANSACTION) as GenericTransaction?
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
        alternativeAmount.postValue(mbwManager.exchangeRateManager.get(enteredAmount, exchangeTo))
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
            if (this.receivingAddress.value != null) {
                receivingAddressText.postValue(AddressUtils.toMultiLineString(
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

    fun estimateTxSize() = transaction?.estimatedTransactionSize ?: account.typicalEstimatedTransactionSize

    /**
     * Recalculate the transaction based on the current choices.
     */
    private fun updateTransactionStatus() {
        val txStatus = getTransactionStatus()
        transactionStatus.postValue(txStatus)
        updateErrorMessage(txStatus)
    }

    protected open fun updateErrorMessage(transactionStatus: TransactionStatus) {
        when (transactionStatus) {
            TransactionStatus.OutputTooSmall -> {
                // Amount too small
                if (!Value.isNullOrZero(amount.value)) {
                    errorText.postValue(context.getString(R.string.amount_too_small_short))
                }
            }
            TransactionStatus.InsufficientFunds -> {
                errorText.postValue(context.getString(R.string.insufficient_funds))
            }
            TransactionStatus.BuildError -> {
                errorText.postValue(context.getString(R.string.tx_build_error))
            }
            else -> errorText.postValue("")
        }
    }

    private fun getRequestedAmountFormatted(): String {
        return if (Value.isNullOrZero(amount.value)) {
            ""
        } else if (transactionStatus.value == TransactionStatus.OutputTooSmall
                || transactionStatus.value == TransactionStatus.InsufficientFunds
                || transactionStatus.value == TransactionStatus.InsufficientFundsForFee) {
            getValueInAccountCurrency().toStringWithUnit(mbwManager.getDenomination(account.coinType))
        } else {
            formatValue(amount.value)
        }
    }

    private fun getRequestedAmountAlternativeFormatted(): String {
        return if (transactionStatus.value == TransactionStatus.OutputTooSmall
                || transactionStatus.value == TransactionStatus.InsufficientFunds
                || transactionStatus.value == TransactionStatus.InsufficientFundsForFee) {
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
        val toSend = amount.value!!

        try {
            return if (paymentRequestHandler.value?.hasValidPaymentRequest() == true) {
                handlePaymentRequest(toSend)
            } else if (receivingAddress.value != null) {
                // createTx potentially takes long, if server interaction is involved
                transaction = account.createTx(receivingAddress.value, toSend, FeePerKbFee(selectedFee.value!!))
                spendingUnconfirmed.postValue(account.isSpendingUnconfirmed(transaction))
                TransactionStatus.OK
            } else {
                TransactionStatus.MissingArguments
            }
        } catch (ex: GenericBuildTransactionException) {
            return TransactionStatus.MissingArguments
        } catch (ex: GenericOutputTooSmallException) {
            return TransactionStatus.OutputTooSmall
        } catch (ex: GenericInsufficientFundsException) {
            return TransactionStatus.InsufficientFunds
        } catch (ex: IOException) {
            return TransactionStatus.BuildError
        }
    }

    private fun updateFeeDataset(): List<FeeItem> {
        val feeItemList = feeItemsBuilder.getFeeItemList(account.basedOnCoinType,
                feeEstimation, feeLvl.value, estimateTxSize())
        if (!isInRange(feeItemList, selectedFee.value!!)) {
            selectedFee.postValue(getCurrentFeeEstimation())
        }
        return feeItemList
    }

    private fun isInRange(feeItems: List<FeeItem>, fee: Value) =
            (feeItems[0].feePerKb <= fee.value && fee.value <= feeItems[feeItems.size - 1].feePerKb)

    private fun getCurrentFeeEstimation() = when (feeLvl.value) {
        MinerFee.LOWPRIO -> Value.valueOf(account.coinType, feeEstimation.low.value)
        MinerFee.ECONOMIC -> Value.valueOf(account.coinType, feeEstimation.economy.value)
        MinerFee.NORMAL -> Value.valueOf(account.coinType, feeEstimation.normal.value)
        MinerFee.PRIORITY -> Value.valueOf(account.coinType, feeEstimation.high.value)
        else -> Value.valueOf(account.coinType, feeEstimation.normal.value)
    }

    private fun getAddressLabel(address: GenericAddress): String {
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
        Building, MissingArguments, OutputTooSmall, InsufficientFunds, InsufficientFundsForFee, BuildError, OK
    }

    companion object {
        private const val SELECTED_FEE = "selectedFee"
        private const val FEE_LVL = "feeLvl"
        private const val PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId"
        private val FEE_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(2)
    }
}
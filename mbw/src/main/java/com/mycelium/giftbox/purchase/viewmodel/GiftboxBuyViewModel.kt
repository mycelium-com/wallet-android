package com.mycelium.giftbox.purchase.viewmodel

import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.*
import com.google.gson.Gson
import com.mrd.bitlib.TransactionUtils
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.common.OrderHeaderViewModel
import com.mycelium.giftbox.purchase.debounce
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.*
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*


class GiftboxBuyViewModel(val productInfo: ProductInfo) : ViewModel(), OrderHeaderViewModel {
    val gson = Gson()

    val accountId = MutableLiveData<UUID>()
    val zeroFiatValue = zeroFiatValue(productInfo)
    val orderResponse = MutableLiveData<OrderResponse>()
    val warningQuantityMessage: MutableLiveData<String> = MutableLiveData("")
    val totalProgress = MutableLiveData(false)
    val lastPriceResponse = MutableLiveData<PriceResponse>()
    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val account by lazy {
        mbwManager.getWalletManager(false).getAccount(accountId.value!!)!!
    }
    val zeroCryptoValue by lazy {
        account.coinType?.value(0)
    }

    fun getPreselectedValues(): List<Value> {
        return productInfo.availableDenominations?.map {
            Value.valueOf(getAssetInfo(), toUnits(zeroFiatValue.type, it))
        }?.sortedBy { it.value } ?: listOf()
    }

    override val productName = MutableLiveData("")
    override val expire = MutableLiveData("")
    override val country = MutableLiveData("")
    override val cardValue = MutableLiveData("")
    override val quantity = MutableLiveData(0)

    val sendTransactionAction = MutableLiveData<Unit>()
    val sendTransaction = Transformations.switchMap(sendTransactionAction) {
        callbackFlow {
            try {
                val address = when (account) {
                    is AbstractEthERC20Account -> {
                        EthAddress(Utils.getEthCoinType(), orderResponse.value!!.payinAddress!!)
                    }
                    is AbstractBtcAccount -> {
                        BtcAddress(
                                Utils.getBtcCoinType(),
                                BitcoinAddress.fromString(orderResponse.value!!.payinAddress)
                        )
                    }
                    else -> TODO("Account not supported yet")
                }
                val price = orderResponse.value?.amountExpectedFrom!!

                val createTx = account.createTx(
                        address, getCryptoAmount(price),
                        FeePerKbFee(feeEstimation.normal), null
                )
                account.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                offer(createTx!! to account.broadcastTx(createTx))
                close()
            } catch (ex: Exception) {
                offer(null to BroadcastResult(ex.localizedMessage, BroadcastResultType.REJECTED))
                cancel(CancellationException("Tx", ex))
            }
        }.asLiveData(Dispatchers.IO)
    }

    val hasDenominations = productInfo.availableDenominations.isNullOrEmpty().not()
    val quantityString: MutableLiveData<String> = MutableLiveData("1")
    val quantityInt = Transformations.map(quantityString) {
        if (it.isDigitsOnly() && !it.isNullOrBlank()) it.toInt() else 1
    }

    private val feeEstimation by lazy {
        mbwManager.getFeeProvider(account.basedOnCoinType).estimation
    }

    fun zeroFiatValue(product: ProductInfo): Value {
        return Value.zeroValue(Utils.getTypeByName(product.currencyCode)!!)
    }

    val totalAmountFiatSingle = MutableLiveData<Value>(zeroFiatValue)
    val totalAmountFiatSingleString = Transformations.map(totalAmountFiatSingle) {
        it.toStringFriendlyWithUnit()
    }

    val totalAmountCrypto: LiveData<Value> = totalAmountCrypto()
    val totalAmountCryptoSingleString = Transformations.map(totalAmountCrypto) {
        it.div(quantityInt.value?.toBigInteger() ?: BigInteger.ONE).toStringFriendlyWithUnit()
    }
    val txValid =  MutableLiveData<AmountValidation?>()

    private fun totalAmountCrypto(forSingleItem: Boolean = false) = Transformations.switchMap(
            zip2(
                    totalAmountFiatSingle,
                    quantityInt
                            .map { if (forSingleItem) 1 else it.toInt() }) { amount: Value, quantity: Int ->
                Pair(
                        amount,
                        quantity
                )
            }) {
        callbackFlow {
            txValid.value = null
            val (amount, quantity) = it
            if (quantity == 0 || amount.isZero()) {
                offer(zeroCryptoValue!!)
            } else {
                if (!forSingleItem) {
                    totalAmountFiat.value = amount.times(quantity.toLong())
                }
                if (quantity > MAX_QUANTITY) {
                    warningQuantityMessage.value = WalletApplication.getInstance()
                            .getString(R.string.max_available_cards_d, MAX_QUANTITY)
                } else {
                    if (!forSingleItem) {
                        warningQuantityMessage.value = ""
                    }
                    totalProgress.value = true
                    GitboxAPI.giftRepository.getPrice(viewModelScope,
                            code = productInfo.code ?: "",
                            quantity = quantity,
                            amount = amount.valueAsBigDecimal.toInt(),
                            currencyId = zeroCryptoValue.getCurrencyId(),
                            success = { priceResponse ->
                                if (priceResponse!!.status == PriceResponse.Status.eRROR) {
                                    return@getPrice
                                }
                                lastPriceResponse.value = priceResponse

                                val cryptoAmount = getCryptoAmount(priceResponse)
                                if (!forSingleItem) {
                                    launch(Dispatchers.IO) {
                                        val (checkValidTransaction, transaction) = checkValidTransaction(
                                                account,
                                                cryptoAmount
                                        )
                                        txValid.postValue(checkValidTransaction.state)
                                        if (checkValidTransaction.state == AmountValidation.Ok) {
                                            tempTransaction.postValue(transaction)
                                            offer(cryptoAmount)
                                        } else {
                                            warningQuantityMessage.postValue(checkValidTransaction.message)
                                        }
                                    }
                                } else {
                                    offer(cryptoAmount)
                                }
                            },
                            error = { _, error ->
                                close()
                                totalProgress.value = false
                            },
                            finally = {
                                totalProgress.value = false
                            })
                }
            }
            awaitClose { }
        }.onStart { emit(zeroCryptoValue) }.asLiveData()
    }

    val errorAmountMessage: LiveData<String> = Transformations.map(totalAmountCrypto) {
        val enough = it.lessOrEqualThan(getMaxSpendable())
        return@map if (enough) "" else WalletApplication.getInstance()
                .getString(R.string.insufficient_funds)
    }
    val errorErrorMessage: LiveData<String> = Transformations.map(txValid) {
        when(it) {
            AmountValidation.Invalid -> "Invalid"
            AmountValidation.ValueTooSmall -> "Value to small"
            AmountValidation.NotEnoughFunds -> "Not enough fund"
            else -> ""
        }
    }

    val totalAmountFiat = MutableLiveData(zeroFiatValue)
    val totalAmountFiatString = Transformations.map(totalAmountFiat) {
        return@map it?.toStringFriendlyWithUnit()
    }

    val totalAmountCryptoString = Transformations.map(totalAmountCrypto) {
        return@map "~" + it.toStringFriendlyWithUnit()
    }

    private val tempTransaction = MutableLiveData<Transaction>()

    private fun getCryptoAmount(price: PriceResponse): Value = getCryptoAmount(price.priceOffer!!)

    private fun getCryptoAmount(price: String): Value {
        val cryptoUnit = BigDecimal(price).movePointRight(account.coinType?.unitExponent!!)
                .toBigInteger()
        return Value.valueOf(account.coinType, cryptoUnit)
    }

    fun getAssetInfo() = Utils.getTypeByName(productInfo.currencyCode)!!


    val minerFeeCrypto = Transformations.map(tempTransaction) {
        it?.totalFee()
    }
    val minerFeeCryptoString = Transformations.map(minerFeeCrypto) {
        it?.toStringFriendlyWithUnit()?.let { "~$it" } ?: ""
    }
    val minerFeeFiat = Transformations.map(minerFeeCrypto) {
        mbwManager.exchangeRateManager.get(it, Utils.getTypeByName(productInfo.currencyCode)) ?: zeroFiatValue
    }
    val minerFeeFiatString = Transformations.map(minerFeeFiat) {
        if (it.lessThan(Value(it.type, 1.toBigInteger()))) {
            "<0.01 " + it.type.symbol
        } else it.toStringFriendlyWithUnit()
    }
    val maxSpendableAmount: MutableLiveData<Value> by lazy { MutableLiveData(maxFiatSpendableAmount()) }
    fun maxFiatSpendableAmount(): Value {
        return convertToFiat(getMaxSpendable()) ?: zeroFiatValue
    }

    private fun getMaxSpendable() =
            mbwManager.getWalletManager(false)
                    .getAccount(accountId.value!!)
                    ?.calculateMaxSpendableAmount(feeEstimation.normal, null, null)!!


    val isGrantedPlus =
            Transformations.map(
                    zip3(
                            totalAmountCrypto,
                            warningQuantityMessage,
                            totalProgress
                    ) { total: Value, quantityError: String, progress: Boolean ->
                        Triple(total, quantityError, progress)
                    }
            ) {
                val (total, quantityError, progress) = it
                total.plus(total.div(quantityInt.value?.toBigInteger() ?: BigInteger.ONE))
                        .lessOrEqualThan(getAccountBalance()) && quantityError.isEmpty() && !progress
            }

    val isGrantedMinus = Transformations.map(quantityInt.debounce(300)) {
        return@map it > 1
    }

    val isGranted = Transformations.map(
            zip4(
                    totalAmountCrypto,
                    totalProgress,
                    quantityInt,
                    txValid
            ) { total: Value, progress: Boolean, quantity: Int, txValid -> Quad(total, progress, quantity, txValid) }) {
        val (total, progress, quantity, txValid) = it
        return@map total.lessOrEqualThan(getAccountBalance()) && total.moreThanZero() && quantity <= MAX_QUANTITY && !progress && txValid == AmountValidation.Ok
    }

    val plusBackground = Transformations.map(isGrantedPlus) {
        ContextCompat.getDrawable(
                WalletApplication.getInstance(),
                if (!it) R.drawable.ic_plus_disabled else R.drawable.ic_plus
        )
    }

    val minusBackground = Transformations.map(isGrantedMinus) {
        ContextCompat.getDrawable(
                WalletApplication.getInstance(),
                if (!it) R.drawable.ic_minus_disabled else R.drawable.ic_minus
        )
    }

    private fun convertToFiat(value: Value): Value? {
        lastPriceResponse.value?.exchangeRate?.let {
            val fiat = value.valueAsBigDecimal.multiply(BigDecimal(it))
            return Value.valueOf(zeroFiatValue.type, toUnits(zeroFiatValue.type, fiat))
        }
        return null
    }

    private fun getAccountBalance(): Value {
        return account.accountBalance.spendable!!
    }

    //colors
    val totalAmountSingleCryptoColor = Transformations.map(totalAmountCrypto) {
        getColorByCryptoValue(it)
    }

    val totalAmountCryptoColor = Transformations.map(totalAmountCrypto) {
        getColorByCryptoValue(it)
    }

    val minerFeeCryptoColor = Transformations.map(minerFeeCrypto) {
        MutableLiveData(
                getColorByCryptoValue(it)
        )
    }

    val totalAmountFiatColor = Transformations.map(totalAmountFiat) {
        getColorByFiatValue(it)
    }

    val totalAmountFiatSingleColor = Transformations.map(totalAmountFiatSingle) {
        getColorByFiatValue(it)
    }

    val minerFeeFiatColor = Transformations.map(minerFeeFiat) {
        ContextCompat.getColor(
                WalletApplication.getInstance(),
                if (it.moreOrEqualThanZero()) R.color.white else R.color.darkgrey
        )
    }

    private fun toUnits(assetInfo: AssetInfo, amount: BigDecimal): BigInteger =
            amount.movePointRight(assetInfo.unitExponent).setScale(0, RoundingMode.HALF_UP)
                    .toBigIntegerExact()

    private fun getColorByCryptoValue(it: Value?) =
            ContextCompat.getColor(
                    WalletApplication.getInstance(),
                    if (it?.moreThanZero() == true) R.color.white_alpha_0_6 else R.color.darkgrey
            )

    private fun getColorByFiatValue(it: Value) =
            ContextCompat.getColor(
                    WalletApplication.getInstance(),
                    if (it.moreThanZero()) R.color.white else R.color.darkgrey
            )

    private fun checkValidTransaction(
            account: WalletAccount<*>,
            value: Value
    ): Pair<Status, Transaction?> {
        if (value.equalZero()) {
            return Status(AmountValidation.Ok) to null //entering a fiat value + exchange is not available
        }
        val res = WalletApplication.getInstance().resources
        return try {
            Status(AmountValidation.Ok) to account.createTx(
                    account.dummyAddress,
                    value,
                    FeePerKbFee(feeEstimation.normal),
                    null
            )
        } catch (e: OutputTooSmallException) {
            Status(AmountValidation.ValueTooSmall, res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit())) to null
        } catch (e: InsufficientFundsForFeeException) {
            if (account is ERC20Account) {
                val totalFee = feeEstimation.normal.times(account.typicalEstimatedTransactionSize.toBigInteger())
                val parentAccountBalance = account.ethAcc.accountBalance.spendable
                val fee = totalFee - parentAccountBalance
                Status(AmountValidation.NotEnoughFunds, res.getString(R.string.please_top_up_your_eth_account,
                        account.ethAcc.label, fee.toStringFriendlyWithUnit(), convert(fee)) + ExchangeViewModel.TAG_ETH_TOP_UP)
            } else {
                Status(AmountValidation.NotEnoughFunds, res.getString(R.string.insufficient_funds_for_fee))
            } to null
        } catch (e: InsufficientFundsException) {
            Status(AmountValidation.NotEnoughFunds, res.getString(R.string.insufficient_funds)) to null
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            Status(AmountValidation.Invalid, res.getString(R.string.tx_build_error) + " " + e.message) to null
        } catch (e: Exception) {
            Status(AmountValidation.Invalid, res.getString(R.string.tx_build_error) + " " + e.message) to null
        }
    }

    fun convert(value: Value) =
            " ~${mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(value.type))?.toStringFriendlyWithUnit() ?: ""}"

    data class Status(val state: AmountValidation, val message: String = "")

    enum class AmountValidation {
        Ok, ValueTooSmall, Invalid, NotEnoughFunds
    }

    companion object {
        const val MAX_QUANTITY = 19
    }
}

fun Value.getCurrencyId(): String {
    var currencyId = this.currencySymbol.removePrefix("t")
    if (currencyId.equals("usdt", true)) {
        currencyId = "usdt20"
    }
    return currencyId
}
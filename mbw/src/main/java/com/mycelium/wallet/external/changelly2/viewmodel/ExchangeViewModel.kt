package com.mycelium.wallet.external.changelly2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mrd.bitlib.TransactionUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.external.changelly.model.FixRate
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException
import kotlinx.coroutines.launch
import java.math.BigDecimal


class ExchangeViewModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    var currencies = setOf("BTC", "ETH")
    val fromAccount = MutableLiveData<WalletAccount<*>>()
    val exchangeInfo = MutableLiveData<FixRate>()
    val sellValue = object : MutableLiveData<String>() {
        override fun setValue(value: String?) {
            if (this.value != value) {
                super.setValue(value)
            }
        }
    }
    val buyValue = MutableLiveData<String>()
    val errorKeyboard = MutableLiveData("")
    val errorTransaction = MutableLiveData("")
    val errorRemote = MutableLiveData("")
    val keyboardActive = MutableLiveData(false)
    val rateLoading = MutableLiveData(false)
    var changellyTx: String? = null
    var swapDirection = 0

    val toAccount = MediatorLiveData<WalletAccount<*>>().apply {
        addSource(fromAccount) {
            if (value?.coinType == it.coinType) {
                viewModelScope.launch { postValue(getToAccountForInit()) }
            }
        }
    }
    val swapEnableDelay = MutableLiveData(false)
    val swapEnabled = MediatorLiveData<Boolean>().apply {
        value = false
        fun update() {
            value = toAccount.value?.canSpend() ?: false
                    && rateLoading.value == false
                    && swapEnableDelay.value == false
        }
        addSource(toAccount) {
            update()
        }
        addSource(rateLoading) {
            update()
        }
        addSource(swapEnableDelay) {
            update()
        }
    }

    val error = MediatorLiveData<String>().apply {
        value = ""
        fun error() =
                when {
                    keyboardActive.value == true -> ""
                    errorKeyboard.value?.isNotEmpty() == true -> errorKeyboard.value
                    errorTransaction.value?.isNotEmpty() == true -> errorTransaction.value
                    errorRemote.value?.isNotEmpty() == true -> errorRemote.value
                    else -> ""
                }
        addSource(errorKeyboard) {
            value = error()
        }
        addSource(errorTransaction) {
            value = error()
        }
        addSource(keyboardActive) {
            value = error()
        }
        addSource(fromAccount) {
            errorKeyboard.value = ""
            errorTransaction.value = ""
            errorRemote.value = ""
        }
        addSource(toAccount) {
            errorKeyboard.value = ""
            errorTransaction.value = ""
            errorRemote.value = ""
        }
    }


    val fromCurrency = fromAccount.map {
        it.coinType
    }
    val fromAddress = fromAccount.map {
        it.receiveAddress.toString()
    }
    val fromChain = fromAccount.map {
        if (it.basedOnCoinType != it.coinType) it.basedOnCoinType.name else ""
    }
    val fromFiatBalance = fromAccount.map {
        mbwManager.exchangeRateManager
                .get(it.accountBalance.spendable, mbwManager.getFiatCurrency(it.coinType))
                ?.toStringFriendlyWithUnit()
    }
    val toCurrency = toAccount.map {
        it?.coinType ?: Utils.getBtcCoinType()
    }
    val toAddress = toAccount.map {
        it?.receiveAddress?.toString()
    }
    val toChain = toAccount.map {
        if (it?.basedOnCoinType != it?.coinType) it?.basedOnCoinType?.name else ""
    }
    val toBalance = toAccount.map {
        it?.accountBalance?.spendable?.toStringFriendlyWithUnit()
    }
    val toFiatBalance = toAccount.map {
        it?.accountBalance?.spendable?.let { value ->
            mbwManager.exchangeRateManager
                    .get(value, mbwManager.getFiatCurrency(it.coinType))
                    ?.toStringFriendlyWithUnit()
        }
    }
//    val exchangeRate = Transformations.map(exchangeInfo) {
//        "1 ${it.from.toUpperCase()} = ${it.result} ${it.to.toUpperCase()}"
//    }

    val exchangeRateFrom = exchangeInfo.map {
        "1 ${it.from.toUpperCase()} = "
    }

    val exchangeRateToValue = exchangeInfo.map {
        it.result.toPlainString()
    }

    val exchangeRateToCurrency = exchangeInfo.map {
        it.to.toUpperCase()
    }

    val fiatSellValue = sellValue.map {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager
                        .get(fromCurrency.value?.value(it), mbwManager.getFiatCurrency(fromCurrency.value))
                        ?.toStringFriendlyWithUnit()?.let { "≈$it" }
            } catch (e: NumberFormatException) {
                "N/A"
            }
        } else {
            ""
        }
    }
    val fiatBuyValue = buyValue.map {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager
                        .get(toCurrency.value?.value(it), mbwManager.getFiatCurrency(toCurrency.value))
                        ?.toStringFriendlyWithUnit()?.let { "≈$it" }
            } catch (e: NumberFormatException) {
                "N/A"
            }
        } else {
            ""
        }
    }

    val minerFee = MutableLiveData("")

    val validateData = MediatorLiveData<Boolean>().apply {
        value = isValid()
        addSource(toAddress) {
            value = isValid()
        }
        addSource(fromAddress) {
            value = isValid()
        }
        addSource(sellValue) {
            value = isValid()
        }
        addSource(exchangeInfo) {
            value = isValid()
        }
        addSource(rateLoading) {
            value = isValid()
        }
    }

    fun isValid(): Boolean =
            try {
                errorTransaction.value = ""
                minerFee.value = ""
                val res = getApplication<WalletApplication>().resources
                val amount = sellValue.value?.toBigDecimal()
                when {
                    toAddress.value == null -> false
                    fromAddress.value == null -> false
                    rateLoading.value == true -> false
                    amount == null -> false
                    amount == BigDecimal.ZERO -> false
                    exchangeInfo.value?.minFrom != null && amount < exchangeInfo.value?.minFrom  -> {
                        errorTransaction.value = res.getString(R.string.exchange_min_msg,
                                exchangeInfo.value?.minFrom?.stripTrailingZeros()?.toPlainString(),
                                exchangeInfo.value?.from?.toUpperCase())
                        false
                    }
                    exchangeInfo.value?.maxFrom != null && amount > exchangeInfo.value?.maxFrom -> {
                        errorTransaction.value = res.getString(R.string.exchange_max_msg,
                                exchangeInfo.value?.maxFrom?.stripTrailingZeros()?.toPlainString(),
                                exchangeInfo.value?.from?.toUpperCase())
                        false
                    }
                    else -> checkValidTransaction() != null
                }
            } catch (e: java.lang.NumberFormatException) {
                false
            }

    fun checkValidTransaction(): Transaction? {
        val res = getApplication<WalletApplication>().resources
        val account = fromAccount.value!!
        val value = account.coinType.value(sellValue.value!!)
        if (value.equalZero()) {
            return null
        }
        val feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        try {
            return prepateTx(account.dummyAddress, sellValue.value!!)
                    ?.apply {
                        minerFee.value =
                                res.getString(R.string.miner_fee) + " " +
                                        this.totalFee().toStringFriendlyWithUnit() + " " +
                                        mbwManager.exchangeRateManager
                                                .get(this.totalFee(), mbwManager.getFiatCurrency(this.type))
                                                ?.toStringFriendlyWithUnit()?.let { "≈$it" }
                    }
        } catch (e: OutputTooSmallException) {
            errorTransaction.value = res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit())
        } catch (e: InsufficientFundsForFeeException) {
            if (account is ERC20Account) {
                val fee = feeEstimation.normal.times(account.typicalEstimatedTransactionSize.toBigInteger())
                errorTransaction.value = res.getString(R.string.please_top_up_your_eth_account,
                        account.ethAcc.label, fee.toStringFriendlyWithUnit(), convert(fee)) + TAG_ETH_TOP_UP
            } else {
                errorTransaction.value = res.getString(R.string.insufficient_funds_for_fee)
            }
        } catch (e: InsufficientFundsException) {
            errorTransaction.value = res.getString(R.string.insufficient_funds)
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            errorTransaction.value = res.getString(R.string.tx_build_error) + " " + e.message
        } catch (e: Exception) {
            errorTransaction.value = res.getString(R.string.tx_build_error) + " " + e.message
        }
        return null
    }

    fun prepateTx(address: Address, amount: String): Transaction? {
        val res = getApplication<WalletApplication>().resources
        val account = fromAccount.value!!
        val value = account.coinType.value(amount)
        if (value.equalZero()) {
            return null
        }
        val feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        try {
            return account.createTx(
                    address,
                    value,
                    FeePerKbFee(feeEstimation.normal),
                    null
            )
        } catch (e: OutputTooSmallException) {
            errorTransaction.postValue(res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit()))
        } catch (e: InsufficientFundsForFeeException) {
            if (account is ERC20Account) {
                val parentAccountBalance = account.ethAcc.accountBalance.spendable
                val topUpForFee = feeEstimation.normal.times(account.typicalEstimatedTransactionSize.toBigInteger()) - parentAccountBalance
                errorTransaction.postValue(res.getString(R.string.please_top_up_your_eth_account,
                        account.ethAcc.label, topUpForFee.toStringFriendlyWithUnit(), convert(topUpForFee)) + TAG_ETH_TOP_UP)
            } else {
                errorTransaction.postValue(res.getString(R.string.insufficient_funds_for_fee))
            }
        } catch (e: InsufficientFundsException) {
            errorTransaction.postValue(res.getString(R.string.insufficient_funds))
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            errorTransaction.postValue(res.getString(R.string.tx_build_error) + " " + e.message)
        } catch (e: Exception) {
            errorTransaction.postValue(res.getString(R.string.tx_build_error) + " " + e.message)
        }
        return null
    }

    fun convert(value: Value) =
            " ~${mbwManager.exchangeRateManager.get(value, mbwManager.getFiatCurrency(value.type))?.toStringFriendlyWithUnit() ?: ""}"


    fun getToAccountForInit() = Utils.sortAccounts(mbwManager.getWalletManager(false)
            .getAllActiveAccounts(), mbwManager.metadataStorage)
            .firstOrNull {
                it.coinType != fromAccount.value?.coinType
                        && isSupported(it.coinType)
            }

    fun isSupported(coinType: CryptoCurrency) =
            currencies.contains(Util.trimTestnetSymbolDecoration(coinType.symbol).toLowerCase())

    companion object {
        const val TAG_ETH_TOP_UP = "<hiden type=\"TAG_ETH_TOP_UP\"/>"
    }

    fun reset() {
        sellValue.value = ""
        buyValue.value = ""
        errorTransaction.value = ""
        errorRemote.value = ""
        errorKeyboard.value = ""
        fromAccount.value = fromAccount.value
        toAccount.value = toAccount.value
    }
}
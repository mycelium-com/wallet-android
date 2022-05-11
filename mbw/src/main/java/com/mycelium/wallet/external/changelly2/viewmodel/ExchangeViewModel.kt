package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.mrd.bitlib.TransactionUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.external.changelly.model.FixRate
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException


class ExchangeViewModel : ViewModel() {
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    var currencies = setOf("BTC", "ETH")
    val fromAccount = MutableLiveData<WalletAccount<*>>()
    val toAccount = MutableLiveData<WalletAccount<*>>()
    val exchangeInfo = MutableLiveData<FixRate>()
    val sellValue = MutableLiveData<String>()
    val buyValue = MutableLiveData<String>()
    val error = MutableLiveData("")

    val feeEstimation = Transformations.map(fromAccount) {
        mbwManager.getFeeProvider(it.basedOnCoinType).estimation
    }
    val fromCurrency = Transformations.map(fromAccount) {
        it.coinType
    }
    val fromAddress = Transformations.map(fromAccount) {
        it.receiveAddress.toString()
    }
    val fromChain = Transformations.map(fromAccount) {
        if (it.basedOnCoinType != it.coinType) it.basedOnCoinType.name else ""
    }
    val fromFiatBalance = Transformations.map(fromAccount) {
        mbwManager.exchangeRateManager.get(it.accountBalance.spendable,
                mbwManager.getFiatCurrency(it.coinType))?.toStringFriendlyWithUnit()
    }
    val toCurrency = Transformations.map(toAccount) {
        it.coinType
    }
    val toAddress = Transformations.map(toAccount) {
        it.receiveAddress.toString()
    }
    val toChain = Transformations.map(toAccount) {
        if (it.basedOnCoinType != it.coinType) it.basedOnCoinType.name else ""
    }
    val toFiatBalance = Transformations.map(toAccount) {
        mbwManager.exchangeRateManager.get(it.accountBalance.spendable,
                mbwManager.getFiatCurrency(it.coinType))?.toStringFriendlyWithUnit()
    }
    val exchangeRate = Transformations.map(exchangeInfo) {
        "1 ${it.from.toUpperCase()} = ${it.result} ${it.to.toUpperCase()}"
    }
    val fiatSellValue = Transformations.map(sellValue) {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager.get(fromCurrency.value?.value(it), mbwManager.getFiatCurrency(fromCurrency.value)).toStringFriendlyWithUnit()
            } catch (e: NumberFormatException) {
                "N/A"
            }
        } else {
            ""
        }
    }
    val fiatBuyValue = Transformations.map(buyValue) {
        if (it?.isNotEmpty() == true) {
            try {
                mbwManager.exchangeRateManager.get(toCurrency.value?.value(it), mbwManager.getFiatCurrency(toCurrency.value)).toStringFriendlyWithUnit()
            } catch (e: NumberFormatException) {
                "N/A"
            }
        } else {
            ""
        }
    }

    val validateData = MediatorLiveData<Boolean>().apply {
        value = isValid()
        addSource(sellValue) {
            value = isValid()
        }
        addSource(exchangeInfo) {
            value = isValid()
        }
    }

    fun isValid(): Boolean =
            try {
                val amount = sellValue.value?.toBigDecimal()
                when {
                    amount == null -> false
                    amount < exchangeInfo.value?.minFrom -> false
                    amount > exchangeInfo.value?.maxFrom -> false
                    else -> checkValidTransaction() != null
                }
            } catch (e: java.lang.NumberFormatException) {
                false
            }

    fun checkValidTransaction(): Transaction? {
        val res = WalletApplication.getInstance().resources
        val account = fromAccount.value!!
        val value = account.coinType.value(sellValue.value!!)
        if (value.equalZero()) {
            return null
        }
        try {
            return account.createTx(
                    account.dummyAddress,
                    value,
                    FeePerKbFee(feeEstimation.value!!.normal),
                    null
            )
        } catch (e: OutputTooSmallException) {
            error.value = res.getString(R.string.amount_too_small_short,
                    Value.valueOf(account.coinType, TransactionUtils.MINIMUM_OUTPUT_VALUE).toStringWithUnit())
        } catch (e: InsufficientFundsException) {
            error.value = res.getString(R.string.insufficient_funds)
        } catch (e: BuildTransactionException) {
            mbwManager.reportIgnoredException("MinerFeeException", e)
            error.value = res.getString(R.string.tx_build_error)
        } catch (e: Exception) {
            error.value = res.getString(R.string.tx_build_error)
        }
        return null
    }
}
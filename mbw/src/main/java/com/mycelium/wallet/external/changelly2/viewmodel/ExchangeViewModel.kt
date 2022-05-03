package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.external.changelly.model.FixRate
import com.mycelium.wapi.wallet.WalletAccount


class ExchangeViewModel : ViewModel() {
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())

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
    val fromFiatBalance = Transformations.map(fromAccount) {
        mbwManager.exchangeRateManager.get(it.accountBalance.spendable,
                mbwManager.getFiatCurrency(fromCurrency.value)).toStringFriendlyWithUnit()
    }
    val toCurrency = Transformations.map(toAccount) {
        it.coinType
    }
    val toAddress = Transformations.map(toAccount) {
        it.receiveAddress.toString()
    }
    val toFiatBalance = Transformations.map(toAccount) {
        mbwManager.exchangeRateManager.get(it.accountBalance.spendable,
                mbwManager.getFiatCurrency(fromCurrency.value)).toStringFriendlyWithUnit()
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


}
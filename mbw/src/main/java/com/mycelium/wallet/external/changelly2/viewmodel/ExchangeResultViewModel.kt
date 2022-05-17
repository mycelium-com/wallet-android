package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import java.math.BigDecimal
import java.text.DateFormat
import java.util.*


class ExchangeResultViewModel : ViewModel() {
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val spendValue = MutableLiveData<String>()
    val spendValueFiat = MutableLiveData<String>()
    val getValue = MutableLiveData<String>()
    val getValueFiat = MutableLiveData<String>()
    val txId = MutableLiveData<String>()
    val date = MutableLiveData<String>()
    val fromAddress = MutableLiveData<String>()
    val toAddress = MutableLiveData<String>()

    fun setTransaction(result: ChangellyTransaction) {
        txId.value = result.id
        spendValue.value = "${result.amountExpectedFrom} ${result.currencyFrom}"
        getValue.value = "${result.amountExpectedTo} ${result.currencyTo}"
        date.value = DateFormat.getDateInstance(DateFormat.LONG).format(Date(result.createdAt * 1000L))
        toAddress.value = result.payoutAddress
        spendValueFiat.value = getFiatValue(result.amountExpectedFrom, result.currencyFrom)
        getValueFiat.value = getFiatValue(result.amountExpectedTo, result.currencyTo)
    }

    private fun getFiatValue(amount: BigDecimal?, currency: String) =
            mbwManager.getWalletManager(false).getAssetTypes()
                    .firstOrNull { it.symbol.equals(currency, true) }
                    ?.let {
                        amount?.let { amount ->
                            mbwManager.exchangeRateManager
                                    .get(it.value(amount.toPlainString()), mbwManager.getFiatCurrency(it))
                                    ?.toStringFriendlyWithUnit()
                        }
                    } ?: ""

}
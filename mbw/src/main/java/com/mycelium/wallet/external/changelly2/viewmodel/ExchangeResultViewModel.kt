package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import java.text.DateFormat
import java.util.*


class ExchangeResultViewModel : ViewModel() {

    val spendValue = MutableLiveData<String>()
    val getValue = MutableLiveData<String>()
    val txId = MutableLiveData<String>()
    val date = MutableLiveData<String>()
    val toAddress = MutableLiveData<String>()

    fun setTransaction(result: ChangellyTransaction) {
        txId.value = result.id
        spendValue.value = "${result.amountExpectedFrom} ${result.currencyFrom}"
        getValue.value = "${result.amountExpectedTo} ${result.currencyTo}"
        date.value = DateFormat.getDateInstance(DateFormat.LONG).format(Date(result.createdAt * 1000L))
        toAddress.value = result.payoutAddress
    }

}
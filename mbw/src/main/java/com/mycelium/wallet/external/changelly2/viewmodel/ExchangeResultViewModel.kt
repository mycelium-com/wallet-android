package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import java.util.*


class ExchangeResultViewModel : ViewModel() {

    val spendValue = MutableLiveData<String>()
    val getValue = MutableLiveData<String>()
    val txId = MutableLiveData<String>()
    val date = MutableLiveData<String>()

    fun setTransaction(result: ChangellyTransaction) {
        txId.value = result.id
        spendValue.value = "${result.moneySent} ${result.currencyFrom}"
        getValue.value = "${result.moneyReceived} ${result.currencyTo}"
        date.value = Date(result.createdAt).toString()
    }

}
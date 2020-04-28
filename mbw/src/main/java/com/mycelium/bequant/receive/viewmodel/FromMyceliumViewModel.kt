package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class FromMyceliumViewModel : ViewModel() {
    val oneCoinFiatRate = MutableLiveData<String>()
    val castodialLabel = MutableLiveData<String>()
    val castodialBalance = MutableLiveData<String>()
    val coin = MutableLiveData<String>()
    val amount = MutableLiveData<String>()
    val amountFiat = MutableLiveData<String>()
    val address = MutableLiveData<String>()

    fun hasOneCoinFiatRate() = oneCoinFiatRate.value != null && oneCoinFiatRate.value?.isNotEmpty() == true
}
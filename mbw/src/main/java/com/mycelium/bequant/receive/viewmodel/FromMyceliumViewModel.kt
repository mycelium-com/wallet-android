package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class FromMyceliumViewModel : ViewModel() {
    val oneCoinFiatRate = MutableLiveData<String>()

    fun hasOneCoinFiatRate() = oneCoinFiatRate.value != null && oneCoinFiatRate.value?.isNotEmpty() == true
}
package com.mycelium.bequant.market.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class AccountViewModel : ViewModel() {
    val searchMode = MutableLiveData(false)
    val privateMode = MutableLiveData(false)
    val totalBalance = MutableLiveData<String>()
    val totalBalanceFiat = MutableLiveData<String>()
}
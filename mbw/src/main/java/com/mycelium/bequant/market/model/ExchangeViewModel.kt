package com.mycelium.bequant.market.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.coins.Value


class ExchangeViewModel : ViewModel() {
    val available = MutableLiveData<Value>()
    val youSend = MutableLiveData<Value>()
    val youGet = MutableLiveData<Value>()
}
package com.mycelium.bequant.market.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.BequantPreference.getMockCastodialBalance
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.coins.Value


class ExchangeViewModel : ViewModel() {
    val available = MutableLiveData<Value>(getMockCastodialBalance())
    val youSend = MutableLiveData(Value.valueOf(Utils.getBtcCoinType(), 0))
    val youGet = MutableLiveData(Value.valueOf(Utils.getEthCoinType(), 0))
    val fullSourceUnitDestinationPrice = MutableLiveData(Value.zeroValue(Utils.getEthCoinType()))

    val youSendText = MutableLiveData<String>()
    val youGetText = MutableLiveData<String>()
}
package com.mycelium.bequant.market.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.BequantPreference.getMockCastodialBalance
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.coins.Value


class ExchangeViewModel : ViewModel() {
    val available = MutableLiveData<Value>(getMockCastodialBalance())
    val youSend = MutableLiveData<Value>(Value.valueOf(Utils.getBtcCoinType(), 1212312312))
    val youGet = MutableLiveData<Value>(Value.valueOf(Utils.getEthCoinType(), 4545454545))
    val fullSourceUnitDestinationPrice = MutableLiveData<Value>(Value.zeroValue(Utils.getEthCoinType()))
}
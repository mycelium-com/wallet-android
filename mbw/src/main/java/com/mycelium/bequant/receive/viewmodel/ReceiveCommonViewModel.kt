package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils
import java.io.Serializable


class ReceiveCommonViewModel : ViewModel(), Serializable {
    val address = MutableLiveData<String>()
    val tag = MutableLiveData<String>()
    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
}
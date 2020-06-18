package com.mycelium.bequant.withdraw.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils


class WithdrawViewModel : ViewModel() {
    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
    val castodialBalance = MutableLiveData<String>()
    val amount = MutableLiveData<String>()
    val address = MutableLiveData<String>()
}
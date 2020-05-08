package com.mycelium.bequant.withdraw.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils


class WithdrawCommonViewModel : ViewModel() {
    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
}
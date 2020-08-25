package com.mycelium.bequant.withdraw.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class WithdrawAddressViewModel : ViewModel() {
    val address = MutableLiveData<String>()
}
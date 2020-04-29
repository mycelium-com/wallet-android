package com.mycelium.bequant.kyc.step2

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class Step2ViewModel : ViewModel() {
    val addressLine1 = MutableLiveData<String>()
    val addressLine2 = MutableLiveData<String>()
    val city = MutableLiveData<String>()
    val postcode = MutableLiveData<String>()
    val country = MutableLiveData<String>()
}
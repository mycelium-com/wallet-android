package com.mycelium.bequant.kyc.steps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.KYCRequest


class Step2ViewModel : ViewModel() {
    val addressLine1 = MutableLiveData<String>()
    val addressLine2 = MutableLiveData<String>()
    val city = MutableLiveData<String>()
    val postcode = MutableLiveData<String>()
    val country = MutableLiveData<String>()
    val countryAcronym = MutableLiveData<String>()
    val nextButton = MutableLiveData<Boolean>()

    fun isValid(): Boolean = listOf(addressLine1, city, postcode, country).all {
        it.value?.trim()?.isNotEmpty() == true
    }
}
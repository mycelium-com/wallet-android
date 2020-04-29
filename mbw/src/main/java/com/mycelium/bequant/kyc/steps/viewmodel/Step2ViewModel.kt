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


    fun fromModel(kyc: KYCRequest) {
        addressLine1.value = kyc.address_1
        addressLine2.value = kyc.address_2
        city.value = kyc.city
        postcode.value = kyc.zip
        country.value = kyc.country
    }

    fun fillModel(kyc: KYCRequest) {
        kyc.address_1 = addressLine1.value
        kyc.address_2 = addressLine2.value
        kyc.city = city.value
        kyc.zip = postcode.value
        kyc.country = country.value
    }
}
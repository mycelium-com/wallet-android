package com.mycelium.bequant.kyc.inputPhone

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.KYCRequest


class InputPhoneViewModel : ViewModel() {
    val phoneNumber = MutableLiveData<String>()
    val phoneCode = MutableLiveData<String>()
    val countryName = MutableLiveData<String>()

    fun fillModel(kyc: KYCRequest) {
        kyc.first_name = phoneNumber.value
        kyc.last_name = countryName.value
        kyc.birthday = phoneCode.value
    }
}
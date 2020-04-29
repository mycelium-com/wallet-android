package com.mycelium.bequant.kyc.steps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.KYCRequest


class Step1ViewModel : ViewModel() {
    val firstName = MutableLiveData<String>()
    val lastName = MutableLiveData<String>()
    val birthday = MutableLiveData<String>()
    val nationality = MutableLiveData<String>()


    fun fromModel(kyc: KYCRequest) {
        firstName.value = kyc.first_name
        lastName.value = kyc.last_name
        birthday.value = kyc.birthday
        nationality.value = kyc.nationality
    }

    fun fillModel(kyc: KYCRequest) {
        kyc.first_name = firstName.value
        kyc.last_name = lastName.value
        kyc.birthday = birthday.value
        kyc.nationality = nationality.value
    }
}
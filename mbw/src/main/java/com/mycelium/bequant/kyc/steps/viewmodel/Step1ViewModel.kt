package com.mycelium.bequant.kyc.steps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import java.util.*


class Step1ViewModel : ViewModel() {
    val firstName = MutableLiveData<String>()
    val lastName = MutableLiveData<String>()
    val birthday = MutableLiveData<Date>()
    val nationality = MutableLiveData<String>()
    val nationalityAcronum = MutableLiveData<String>()
    val nextButton = MutableLiveData<Boolean>()

    fun fromModel(kyc: KYCRequest) {
        firstName.value = kyc.first_name
        lastName.value = kyc.last_name
        birthday.value = kyc.birthday
        nationalityAcronum.value = kyc.nationality
    }

    fun fillModel(kyc: KYCRequest) {
        kyc.first_name = firstName.value
        kyc.last_name = lastName.value
        kyc.birthday = birthday.value
        kyc.nationality = nationalityAcronum.value
    }

    fun isValid(): Boolean {
        if (firstName.value?.trim()?.isNotEmpty() != true) {
            return false
        }
        if (lastName.value?.trim()?.isNotEmpty() != true) {
            return false
        }
        if (birthday.value == null) {
            return false
        }
        if (nationality.value == null) {
            return false
        }
        return true
    }

}
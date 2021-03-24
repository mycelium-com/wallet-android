package com.mycelium.bequant.kyc.steps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.KYCRequest
import java.util.*


class Step1ViewModel : ViewModel() {
    val email = MutableLiveData<String>()

   val nextButton = MutableLiveData<Boolean>()

    fun fromModel(kyc: KYCRequest) {
        email.value = kyc.email ?: ""
    }

    fun fillModel(kyc: KYCRequest) {
        kyc.email = email.value
    }

    fun isValid(): Boolean {
        if (email.value?.trim()?.isNotEmpty() != true) {
            return false
        }
        return true
    }

}
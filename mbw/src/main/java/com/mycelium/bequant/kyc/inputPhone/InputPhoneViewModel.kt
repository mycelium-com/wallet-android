package com.mycelium.bequant.kyc.inputPhone

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.client.models.KycSaveMobilePhoneRequest


class InputPhoneViewModel : ViewModel() {
    fun getRequest(): KycSaveMobilePhoneRequest? {
        if (!isValidMobile(phoneNumber.value)){
            return null
        }
        if (TextUtils.isEmpty(countryCode.value)){
            return null
        }
        return KycSaveMobilePhoneRequest(phoneNumber.value!!,countryCode.value!!)
    }

    val phoneNumber = MutableLiveData<String>()
    val phoneCode = MutableLiveData<String>("+7")
    val countryCode = MutableLiveData<String>("+7")

    private fun isValidMobile(phone: String?): Boolean {
        return Patterns.PHONE.matcher(phone).matches()
    }
}
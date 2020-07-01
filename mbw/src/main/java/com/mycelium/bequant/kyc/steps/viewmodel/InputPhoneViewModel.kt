package com.mycelium.bequant.kyc.steps.viewmodel

import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.client.models.KycSaveMobilePhoneRequest


class InputPhoneViewModel : ViewModel() {
    fun getRequest(): KycSaveMobilePhoneRequest? {
        if (!isValidMobile(phoneNumber.value)) {
            return null
        }
        if (countryModel == null) {
            return null
        }
        return KycSaveMobilePhoneRequest(phoneNumber.value!!, countryModel.value!!.code.toString())
    }

    val phoneNumber = MutableLiveData<String>()
    val countryModel = MutableLiveData<CountryModel>(CountryModel("Russia","RU",7))

    private fun isValidMobile(phone: String?): Boolean {
        if (phone == null) {
            return false
        }
        return Patterns.PHONE.matcher(phone).matches()
    }
}
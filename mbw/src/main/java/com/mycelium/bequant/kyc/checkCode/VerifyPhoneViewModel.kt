package com.mycelium.bequant.kyc.checkCode

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.client.models.KycCheckMobilePhoneRequest


class VerifyPhoneViewModel : ViewModel() {
    val code = MutableLiveData<String>()

    fun fillModel(): KycCheckMobilePhoneRequest? {
        val toInt = code.value?.toInt()
        toInt?.let {
            return@fillModel KycCheckMobilePhoneRequest(it)
        }
        return null
    }
}
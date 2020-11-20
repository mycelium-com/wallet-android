package com.mycelium.bequant.kyc.steps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class DocumentViewModel : ViewModel() {
    val identityCount = MutableLiveData<Int>()
    val poaCount = MutableLiveData<Int>()
    val selfieCount = MutableLiveData<Int>()
    val nextButton = MutableLiveData<Boolean>()

    fun isValid() =
            identityCount.value ?: 0 > 0 &&
                    poaCount.value ?: 0 > 0 &&
                    selfieCount.value ?: 0 > 0
}
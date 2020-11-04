package com.mycelium.bequant.kyc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel

class BequantKycViewModel : ViewModel() {
    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    val country = MutableLiveData<CountryModel>()
    fun updateActionBarTitle(title: String) = _title.postValue(title)
}
package com.mycelium.bequant.signup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SetupCodeViewModel : ViewModel() {
    val name = MutableLiveData<String>()
    val secretCode = MutableLiveData<String>()
}
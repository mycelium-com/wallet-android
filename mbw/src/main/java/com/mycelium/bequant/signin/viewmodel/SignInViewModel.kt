package com.mycelium.bequant.signin.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SignInViewModel : ViewModel() {
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
}
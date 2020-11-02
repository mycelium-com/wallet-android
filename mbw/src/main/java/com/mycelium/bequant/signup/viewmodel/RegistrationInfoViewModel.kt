package com.mycelium.bequant.signup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.client.models.RegisterAccountRequest

class RegistrationInfoViewModel : ViewModel() {
    val email = MutableLiveData<String>()

    fun setRegister(register: RegisterAccountRequest) {
        email.value = register.email
    }
}
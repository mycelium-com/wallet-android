package com.mycelium.bequant.signup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.remote.model.Register


class RegistrationInfoViewModel : ViewModel() {
    val email = MutableLiveData<String>()

    fun setRegister(register: Register) {
        email.value = register.email
    }
}
package com.mycelium.bequant.kyc.steps.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class HeaderViewModel : ViewModel() {
    val visibility = MutableLiveData<Int>(View.GONE)

    fun hide() {
        visibility.value = View.GONE
    }
}
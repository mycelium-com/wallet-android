package com.mycelium.wallet.activity.fio.domain.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*


class FIODomainViewModel : ViewModel() {
    val fioDomain = MutableLiveData<String>("")
    val expireDate = MutableLiveData<Date>()
}
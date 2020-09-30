package com.mycelium.wallet.activity.fio.domain.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*


class FIODomainViewModel : ViewModel() {
    val fioDomain = MutableLiveData<String>("")
    val fioDomainExpireDate = MutableLiveData<Date>()

    val DATE_FORMAT = SimpleDateFormat("MMMM dd, yyyy\nK:mm a")

    fun dateToString(date: Date) = DATE_FORMAT.format(date)

}
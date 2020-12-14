package com.mycelium.wallet.activity.fio.mapaccount.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FioAccount
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class FIODomainViewModel : ViewModel() {
    val fioDomain = MutableLiveData<FIODomain>()
    val fioAccount = MutableLiveData<FioAccount>()

    val DATE_FORMAT = SimpleDateFormat("MMMM dd, yyyy\nK:mm a")

    fun dateToString(date: Date) = DATE_FORMAT.format(date)

    fun isExpired(date: Date): Boolean = TimeUnit.MILLISECONDS.toDays(date.time - Date().time) < 30

}
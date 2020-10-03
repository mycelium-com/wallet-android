package com.mycelium.wallet.activity.fio.mapaccount.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import java.text.SimpleDateFormat
import java.util.*


class AccountMappingViewModel : ViewModel() {
    val fioAccount = MutableLiveData<FioAccount>()
    val fioName = MutableLiveData<RegisteredFIOName>()
    val acknowledge = MutableLiveData<Boolean>(false)

    val DATE_FORMAT = SimpleDateFormat("MMMM dd, yyyy\nK:mm a")

    fun dateToString(date: Date) = DATE_FORMAT.format(date)
}
package com.mycelium.wallet.activity.fio.mapaccount.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.activity.fio.mapaccount.Mode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import java.text.SimpleDateFormat
import java.util.*

class FIOMapPubAddressViewModel : ViewModel() {
    val accountList = MutableLiveData<List<FioAccount>>()
    val fioAddress = MutableLiveData<String>("")
    val fioNameExpireDate = MutableLiveData<Date>(Date())
    val mode = MutableLiveData<Mode>(Mode.LIST)
    val extraAccount = MutableLiveData<WalletAccount<*>>()

    val DATE_FORMAT = SimpleDateFormat("MMMM dd, yyyy\nK:mm a")

    fun dateToString(date: Date) = DATE_FORMAT.format(date)
}
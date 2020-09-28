package com.mycelium.wallet.activity.fio.mapaccount.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.fio.FioAccount

class FIOMapPubAddressViewModel : ViewModel() {
    val account = MutableLiveData<FioAccount>()
    val fioAddress = MutableLiveData<String>("")
    val acknowledge = MutableLiveData<Boolean>(false)
}
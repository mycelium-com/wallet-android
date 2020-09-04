package com.mycelium.wallet.activity.fio.mapaddress

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.fio.FioAccount

class FIORegisterAddressViewModel : ViewModel() {
    val account = MutableLiveData<FioAccount>()
    val registrationFee = MutableLiveData<String>()
    val remainingBalance = MutableLiveData<String>()
    val address = MutableLiveData<String>("")
    val domain = MutableLiveData<String>("fiotestnet")
    val addressWithDomain = MutableLiveData<String>("")
    val expirationDate = MutableLiveData<String>("")
}
package com.mycelium.wallet.activity.fio.mapaddress

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount

class FIORegisterAddressViewModel : ViewModel() {
    val account = MutableLiveData<FioAccount>()
    val registrationFee = MutableLiveData<Value>()
    val remainingBalance = MutableLiveData<Value>()
    val address = MutableLiveData<String>("")
    val domain = MutableLiveData<String>("fiotestnet")
    val addressWithDomain = MutableLiveData<String>("")
    val expirationDate = MutableLiveData<String>("")
    val isFioAddressAvailable = MutableLiveData<Boolean>(true)
    val isFioAddressValid = MutableLiveData<Boolean>(true)
    val isFioServiceAvailable = MutableLiveData<Boolean>(true)
}
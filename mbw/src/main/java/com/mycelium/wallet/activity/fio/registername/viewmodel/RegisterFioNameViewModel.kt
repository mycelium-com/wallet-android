package com.mycelium.wallet.activity.fio.registername.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FioAccount
import java.util.*

class RegisterFioNameViewModel : ViewModel() {
    val fioAccountToRegisterName = MutableLiveData<FioAccount>()
    val accountToPayFeeFrom = MutableLiveData<WalletAccount<*>>()
    val registrationFee = MutableLiveData<Value>()
    val address = MutableLiveData<String>("")
    val domain = MutableLiveData<FIODomain>(DEFAULT_DOMAIN1)
    val addressWithDomain = MutableLiveData<String>()
    val isFioAddressAvailable = MutableLiveData<Boolean>(true)
    val isFioAddressValid = MutableLiveData<Boolean>(true)
    val isFioServiceAvailable = MutableLiveData<Boolean>(true)
    val isRenew = MutableLiveData(false)

    companion object {
        val DEFAULT_DOMAIN1 = FIODomain("mycelium", Date(), true)
        val DEFAULT_DOMAIN2 = FIODomain("fiotestnet", Date(), true)
    }
}
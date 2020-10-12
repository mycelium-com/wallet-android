package com.mycelium.wallet.activity.fio.requests.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioSendRequestViewModel : ViewModel() {
    val request = MutableLiveData<FIORequestContent>()
    val payeeName = MutableLiveData<String>("newfriend@hisdomain")
    val memoFrom = MutableLiveData<String>("Please give me money to party - and come on!!!!")
    val alternativeAmountFormatted = MutableLiveData<String>("55.02 USD")
    val payerName = MutableLiveData<String>("myfiowallet@mycelium")
    val payerNameOwnerAccount = MutableLiveData<WalletAccount<*>>()
    val payerAccount = MutableLiveData<WalletAccount<*>>()
    val memoTo = MutableLiveData<String>("")
    val amount = MutableLiveData<Value>(Value.valueOf(Utils.getBtcCoinType(), 12000))
    val payeeTokenPublicAddress = MutableLiveData<String>("")
    val payerTokenPublicAddress = MutableLiveData<String>("")

    fun pay() {

    }

    fun decline() {

    }
}
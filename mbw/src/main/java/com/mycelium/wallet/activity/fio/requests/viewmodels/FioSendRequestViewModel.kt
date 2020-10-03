package com.mycelium.wallet.activity.fio.requests.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value

open class FioSendRequestViewModel : ViewModel() {
    val from = MutableLiveData<String>("newfriend@hisdomain")
    val memoFrom = MutableLiveData<String>("Please give me money to party - and come on!!!!")
    val amount = MutableLiveData<Value>(Value.valueOf(Utils.getBtcCoinType(), 12000))
    val alternativeAmountFormatted = MutableLiveData<String>("55.02 USD")
    val satisfyRequestFrom = MutableLiveData<String>("myfiowallet@mycelium")
    val satisfyRequestFromAccount = MutableLiveData<WalletAccount<*>>()
    val memoTo = MutableLiveData<String>("")
    val selectedFee = MutableLiveData<Value>()
    val tx = MutableLiveData<Transaction>()

    fun pay() {

    }

    fun decline() {

    }
}
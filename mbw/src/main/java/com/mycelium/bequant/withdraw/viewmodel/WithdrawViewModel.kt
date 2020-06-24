package com.mycelium.bequant.withdraw.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.bequant.remote.trading.model.InlineResponse200
import com.mycelium.wallet.Utils


class WithdrawViewModel : ViewModel() {

    val accountApi = AccountApiRepository()

    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
    val castodialBalance = MutableLiveData<String>()
    val amount = MutableLiveData<String>()
    val address = MutableLiveData<String>()

    val paymentId = MutableLiveData<String>()
    val includeFee = MutableLiveData<Boolean>()
    val autoCommit = MutableLiveData<Boolean>()
    val useOffChain = MutableLiveData<String>()

    fun withdraw(success: (InlineResponse200?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        accountApi.accountCryptoWithdrawPost(viewModelScope,
                currency.value!!,
                amount.value!!,
                address.value!!,
                paymentId.value!!,
                includeFee.value!!,
                autoCommit.value!!,
                useOffChain.value!!, success = success, error = error, finally = finally
        )
    }
}
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

    fun loadBalance(success: (Array<Balance>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        accountApi.accountBalanceGet(viewModelScope, success = success, error = error, finally = finally)
    }

    fun withdraw(success: (InlineResponse200?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        accountApi.accountCryptoWithdrawPost(
                scope = viewModelScope,
                currency = currency.value!!,
                amount = amount.value!!,
                address = address.value!!, success = success, error = error, finally = finally
        )
    }
}
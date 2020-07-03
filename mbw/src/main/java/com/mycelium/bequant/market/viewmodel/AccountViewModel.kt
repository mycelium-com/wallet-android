package com.mycelium.bequant.market.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Balance
import com.mycelium.view.Denomination
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigInteger


class AccountViewModel : ViewModel() {
    val searchMode = MutableLiveData(false)
    val privateMode = MutableLiveData(false)
    val totalBalance = MutableLiveData<String>()
    val totalBalanceFiat = MutableLiveData<String>()

    val tradingBalances = MutableLiveData<Array<Balance>>()
    val accountBalances = MutableLiveData<Array<Balance>>()

    fun loadBalance(success: (Array<Balance>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        Api.accountRepository.accountBalanceGet(viewModelScope, success = success, error = error, finally = finally)
    }
}
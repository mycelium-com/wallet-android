package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.remote.trading.model.Address
import com.mycelium.bequant.remote.repositories.Api


class ShowQRViewModel : ViewModel() {

    fun createDepositAddress(currency:String, success: (Address?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        Api.accountRepository.cryptoAddressCurrencyPost(viewModelScope, currency, success = success, error = error, finally = finally)
    }

    val addressLabel = MutableLiveData<String>()
    val tagLabel = MutableLiveData<String>()
}
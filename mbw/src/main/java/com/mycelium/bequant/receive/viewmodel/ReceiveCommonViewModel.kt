package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.Utils
import java.io.Serializable


class ReceiveCommonViewModel : ViewModel(), Serializable {
    fun depositAddress(finally: () -> Unit) {
        currency.value?.let {
            Api.accountRepository.cryptoAddressCurrencyGet(viewModelScope, it, {
                address.value = it?.address
                tag.value = it?.paymentId
            }, { _, message ->
                this.error.value = message
            }, finally)
        }
    }

    val error = MutableLiveData<String>()
    val address = MutableLiveData<String>()
    val tag = MutableLiveData<String>()
    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
}
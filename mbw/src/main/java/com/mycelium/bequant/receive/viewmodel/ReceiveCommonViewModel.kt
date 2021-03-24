package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.Utils
import java.io.Serializable


class ReceiveCommonViewModel : ViewModel(), Serializable {
    fun fetchDepositAddress(finally: () -> Unit) {
        currency.value?.let { currencySymbol ->
            Api.accountRepository.cryptoAddressCurrencyGet(
                    viewModelScope,
                    currencySymbol,
                    {
                        address.value = it?.address
                        tag.value = it?.paymentId ?: ""
                    },
                    { _, message ->
                        error.value = message
                    },
                    finally)
        }
    }

    val error = MutableLiveData<String>()
    val address = MutableLiveData<String>()
    val tag = MutableLiveData<String>()
    var currency = MutableLiveData(Utils.getBtcCoinType().symbol)
}
package com.mycelium.wallet.external.changelly2.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.mycelium.wapi.wallet.WalletAccount


class ExchangeViewModel : ViewModel() {
    val fromAccount = MutableLiveData<WalletAccount<*>>()
    val toAccount = MutableLiveData<WalletAccount<*>>()

    val fromCurrency = Transformations.map(fromAccount) {
        it.coinType
    }
    val toCurrency = Transformations.map(toAccount) {
        it.coinType
    }
    val exchangeRate = MutableLiveData<String>()

}
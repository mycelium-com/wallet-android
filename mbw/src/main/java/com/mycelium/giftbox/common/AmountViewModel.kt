package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface AmountViewModel {
    val amount: MutableLiveData<String>
    val amountFiat: MutableLiveData<String>
    val minerFee: MutableLiveData<String>
    val date: MutableLiveData<String>
}
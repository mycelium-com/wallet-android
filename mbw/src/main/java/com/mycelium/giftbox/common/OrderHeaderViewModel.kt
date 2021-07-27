package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface OrderHeaderViewModel {
    val productName: MutableLiveData<String>
    val expire: MutableLiveData<String>
    val country: MutableLiveData<String>
    val cardValue: MutableLiveData<String>
    val quantity: MutableLiveData<Int>
}
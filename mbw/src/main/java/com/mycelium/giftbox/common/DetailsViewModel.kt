package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface DetailsViewModel {
    val description: MutableLiveData<String>
    val expiry: MutableLiveData<String>
}
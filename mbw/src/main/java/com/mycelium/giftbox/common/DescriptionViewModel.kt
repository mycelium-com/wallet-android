package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface DescriptionViewModel {
    val description: MutableLiveData<String>
    val more: MutableLiveData<Boolean>
}
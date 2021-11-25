package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface ListStateViewModel {
    val state: MutableLiveData<ListState>
}
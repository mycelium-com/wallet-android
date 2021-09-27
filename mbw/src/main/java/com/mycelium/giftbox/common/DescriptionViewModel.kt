package com.mycelium.giftbox.common

import androidx.lifecycle.MutableLiveData


interface DescriptionViewModel {
    val description: MutableLiveData<String>
    val more: MutableLiveData<Boolean>
    val moreVisible: MutableLiveData<Boolean>
    val termsLink: MutableLiveData<String?>
    val redeemInstruction: MutableLiveData<String?>
}
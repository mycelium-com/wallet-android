package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel


class GiftBoxViewModel : ViewModel() {
    val countries = MutableLiveData<List<CountryModel>>(emptyList())
}
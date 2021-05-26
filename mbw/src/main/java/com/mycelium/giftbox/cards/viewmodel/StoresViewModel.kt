package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.Product


class StoresViewModel : ViewModel() {
    val products = MutableLiveData<List<Product>>(emptyList())
}
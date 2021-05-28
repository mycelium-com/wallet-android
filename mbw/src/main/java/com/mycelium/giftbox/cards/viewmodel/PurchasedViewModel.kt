package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.Order


class PurchasedViewModel : ViewModel() {
    val orders = MutableLiveData<List<Order>>(emptyList())
}
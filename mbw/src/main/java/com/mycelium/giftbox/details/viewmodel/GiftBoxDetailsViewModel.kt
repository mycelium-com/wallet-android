package com.mycelium.giftbox.details.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.GetOrderResponse


class GiftBoxDetailsViewModel : ViewModel() {
    val cardAmount = MutableLiveData<String>()
    val expireDate = MutableLiveData<String>()

    fun setOrder(order: GetOrderResponse) {
        cardAmount.value = "${order.amount} ${order.currency_code}"
        expireDate.value = "Does not expire" //TODO find where we can get expire date
    }
}
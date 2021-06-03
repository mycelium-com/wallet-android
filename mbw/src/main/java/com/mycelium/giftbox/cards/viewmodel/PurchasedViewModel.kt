package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.OrdersHistoryResponse


class PurchasedViewModel : ViewModel() {
    val orders = MutableLiveData<List<Order>>(emptyList())
    var ordersSize = 0L

    fun setOrdersResponse(it: OrdersHistoryResponse?, append: Boolean = false) {
        if (append) {
            orders.value = (orders.value ?: emptyList()) +
                    (it?.items ?: emptyList())
        } else {
            orders.value = it?.items ?: emptyList()
        }
        ordersSize = it?.size ?: 0
    }
}
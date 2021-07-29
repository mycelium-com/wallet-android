package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.OrdersHistoryResponse


class PurchasedViewModel : ViewModel() {
    val loading = MutableLiveData<Boolean>(false)
    val orders = MutableLiveData<List<Order>>(emptyList())
    var ordersSize = 0L

    fun setOrdersResponse(it: OrdersHistoryResponse?, append: Boolean = false) {
        orders.value = if (append) {
            (orders.value ?: emptyList()) +
                    (it?.items ?: emptyList())
        } else {
            it?.items ?: emptyList()
        }.sortedByDescending { it.timestamp }
        ordersSize = it?.size ?: 0
    }
}
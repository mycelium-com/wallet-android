package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.OrdersHistoryResponse
import com.mycelium.giftbox.common.ListState
import com.mycelium.giftbox.common.ListStateViewModel


open class PurchasedViewModel : ViewModel(), ListStateViewModel {
    val orders = MutableLiveData<List<Order>>(emptyList())
    var ordersSize = MutableLiveData(0L)
    override val state = MutableLiveData<ListState>(ListState.OK)

    fun setOrdersResponse(it: OrdersHistoryResponse?, append: Boolean = false) {
        orders.value = if (append) {
            (orders.value ?: emptyList()) +
                    (it?.items ?: emptyList())
        } else {
            it?.items ?: emptyList()
        }.sortedByDescending { it.timestamp }
        state.value = if (orders.value?.isNotEmpty() == true) ListState.OK else ListState.NOT_FOUND
        ordersSize.value = it?.size ?: 0
    }
}
package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.common.ListState
import com.mycelium.giftbox.common.ListStateViewModel


open class PurchasedViewModel : ViewModel(), ListStateViewModel {
    val orders = MutableLiveData<List<MCOrderResponse>>(emptyList())
    var ordersSize = MutableLiveData(0)
    override val state = MutableLiveData<ListState>(ListState.OK)

    fun setOrdersResponse(list: List<MCOrderResponse>?, append: Boolean = false) {
        orders.value = (if (append) {
            (orders.value ?: emptyList()) +
                    (list ?: emptyList())
        } else {
            list ?: emptyList()
        }).sortedByDescending { it.createdDate }
        state.value = if (orders.value?.isNotEmpty() == true) ListState.OK else ListState.NOT_FOUND
        ordersSize.value = (list?.size ?: 0)
    }
}
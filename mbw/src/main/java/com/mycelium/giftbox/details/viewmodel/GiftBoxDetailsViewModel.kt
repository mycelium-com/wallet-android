package com.mycelium.giftbox.details.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.GetOrderResponse
import com.mycelium.giftbox.client.models.Item


class GiftBoxDetailsViewModel : ViewModel() {
    val cardAmount = MutableLiveData<String>()
    val expireDate = MutableLiveData<String>()

    fun setOrder(order: GetOrderResponse) {
        cardAmount.value = "${order.amount} ${order.currency_code}"
        expireDate.value = "Does not expire" //TODO find where we can get expire date
    }

    private val load = MutableLiveData<Params>()

    fun load(item: Item) {
        load.value = Params(item)
    }

    val loadSubsription = {
        load.switchMap {
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.order(
                        Constants.CLIENT_USER_ID,
                        it.item.client_order_id!!
                )
            }.asLiveData()
        }
    }

    data class Params(val item: Item)
}
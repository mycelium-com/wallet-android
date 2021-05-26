package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.Item


class PurchasedViewModel : ViewModel() {
    val orders = MutableLiveData<List<Item>>(emptyList())
    private val load = MutableLiveData<Params>()

    fun load(offset: Long = 0, limit: Long = 30) {
        load.value = Params(offset, limit)
    }

    val loadSubsription = {
        load.switchMap {
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.orders(Constants.CLIENT_USER_ID, it.offset, it.limit)
            }.asLiveData()
        }
    }

    data class Params(val offset: Long = 0, val limit: Long = 30)
}
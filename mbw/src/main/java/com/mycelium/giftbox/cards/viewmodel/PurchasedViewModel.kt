package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.GitboxAPI


class PurchasedViewModel : ViewModel() {

    private val load = MutableLiveData<Params>()
    fun load(params: Params) {
        load.value = params
    }

    val loadSubsription = {
        load.switchMap {
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.orders(it.clientUserId)
            }.asLiveData()
        }
    }

    data class Params(val clientUserId: String)
}
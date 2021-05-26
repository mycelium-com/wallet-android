package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.GitboxAPI


class StoresViewModel : ViewModel() {

    private val load = MutableLiveData<Params>()
    fun load(params: Params) {
        load.value = params
    }

    val loadSubsription = {
        load.switchMap {
            val (clientUserId, clientOrderId) = it
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.products(
                    clientUserId = clientUserId,
                    clientOrderId = clientOrderId,
                    offset = 0,
                    limit = 100
                )
            }.asLiveData()
        }
    }

    data class Params(val clientUserId: String, val clientOrderId: String)
}
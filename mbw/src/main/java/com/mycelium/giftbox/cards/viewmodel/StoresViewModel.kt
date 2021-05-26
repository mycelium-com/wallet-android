package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.Product


class StoresViewModel : ViewModel() {
    val products = MutableLiveData<List<Product>>(emptyList())
    private val load = MutableLiveData<Params>()

    fun load(search: String? = null,
             category: String? = null,
             countries: List<CountryModel> = emptyList(),
             offset: Long = 0,
             limit: Long = 30) {
        load.value = Params(search, category, countries, offset, limit)
    }

    val loadSubsription = {
        load.switchMap {
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.products(
                        it.search,
                        it.countries.joinToString(","),
                        it.category,
                        it.offset,
                        it.limit,
                        Constants.CLIENT_USER_ID,
                        Constants.CLIENT_ORDER_ID
                )
            }.asLiveData()
        }
    }

    data class Params(val search: String? = null,
                      val category: String? = null,
                      val countries: List<CountryModel> = emptyList(),
                      val offset: Long = 0,
                      val limit: Long = 30)
}
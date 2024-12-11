package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.ProductsResponse
import com.mycelium.giftbox.common.ListState
import com.mycelium.giftbox.common.ListStateViewModel


class StoresViewModel : ViewModel(), ListStateViewModel {
    var products = mutableListOf<MCProductInfo>()
    var productsSize = MutableLiveData<Long>(0L)
    override val state = MutableLiveData<ListState>(ListState.OK)
    var category: String? = null
    var search = MutableLiveData<String>("")

//    fun setProductsResponse(it: ProductsResponse?, append: Boolean = false) {
//        if (!append) {
//            products.clear()
//        }
//        products.addAll(it?.products ?: emptyList())
//        productsSize.value = it?.size ?: 0
//        state.value = if (products.isNotEmpty()) ListState.OK else ListState.NOT_FOUND
//    }

    fun setProducts(it: List<MCProductInfo>, append: Boolean = false) {
        if (!append) {
            products.clear()
        }
        products.addAll(it)
        productsSize.value = it.size.toLong()
    }
}
package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.ProductsResponse


class StoresViewModel : ViewModel() {
    var products = mutableListOf<ProductInfo>()
    var productsSize = 0L
    val loading = MutableLiveData<Boolean>(false)
    var category: String? = null
    var search = MutableLiveData<String>("")
//    var scrollPositionY = 0

    fun setProductsResponse(it: ProductsResponse?, append: Boolean = false) {
        if (!append) {
            products.clear()
        }
        products.addAll(it?.products ?: emptyList())
        productsSize = it?.size ?: 0
    }
}
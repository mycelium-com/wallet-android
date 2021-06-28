package com.mycelium.giftbox.cards.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.ProductsResponse


class StoresViewModel : ViewModel() {


    val products = MutableLiveData<List<ProductInfo>>(emptyList())
    var productsSize = 0L
    val loading = MutableLiveData<Boolean>(false)
    var category: String? = null
    var search: String? = null
    var quickSearch = false

    fun setProductsResponse(it: ProductsResponse?, append: Boolean = false) {
        if (append) {
            products.value = (products.value ?: emptyList()) +
                    (it?.products ?: emptyList())
        } else {
            products.value = it?.products ?: emptyList()
        }
        productsSize = it?.size ?: 0
    }
}
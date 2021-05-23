package com.mycelium.giftbox.client.models

data class ProductsResponse(
        val countries: List<String>?,
        val products: List<Product>?,
        val size: Int?
)

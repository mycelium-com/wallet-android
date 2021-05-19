package com.mycelium.giftbox.client

data class ProductsResponse(
        val countries: List<String>,
        val products: List<Product>,
        val size: Int
)

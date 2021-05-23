package com.mycelium.giftbox.client.models

data class ProductResponse(
        val priceCurrency: String?,
        val priceOffer: String?,
        val product: Product?,
        val similarProducts: List<Product>?
)

package com.mycelium.giftbox.client

data class ProductResponse(
        val priceCurrency: String,
        val priceOffer: String,
        val product: Product,
        val similarProducts: List<Product>?
)

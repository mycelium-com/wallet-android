package com.mycelium.giftbox.client.models

data class CheckoutProductResponse(
    val currencies: List<Currency>?,
    val fromCurrencyId: String?,
    val priceOffer: String?,
    val product: Product?
)
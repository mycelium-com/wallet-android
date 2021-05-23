package com.mycelium.giftbox.client.models

data class PriceResponse(
        val status: String?,
        val error_code: String?,
        val error_string: String?,
        val error_details: String?,
        val priceOffer: String?
)

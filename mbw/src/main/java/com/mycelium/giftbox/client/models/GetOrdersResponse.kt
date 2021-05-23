package com.mycelium.giftbox.client.models

data class GetOrdersResponse(
    val items: List<Item>?,
    val size: Int?,
    val status: String?
)
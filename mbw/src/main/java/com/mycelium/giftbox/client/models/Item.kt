package com.mycelium.giftbox.client.models

data class Item(
    val amount: String?,
    val client_order_id: String?,
    val items: Any,
    val product_code: String?,
    val product_img: String?,
    val product_name: String?,
    val quantity: String?,
    val status: String?,
    val timestamp: String?
)
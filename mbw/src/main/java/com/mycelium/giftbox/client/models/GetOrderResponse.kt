package com.mycelium.giftbox.client.models

data class GetOrderResponse(
    val amount: String?,
    val amount_expected_from: String?,
    val client_order_id: Any,
    val currency_code: String?,
    val currency_from: String?,
    val currency_from_info: CurrencyFromInfo,
    val pay_till: String?,
    val payin_address: String?,
    val payin_extra_id: String?,
    val payment_status: String?,
    val product_code: String?,
    val product_img: String?,
    val product_name: String?,
    val quantity: String?,
    val status: String?,
    val timestamp: String?,
    val tx_created_at: String?
)
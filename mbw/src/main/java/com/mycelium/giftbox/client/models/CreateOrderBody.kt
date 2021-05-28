package com.mycelium.giftbox.client.models

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateOrderBody(
    @JsonProperty("client_user_id") val clientUserId: String,
    @JsonProperty("client_order_id") val clientOrderId: String,
    @JsonProperty("code") val code: String,
    @JsonProperty("quantity") val quantity: Int,
    @JsonProperty("amount") val amount: Int,
    @JsonProperty("currency_id") val currencyId: String

)

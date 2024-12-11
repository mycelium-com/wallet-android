package com.mycelium.giftbox.client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderList(
    @JsonProperty("orders")
    val list: List<MCOrderResponse>
)

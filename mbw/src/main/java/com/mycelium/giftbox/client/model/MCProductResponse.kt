package com.mycelium.giftbox.client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MCProductResponse(
    @JsonProperty("total_count")
    val count: Int,
    @JsonProperty("items")
    val items: List<MCProductInfo>
)


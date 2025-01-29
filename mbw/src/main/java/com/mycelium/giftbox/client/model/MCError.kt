package com.mycelium.giftbox.client.model

import com.fasterxml.jackson.annotation.JsonProperty

data class MCError(
    @JsonProperty("code")
    val code: Int,
    @JsonProperty("title")
    val title: String,
    @JsonProperty("body")
    val body: String
)

data class MCErrorWrap(
    @JsonProperty("detail")
    val error: MCError
)
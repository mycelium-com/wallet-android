package com.mycelium.bequant.remote.client.models

import com.fasterxml.jackson.annotation.JsonProperty

data class OnceTokenResponse(
        @JsonProperty("expiredAt")
        val expiredAt: Long,
        @JsonProperty("token")
        val token: String
)
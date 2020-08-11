package com.mycelium.bequant.remote.client.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnceTokenResponse(
        @Json(name = "expiredAt")
        val expiredAt: Long,
        @Json(name = "token")
        val token: String
)
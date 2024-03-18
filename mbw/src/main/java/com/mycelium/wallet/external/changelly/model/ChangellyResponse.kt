package com.mycelium.wallet.external.changelly.model


class ChangellyResponse<T>(
    var result: T?,
    val error: Error? = null
)

class ChangellyListResponse<T>(
    var result: List<T>?,
    val error: Error? = null
)

data class Error(val code: Int, val message: String)


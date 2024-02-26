package com.mycelium.bequant.remote.model

data class User(
    val status: Status,
) {
    fun isVIP() = status == Status.VIP
    enum class Status {
        VIP,
        REGULAR,
    }
}
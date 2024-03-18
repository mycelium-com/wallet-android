package com.mycelium.bequant.remote.model

data class User(
    val status: Status = Status.REGULAR,
) {
    enum class Status {
        VIP,
        REGULAR;

        fun isVIP() = this == VIP
    }
}

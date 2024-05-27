package com.mycelium.bequant.remote.model

enum class UserStatus {
    VIP,
    REGULAR;

    fun isVIP() = this == VIP

    companion object {
        fun fromName(name: String?) = when (name) {
            VIP.name -> VIP
            REGULAR.name -> REGULAR
            else -> null
        }
    }
}

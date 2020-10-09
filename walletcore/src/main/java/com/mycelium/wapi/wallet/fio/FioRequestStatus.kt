package com.mycelium.wapi.wallet.fio

enum class FioRequestStatus(val status: String) {
    REQUESTED("requested"),
    PAID("paid"),
    NONE("none");

    companion object {
        fun getStatus(status: String) = values().first { it.status == status }
    }
}

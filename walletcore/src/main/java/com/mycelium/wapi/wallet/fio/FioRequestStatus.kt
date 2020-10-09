package com.mycelium.wapi.wallet.fio

enum class FioRequestStatus(val status: String) {
    SENT("sent"),
    PENDING("pending"),
    NONE("none");

    companion object {
        fun getStatus(status: String) = values().firstOrNull() { it.status == status }
                ?: NONE
    }
}

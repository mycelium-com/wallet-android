package com.mycelium.wapi.wallet.fio

enum class FioRequestStatus(val status: String ) {
    SENT("sent"),
    PENDING("pending");

    companion object {
        fun getStatus(status: String)  = values().first { it.status == status }
    }
}
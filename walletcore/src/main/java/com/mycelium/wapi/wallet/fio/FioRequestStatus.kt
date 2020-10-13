package com.mycelium.wapi.wallet.fio

enum class FioRequestStatus(val status: String) {
    REQUESTED("Not paid"),
    REJECTED("Rejected"),
    SENT_TO_BLOCKCHAIN("Paid"),
    NONE("None");

    companion object {
        fun getStatus(status: String) = values().first { it.status == status }
    }
}

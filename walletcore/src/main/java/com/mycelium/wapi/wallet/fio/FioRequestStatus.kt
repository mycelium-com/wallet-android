package com.mycelium.wapi.wallet.fio

enum class FioRequestStatus(val status: String) {
    REQUESTED("requested"),
    REJECTED("rejected"),
    SENT_TO_BLOCKCHAIN("sent_to_blockchain"),
    NONE("none");

    companion object {
        fun getStatus(status: String) = values().first { it.status == status }
    }
}

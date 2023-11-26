package com.mycelium.wallet.external.changelly2

import com.mycelium.wallet.external.changelly.model.ChangellyTransaction


fun ChangellyTransaction.getReadableStatus(prefix: String = "") =
        when (status) {
            "waiting" -> "%sin progress"
            "confirming" -> "%sin progress"
            "exchanging" -> "%sin progress"
            "sending" -> "%sin progress"
            "finished" -> "%scompleted"
            "failed" -> "%sfailed"
            "refunded" -> "%sfailed"
            "hold" -> "Hold"
            "expired" -> "%sexpired"
            else -> "Unknown tx status"
        }.let {
            if (prefix.isNotEmpty()) it.format("$prefix ")
            else it.format("")
        }.capitalize()
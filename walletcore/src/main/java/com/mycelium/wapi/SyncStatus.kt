package com.mycelium.wapi

import java.util.Date

enum class SyncStatus {
    UNKNOWN, SUCCESS, ERROR, INTERRUPT
    , ERROR_INTERNET_CONNECTION
}

data class SyncStatusInfo @JvmOverloads constructor(val status: SyncStatus, val timestamp: Date = Date())


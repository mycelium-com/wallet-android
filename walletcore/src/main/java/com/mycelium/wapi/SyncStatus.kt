package com.mycelium.wapi

import java.util.Date

enum class SyncStatus {
    UNKNOWN, SUCCESS, ERROR, INTERRUPT
}

data class SyncStatusInfo @JvmOverloads constructor(val status: SyncStatus, val timestamp: Date = Date())


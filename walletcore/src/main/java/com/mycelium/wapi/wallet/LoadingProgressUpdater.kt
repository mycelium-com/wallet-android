package com.mycelium.wapi.wallet

interface LoadingProgressUpdater {
    var status: LoadingProgressStatus
    var percent: Int

    fun clearLastFullUpdateTime()
}
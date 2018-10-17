package com.mycelium.wapi.wallet

interface LoadingProgressUpdater {
    var comment: String
    var percent: Int

    fun clearLastFullUpdateTime()
}
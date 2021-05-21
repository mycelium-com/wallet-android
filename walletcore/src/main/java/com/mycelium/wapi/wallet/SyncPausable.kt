package com.mycelium.wapi.wallet

import com.mycelium.wapi.api.request.CancelableRequest
import java.util.concurrent.ConcurrentSkipListSet

interface SyncPausable {
    /**
     * Interrupt gracefully ongoing sync.
     */
    fun interruptSync()
}


abstract class SyncPausableAccount : SyncPausable {
    @Volatile
    var maySync = true

    private var cancelableRequests = ConcurrentSkipListSet<CancelableRequest>()

    fun addCancelableRequest(request: CancelableRequest) {
        cancelableRequests.add(request)
    }

    fun clearCancelableRequests() {
        cancelableRequests.clear()
    }

    override fun interruptSync() {
        maySync = false
        cancelableRequests.forEach { it.cancel?.invoke() }
        clearCancelableRequests()
    }
}

fun Collection<WalletAccount<*>>.interruptSync() {
    forEach { it.interruptSync() }
}
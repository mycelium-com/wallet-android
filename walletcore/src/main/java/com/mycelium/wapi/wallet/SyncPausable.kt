package com.mycelium.wapi.wallet

import com.mycelium.wapi.api.request.CancelableRequest

interface SyncPausable {
    /**
     * Interrupt gracefully ongoing sync.
     */
    fun interruptSync()
}


abstract class SyncPausableAccount() : SyncPausable {
    @Volatile
    var maySync = true

    var cancelableRequest: CancelableRequest? = null
        set(value) {
            if (value != null && value.cancel == null) throw IllegalStateException("cancel function shouldn't be null")
            field = value
        }

    override fun interruptSync() {
        maySync = false
        cancelableRequest?.cancel?.invoke()
        cancelableRequest = null
    }
}

fun Collection<WalletAccount<*>>.interruptSync() {
    forEach { it.interruptSync() }
}
package com.mycelium.wapi.wallet

interface SyncPausable {
    /**
     * Interrupt gracefully ongoing sync.
     */
    fun interruptSync()
}


abstract class SyncPausableAccount() : SyncPausable {
    @Volatile
    var maySync = true

    override fun interruptSync() {
        maySync = false
    }
}

fun Collection<WalletAccount<*>>.interruptSync() {
    forEach { it.interruptSync() }
}
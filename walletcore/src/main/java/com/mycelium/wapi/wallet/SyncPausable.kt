package com.mycelium.wapi.wallet

interface SyncPausable {
    /**
     * Interrupt gracefully ongoing sync. This method is blocking until it takes effect.
     */
    fun interruptSync()

    /**
     * @return true if syncing was not interrupted by interruptSync()
     */
    fun maySync(): Boolean
}

open class SyncPausableContext: SyncPausable {
    @Volatile
    private var maySync = true

    override fun interruptSync() {
        maySync = false
        synchronized(this) {}
        maySync = true
    }

    override fun maySync() = maySync
}


open class SyncPausableAccount(val syncPausableContext: SyncPausableContext): SyncPausable {
    override fun interruptSync() {
        syncPausableContext.interruptSync()
    }

    override fun maySync() = syncPausableContext.maySync()
}
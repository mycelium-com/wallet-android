package com.mycelium.wapi.wallet

interface SyncPausable {
    /**
     * Interrupt ongoing sync and pause all sync. This method is blocking until the pause takes
     * effect.
     *
     * @param seconds duration of the pause
     */
    fun pauseSync(seconds: Int)

    /**
     * @return true if syncing was not paused by pauseSync
     */
    fun maySync(): Boolean
}

open class SyncPausableContext: SyncPausable {
    @Volatile
    private var noSyncUntilTs = 0L

    override fun pauseSync(seconds: Int) {
        noSyncUntilTs = System.currentTimeMillis() + seconds * 1000
        synchronized(this) {}
    }

    override fun maySync() = System.currentTimeMillis() > noSyncUntilTs
}


open class SyncPausableAccount(val syncPausableContext: SyncPausableContext): SyncPausable {
    override fun pauseSync(seconds: Int) {
        syncPausableContext.pauseSync(seconds)
    }

    override fun maySync() = syncPausableContext.maySync()
}
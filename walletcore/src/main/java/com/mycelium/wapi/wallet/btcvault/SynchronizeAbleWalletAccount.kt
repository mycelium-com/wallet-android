package com.mycelium.wapi.wallet.btcvault

import com.google.common.collect.ImmutableMap
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.SyncPausableAccount
import com.mycelium.wapi.wallet.WalletAccount
import java.util.*

abstract class SynchronizeAbleWalletAccount<ADDRESS : Address> : SyncPausableAccount(), WalletAccount<ADDRESS> {
    private val lastSync = hashMapOf<SyncMode.Mode, Date>()

    @Volatile
    private var isSyncing = false

    /**
     * Checks if the account needs to be synchronized, according to the provided SyncMode
     *
     * @param syncMode the requested sync mode
     * @return true if sync is needed
     */
    private fun needsSynchronization(syncMode: SyncMode): Boolean {
        if (syncMode.ignoreSyncInterval) {
            return true
        }

        val modeLastSync = lastSync[syncMode.mode]
        // never synced for this mode before - just do it. now.
                ?: return true
        // check how long ago the last sync for this mode
        val lastSyncAge = Date().time - modeLastSync.time
        return lastSyncAge > getSyncInterval(syncMode)
    }

    /**
     * Returns the normal sync interval for this mode
     * if synchronize() is called faster than this interval (and ignoreSyncInterval=false), the sync is disregarded
     *
     * @param syncMode the Mode to get the interval for
     * @return the interval in milliseconds
     */
    private fun getSyncInterval(syncMode: SyncMode): Int = MIN_SYNC_INTERVAL[syncMode.mode]!!

    /**
     * Synchronize this account
     *
     *
     * This method should only be called from the wallet manager
     *
     * @param mode set synchronization parameters
     * @return false if synchronization failed due to failed blockchain
     * connection
     */
    override suspend fun synchronize(mode: SyncMode?): Boolean {
        val fixMode = mode ?: SyncMode.NORMAL
        return if (needsSynchronization(fixMode)) {
            isSyncing = true
            val synced = doSynchronization(fixMode)
            isSyncing = false
            // if sync went well, remember current time for this sync mode
            if (synced) {
                lastSync[fixMode.mode] = Date()
                lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
            }
            synced
        } else {
            true
        }
    }

    override fun isSyncing(): Boolean = isSyncing

    override fun isVisible(): Boolean = true

    /**
     * Do the necessary steps to synchronize this account.
     * This function has to be implemented for the individual accounts and will only be called, if it is
     * needed (according to various timeouts, etc)
     *
     * @param mode SyncMode
     * @return true if sync was successful
     */
    protected abstract suspend fun doSynchronization(mode: SyncMode): Boolean

    override val dependentAccounts: List<WalletAccount<Address>>
        get() = listOf()


    companion object {
        private val MIN_SYNC_INTERVAL = ImmutableMap.of(
                SyncMode.Mode.FAST_SYNC, 1000,
                SyncMode.Mode.ONE_ADDRESS, 1000,
                SyncMode.Mode.NORMAL_SYNC, 30 * 1000,
                SyncMode.Mode.FULL_SYNC, 120 * 1000
        )
    }
}

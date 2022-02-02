package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class Synchronizer(val walletManager: WalletManager, val syncMode: SyncMode,
                   val accounts: List<WalletAccount<*>?> = listOf()) : Runnable {

    private val logger = Logger.getLogger(Synchronizer::class.simpleName)

    override fun run() {
        logger.log(Level.INFO, "Synchronizing start")
        walletManager.reportStartSync()
        walletManager.walletListener?.syncStarted()

        try {
            if (walletManager.isNetworkConnected) {
                // If we have any lingering outgoing transactions broadcast them now
                // this function goes over all accounts - it is reasonable to
                // exclude this from SyncMode.onlyActiveAccount behaviour
                if (!broadcastOutgoingTransactions()) {
                    return
                }

                // Synchronize selected accounts with the blockchain
                runSync(syncAccountList())
            } else {
                syncAccountList()
                        .forEach {
                            it.setLastSyncStatus(SyncStatusInfo(SyncStatus.ERROR_INTERNET_CONNECTION, Date()))
                        }
            }
        } finally {
            walletManager.reportStopSync()
            walletManager.walletListener?.syncStopped()
        }
    }

    private fun syncAccountList() =
            (if (accounts.isEmpty() ||
                    syncMode == SyncMode.FULL_SYNC_ALL_ACCOUNTS ||
                    syncMode == SyncMode.NORMAL_ALL_ACCOUNTS_FORCED) {
                walletManager.getAllActiveAccounts()
            } else {
                accounts.filterNotNull().filter { it.isActive }
            }).filter { !it.isSyncing }

    private fun runSync(list: List<WalletAccount<*>>) {
        //split synchronization by coinTypes in own threads
        runBlocking {
            list.filter {
                (it is SyncPausableAccount && it.maySync) || it !is SyncPausableAccount
            }.forEach {
                launch {
                    logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account ${it.id}: ...")
                    val timeStart = System.currentTimeMillis()
                    val isSyncSuccessful = try {
                        if (it is SyncPausableAccount && !it.maySync) {
                            false
                        } else {
                            it.synchronize(syncMode)
                        }
                    } catch (ex: Exception) {
                        logger.log(Level.SEVERE, "Sync error", ex)
                        false
                    }
                    val timeEnd = System.currentTimeMillis()
                    var syncTime = timeEnd - timeStart
                    logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account ${it.id}: ${if(isSyncSuccessful) "success" else "failed!"} ($syncTime ms)")
                }
            }
        }
        list.filterIsInstance(SyncPausableAccount::class.java).forEach {
            it.maySync = true
            it.clearCancelableRequests()
        }
    }

    private fun broadcastOutgoingTransactions(): Boolean =
            if (accounts.isEmpty()) {
                walletManager.getAllActiveAccounts()
            } else {
                accounts
            }
                    .filterNotNull()
                    .filterNot { it.isArchived }
                    .all { it.broadcastOutgoingTransactions() }
}

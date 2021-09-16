package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.SyncPausableAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
                val list = if (accounts.isEmpty() ||
                        syncMode == SyncMode.FULL_SYNC_ALL_ACCOUNTS ||
                        syncMode == SyncMode.NORMAL_ALL_ACCOUNTS_FORCED) {
                    walletManager.getAllActiveAccounts()
                } else {
                    accounts.filterNotNull().filter { it.isActive }
                }.filter { !it.isSyncing }
                runSync(list)
            }
        } finally {
            walletManager.reportStopSync()
            walletManager.walletListener?.syncStopped()
        }
    }

    private fun runSync(list: List<WalletAccount<*>>) {
        //split synchronization by coinTypes in own threads
        runBlocking {
            list.filter {
                (it is SyncPausableAccount && it.maySync) || it !is SyncPausableAccount
            }.forEach {
                launch {
                    logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account ${it.id}: ...")
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
                    logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account ${it.id}: ${if(isSyncSuccessful) "success" else "failed!"}")
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

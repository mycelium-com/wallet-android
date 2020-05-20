package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import kotlinx.coroutines.*
import java.util.logging.Level
import java.util.logging.Logger

class Synchronizer(val walletManager: WalletManager, val syncMode: SyncMode,
                   val accounts: List<WalletAccount<*>?> = listOf()) : Runnable {

    private val logger = Logger.getLogger(Synchronizer::class.simpleName)

    override fun run() {
        walletManager.state = State.SYNCHRONIZING
        walletManager.walletListener?.syncStarted()

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
            startSync(list)
        }
    }

    private fun startSync(list: List<WalletAccount<*>>) {
        //split synchronization by coinTypes in own threads
        GlobalScope.launch(Dispatchers.Default) {
                list.map {
                    async {
                        val accountLabel = it.label ?: ""
                        logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account $accountLabel with id ${it.id}")
                        var isSyncSuccessful = false;
                        try {
                            isSyncSuccessful = it.synchronize(syncMode)
                        } catch (ex: Exception) {
                            logger.log(Level.WARNING,"Sync error", ex)
                        }
                        logger.log(Level.INFO, "Account ${it.id} sync result: ${isSyncSuccessful}")
                    }
                }.map {
                    it.await()
                }
        }.invokeOnCompletion {
            walletManager.state = State.READY
            walletManager.walletListener?.syncStopped()
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

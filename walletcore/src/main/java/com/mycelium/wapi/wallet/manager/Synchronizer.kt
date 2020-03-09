package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class Synchronizer(val walletManager: WalletManager, val syncMode: SyncMode,
                   val accounts: List<WalletAccount<*>?> = listOf()) : Runnable {

    private val logger = Logger.getLogger(Synchronizer::class.simpleName)

    companion object {
        private val lock = Any()
    }

    override fun run() {
        walletManager.state = State.SYNCHRONIZING
        walletManager.walletListener?.syncStarted()

        try {
            synchronized(lock) {
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
                    }
                    startSync(list)
                }
            }
        } finally {
            walletManager.state = State.READY
            walletManager.walletListener?.syncStopped()
        }
    }

    private fun startSync(list: List<WalletAccount<*>>) {
        //split synchronization by coinTypes in own threads
        runBlocking(Dispatchers.Default) {
            list.map {
                        async {
                            val accountLabel = it.label ?: ""
                            logger.log(Level.INFO, "Synchronizing ${it.coinType.symbol} account $accountLabel with id ${it.id}")
                            val isSyncSuccessful = it.synchronize(syncMode)
                            logger.log(Level.INFO, "Account ${it.id} sync result: ${isSyncSuccessful}")
                        }
                    }.map {
                        it.await()
                    }
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

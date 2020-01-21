package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class Synchronizer(val walletManager: WalletManager, val syncMode: SyncMode,
                   val accounts: List<WalletAccount<*>?> = listOf()) : Runnable {

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
                    val list = if (accounts.isEmpty()) {
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
            list.groupBy { it.coinType }
                    .values.flatten()
                    .map {
                        async { it.synchronize(syncMode) }
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

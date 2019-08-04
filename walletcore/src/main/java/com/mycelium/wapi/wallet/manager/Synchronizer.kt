package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager

class Synchronizer(val walletManager: WalletManager, val syncMode: SyncMode,
                   val accounts: List<WalletAccount<*>?> = listOf()) : Runnable {

    override fun run() {
        walletManager.state = State.SYNCHRONIZING
        walletManager.walletListener?.syncStarted()

        try {
            synchronized(walletManager.getAllActiveAccounts()) {
                if (walletManager.isNetworkConnected) {

                    // If we have any lingering outgoing transactions broadcast them now
                    // this function goes over all accounts - it is reasonable to
                    // exclude this from SyncMode.onlyActiveAccount behaviour
                    if (!broadcastOutgoingTransactions()) {
                        return
                    }

                    // Synchronize selected accounts with the blockchain
                    val list = if (accounts.isEmpty()) walletManager.getAllActiveAccounts() else accounts
                    list.forEach { it!!.synchronize(syncMode) }
                }

            }
        } finally {
            walletManager.state = State.READY
            walletManager.walletListener?.syncStopped()
        }
    }

    private fun broadcastOutgoingTransactions(): Boolean =
            if (accounts.isEmpty()) { walletManager.getAllActiveAccounts() } else { accounts }
                    .filterNotNull()
                    .filterNot { it.isArchived }
                    .all { it.broadcastOutgoingTransactions() }
}

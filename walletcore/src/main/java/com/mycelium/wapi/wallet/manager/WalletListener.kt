package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.WalletAccount


interface WalletListener {
    fun syncStarted()
    fun syncStopped()
    fun accountSyncStopped(walletAccount: WalletAccount<*>)
}
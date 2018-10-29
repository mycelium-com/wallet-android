package com.mycelium.wapi.wallet.manager


interface WalletListener {
    fun syncStarted()
    fun syncStopped()
}
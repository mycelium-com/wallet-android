package com.mycelium.wapi.wallet


interface AccountListener {
    fun balanceUpdated(walletAccount: WalletAccount<*, *>)
}
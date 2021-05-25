package com.mycelium.wapi.wallet


interface AccountListener {
    fun balanceUpdated(walletAccount: WalletAccount<*>)
    fun serverConnectionError(walletAccount: WalletAccount<*>, s: String)
    fun receivingAddressChanged(walletAccount: WalletAccount<*>, receivingAddress: Address)
}
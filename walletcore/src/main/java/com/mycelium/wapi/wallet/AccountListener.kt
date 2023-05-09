package com.mycelium.wapi.wallet

import java.util.UUID

interface AccountListener {
    fun balanceUpdated(walletAccount: WalletAccount<*>)
    fun serverConnectionError(walletAccount: WalletAccount<*>, s: String)
    fun receivingAddressChanged(walletAccount: WalletAccount<*>, receivingAddress: Address)
    fun onAccountActiveStateChanged(id: UUID)
}
package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.WalletAccount
import java.util.*


interface WalletModule {
    fun getId(): String
    fun loadAccounts(): Map<UUID, WalletAccount<*, *>>

    fun createAccount(config: Config): WalletAccount<*, *>?

    fun canCreateAccount(config: Config): Boolean

    fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean
}
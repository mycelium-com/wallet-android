package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import java.util.*


interface WalletModule {
    fun getId(): String
    fun loadAccounts(): Map<UUID, WalletAccount<*, *>>

    fun createAccount(config: Config): WalletAccount<*, *>?

    fun canCreateAccount(config: Config): Boolean

    fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean

    fun getSupportedAssets(): List<GenericAssetInfo>
}
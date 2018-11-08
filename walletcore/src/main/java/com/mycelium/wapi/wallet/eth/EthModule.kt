package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*
import kotlin.collections.HashMap

class EthModule(internal val metaDataStorage: IMetaDataStorage) : WalletModule {

    override fun getId(): String = "ETH"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val map = HashMap<UUID, WalletAccount<*,*>>()
        return map
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        return EthAccount()
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is EthConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        return true
    }

}
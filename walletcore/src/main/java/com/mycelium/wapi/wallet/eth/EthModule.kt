package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*
import kotlin.collections.HashMap

class EthModule(metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {

    override fun getId(): String = "ETH"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val map = HashMap<UUID, WalletAccount<*,*>>()
        return map
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        val newEthAccount = EthAccount()

        val baseName = "Ethereum"
        newEthAccount.label = createLabel(baseName, newEthAccount.id)
        return newEthAccount
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is EthConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        return true
    }

}
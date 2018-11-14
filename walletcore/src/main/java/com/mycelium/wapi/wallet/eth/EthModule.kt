package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.metadata.MetadataCategory
import java.util.*
import kotlin.collections.HashMap

class EthModule(internal val metaDataStorage: IMetaDataStorage) : WalletModule {

    override fun getId(): String = "ETH"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val map = HashMap<UUID, WalletAccount<*,*>>()
        return map
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        val newEthAccount = EthAccount()

        val baseName = "Ethereum"
        //append counter if name already in use
        var defaultName = baseName
        var num = 1

        val metadataKeyCategory = MetadataCategory("al")
        // while the account for this label exists, change the label (increment index)
        while (metaDataStorage.getFirstKeyForCategoryValue(metadataKeyCategory.category, defaultName).isPresent) {
            defaultName = baseName + " (" + num++ + ')'.toString()
        }

        // we just put the default name into storage first, if there is none
        // if the user cancels entry or it gets somehow aborted, we at least have a valid entry
        if (metaDataStorage.getKeyCategoryValueEntry(metadataKeyCategory.of(newEthAccount.id.toString()).key,
                        metadataKeyCategory.category, "").isEmpty()) {
            metaDataStorage.storeKeyCategoryValueEntry(metadataKeyCategory.of(newEthAccount.id.toString()), defaultName)
        }

        newEthAccount.label = defaultName
        return newEthAccount
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is EthConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        return true
    }

}
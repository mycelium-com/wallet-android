package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.metadata.MetadataCategory
import java.util.UUID

abstract class GenericModule(private val metaDataStorage: IMetaDataStorage) : WalletModule {
    protected val assetsList = mutableListOf<GenericAssetInfo>()


    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        TODO("NOt implemeted")
    }

    // creates label for the account and stores in the database
    fun createLabel(baseName: String) : String {

        //append counter if name already in use
        var defaultName = baseName
        var num = 1

        val metadataKeyCategory = MetadataCategory("al")
        // while the account for this label exists, change the label (increment index)
        while (metaDataStorage.getFirstKeyForCategoryValue(metadataKeyCategory.category, defaultName).isPresent) {
            defaultName = baseName + " (" + num++ + ')'.toString()
        }

        return defaultName
    }

    fun storeLabel(accountId: UUID, label: String): String {
        val metadataKeyCategory = MetadataCategory("al")
        metaDataStorage.storeKeyCategoryValueEntry(metadataKeyCategory.of(accountId.toString()), label)
        return label
    }

    // reads label from the database by account UUID
    fun readLabel(accountId: UUID) : String {
        val metadataKeyCategory = MetadataCategory("al")

        return metaDataStorage.getKeyCategoryValueEntry(metadataKeyCategory.of(accountId.toString()).key,
                metadataKeyCategory.category, "")
    }

    override fun getSupportedAssets(): List<GenericAssetInfo> {
        return assetsList
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun afterAccountsLoaded() {
    }
}
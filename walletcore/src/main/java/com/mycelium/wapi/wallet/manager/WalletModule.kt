package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.metadata.MetadataCategory
import java.lang.IllegalStateException
import java.util.*


abstract class WalletModule(private val metaDataStorage: IMetaDataStorage) {
    protected val assetsList = mutableListOf<AssetInfo>()

    open fun loadAccounts(): Map<UUID, WalletAccount<*>> = TODO("NOt implemeted")

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

    open fun getSupportedAssets(): List<AssetInfo> = assetsList

    open fun setCurrencySettings(currencySettings: CurrencySettings): Unit =
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    open fun afterAccountsLoaded() {}

    abstract val id: String

    @Throws(IllegalStateException::class)
    abstract fun createAccount(config: Config): WalletAccount<*>

    abstract fun canCreateAccount(config: Config): Boolean

    abstract fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean

    abstract fun getAccounts(): List<WalletAccount<*>>

    abstract fun getAccountById(id: UUID): WalletAccount<*>?
}
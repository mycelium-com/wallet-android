package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.metadata.MetadataCategory
import java.util.*

abstract class GenericModule(private val metaDataStorage: IMetaDataStorage) : WalletModule {

    // creates label for the account and stores in the database
    fun createLabel(baseName: String, accountId: UUID) : String {

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
        if (metaDataStorage.getKeyCategoryValueEntry(metadataKeyCategory.of(accountId.toString()).key,
                        metadataKeyCategory.category, "").isEmpty()) {
            metaDataStorage.storeKeyCategoryValueEntry(metadataKeyCategory.of(accountId.toString()), defaultName)
        }

        return defaultName
    }

}
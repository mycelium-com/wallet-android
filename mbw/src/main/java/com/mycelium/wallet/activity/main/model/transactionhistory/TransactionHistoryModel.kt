package com.mycelium.wallet.activity.main.model.transactionhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.persistence.MetadataStorage

/**
 * Model for [com.mycelium.wallet.activity.main.TransactionHistoryFragment]
 */
class TransactionHistoryModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    val transactionHistory = TransactionHistoryLiveData(mbwManager)
    val addressBook = mbwManager.metadataStorage.allAddressLabels
    val storage = mbwManager.metadataStorage

    fun cacheAddressBook() {
        addressBook.clear()
        addressBook.putAll(mbwManager.metadataStorage.allAddressLabels)
    }
}
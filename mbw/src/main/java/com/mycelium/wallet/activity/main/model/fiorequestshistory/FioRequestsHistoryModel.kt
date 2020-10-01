package com.mycelium.wallet.activity.main.model.fiorequestshistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mycelium.wallet.MbwManager

/**
 * Model for [com.mycelium.wallet.activity.main.TransactionHistoryFragment]
 */
class FioRequestsHistoryModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    val fioRequestHistory = FioRequestsLiveData(mbwManager)
    val addressBook = mbwManager.metadataStorage.allAddressLabels

    fun cacheAddressBook() {
        addressBook.clear()
        addressBook.putAll(mbwManager.metadataStorage.allAddressLabels)
    }
}
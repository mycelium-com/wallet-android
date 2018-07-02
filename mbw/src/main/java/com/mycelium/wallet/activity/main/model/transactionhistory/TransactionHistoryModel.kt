package com.mycelium.wallet.activity.main.model.transactionhistory

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.mrd.bitlib.model.Address
import com.mycelium.wallet.MbwManager

/**
 * Model for [com.mycelium.wallet.activity.main.TransactionHistoryFragment]
 */
class TransactionHistoryModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)!!
    val transactionHistory = TransactionHistoryLiveData(mbwManager)
    @Volatile
    var addressBook: Map<Address, String>? = null

    fun cacheAddressBook() {
        addressBook = mbwManager.metadataStorage.allAddressLabels
    }
}
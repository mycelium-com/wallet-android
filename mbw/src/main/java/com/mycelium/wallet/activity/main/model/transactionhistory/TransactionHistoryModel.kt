package com.mycelium.wallet.activity.main.model.transactionhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.WalletAccount

/**
 * Model for [com.mycelium.wallet.activity.main.TransactionHistoryFragment]
 */
class TransactionHistoryModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    val addressBook = mbwManager.metadataStorage.allAddressLabels
    val storage = mbwManager.metadataStorage
    val account = MutableLiveData<WalletAccount<*>>()

    var txs: TransactionHistoryLiveData? = null // TODO refactor append  logic

    val transactionHistory =
            account.switchMap {
                TransactionHistoryLiveData(mbwManager, application, it).apply {
                    txs = this
                }
            }

    /** Re-creates the history LiveData for the current account (e.g. after the TRON filter changed). */
    fun reloadHistory() {
        account.value = account.value
    }

    fun cacheAddressBook() {
        addressBook.clear()
        addressBook.putAll(mbwManager.metadataStorage.allAddressLabels)
    }
}
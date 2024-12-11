package com.mycelium.wallet.activity.main.model.transactionhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIOOBTransaction

/**
 * Model for [com.mycelium.wallet.activity.main.TransactionHistoryFragment]
 */
class TransactionHistoryModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    val addressBook = mbwManager.metadataStorage.allAddressLabels
    val storage = mbwManager.metadataStorage
    val account = MutableLiveData<WalletAccount<*>>()
    val fioMetadataMap = mutableMapOf<String, FIOOBTransaction>()

    var txs: TransactionHistoryLiveData? = null // TODO refactor append  logic

    val transactionHistory =
            account.switchMap {
                TransactionHistoryLiveData(mbwManager, it, fioMetadataMap).apply {
                    txs = this
                }
            }

    fun cacheAddressBook() {
        addressBook.clear()
        addressBook.putAll(mbwManager.metadataStorage.allAddressLabels)
    }
}
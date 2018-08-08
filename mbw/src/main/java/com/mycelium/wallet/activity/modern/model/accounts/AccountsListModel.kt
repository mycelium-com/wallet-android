package com.mycelium.wallet.activity.modern.model.accounts

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import com.mycelium.wallet.MbwManager

/**
 * Model for [com.mycelium.wallet.activity.modern.adapter.AccountListAdapter]
 */
class AccountsListModel(application: Application) : AndroidViewModel(application) {
    val accountsData : AccountsViewLiveData
    init {
        val mbwManager = MbwManager.getInstance(application)!!
        accountsData = AccountsViewLiveData(mbwManager)
    }
}
package com.mycelium.wallet.activity.modern.model.accounts

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.content.Context
import com.mycelium.wallet.MbwManager

class AccountsListModel(application: Application) : AndroidViewModel(application) {
    val accountsData : AccountsLiveData
    init {
        val mbwManager = MbwManager.getInstance(application)!!
        accountsData = AccountsLiveData(application, mbwManager,
                application.getSharedPreferences("account_list", Context.MODE_PRIVATE))
    }
}
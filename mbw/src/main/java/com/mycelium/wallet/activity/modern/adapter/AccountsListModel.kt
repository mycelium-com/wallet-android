package com.mycelium.wallet.activity.modern.adapter

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.content.Context
import com.mycelium.wallet.MbwManager

class AccountsListModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)!!
    val accountsData = AccountsLiveData(application, mbwManager,
            application.getSharedPreferences("account_list", Context.MODE_PRIVATE))
}
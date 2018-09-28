package com.mycelium.wallet.activity.export

import android.app.Application
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.ExportableAccount

class ExportAsQrModel(val context: Application,
                      val accountData : ExportableAccount.Data) {

    private val mbwManager = MbwManager.getInstance(context)

    init {
        mbwManager.eventBus.register(this)
    }

    fun onCleared() {
        mbwManager.stopWatchingAddress()
        mbwManager.eventBus.unregister(this)
    }
}
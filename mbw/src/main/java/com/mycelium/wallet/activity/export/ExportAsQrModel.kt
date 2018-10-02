package com.mycelium.wallet.activity.export

import android.app.Application
import com.mycelium.wapi.wallet.ExportableAccount

class ExportAsQrModel(val context: Application,
                      val accountData : ExportableAccount.Data) {

    init {
    }

}
package com.mycelium.wallet.activity.export

import android.app.Application

abstract class ExportAsQrMultiKeysViewModel(context: Application) : ExportAsQrViewModel(context) {
    /**
     * Updates account data based on extra toggles
     */
    abstract fun onToggleClicked(toggleNum: Int)
}
package com.mycelium.bequant.common

import android.app.AlertDialog
import android.content.Context


class ErrorHandler(val context: Context) {
    fun handle() {
        AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Error")
                .create()
                .show()
    }
}
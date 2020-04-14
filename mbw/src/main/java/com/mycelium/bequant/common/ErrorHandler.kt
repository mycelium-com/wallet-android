package com.mycelium.bequant.common

import android.app.AlertDialog
import android.content.Context


class ErrorHandler(val context: Context) {
    fun handle(error: String) {
        AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(error)
                .create()
                .show()
    }
}
package com.mycelium.wallet.external.mediaflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build


class NewsSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val syncIntent = Intent(context, NewsSyncService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(syncIntent)
        } else {
            context.startService(syncIntent)
        }
    }
}



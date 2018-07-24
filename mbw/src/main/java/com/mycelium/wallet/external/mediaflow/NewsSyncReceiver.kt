package com.mycelium.wallet.external.mediaflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NewsSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startService(Intent(context, NewsSyncService::class.java))
    }
}



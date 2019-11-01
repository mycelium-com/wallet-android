package com.mycelium.wallet.external.mediaflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build


class NewsSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NewsSyncUtils.startNewsSyncService(context)

    }
}



package com.mycelium.wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NetworkChangedReceiver : BroadcastReceiver() {
    // We receive this event on wallet start, but this would start heavy init, which we don't want to.
    var wasInited: Boolean = false
    override fun onReceive(context: Context, intent: Intent) {
        if (!wasInited) {
            wasInited = true
            return
        }
        if (intent.action == "android.net.conn.CONNECTIVITY_CHANGE") {
            val mbwManager = MbwManager.getInstance(context)
            val connected = Utils.isConnected(context)
            mbwManager.getWalletManager(false).setNetworkConnected(connected)
            if (mbwManager.hasColoredAccounts()) {
                GlobalScope.launch(Dispatchers.Default, CoroutineStart.DEFAULT) {
                    mbwManager.coluManager.setNetworkConnected(connected)
                }
            }
            mbwManager.wapi.setNetworkConnected(connected)
        }
    }
}

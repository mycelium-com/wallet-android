
package com.mycelium.wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.net.conn.CONNECTIVITY_CHANGE") {
            val mbwManager = MbwManager.getInstance(context)
            val connected = Utils.isConnected(context)
            mbwManager.getWalletManager(false).setNetworkConnected(connected)
            mbwManager.coluManager.setNetworkConnected(connected)
            mbwManager.wapi.setNetworkConnected(connected)
        }
    }
}

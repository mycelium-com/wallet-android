package com.mycelium.wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mycelium.wallet.event.NetworkConnectionStateChanged
import java.util.logging.Level
import java.util.logging.Logger

class NetworkChangedReceiver : BroadcastReceiver() {
    // We receive this event on wallet start, but this would start heavy init, which we don't want to.
    var wasInited: Boolean = false

    val logger = Logger.getLogger(NetworkChangedReceiver::class.java.simpleName)

    override fun onReceive(context: Context, intent: Intent) {
        if (!wasInited) {
            wasInited = true
            return
        }
        if (intent.action == "android.net.conn.CONNECTIVITY_CHANGE") {
            val mbwManager = MbwManager.getInstance(context)
            val connected = Utils.isConnected(context, "CONNECTIVITY_CHANGE")
            logger.log(Level.INFO, "Connectivity status has been changed. Connected: $connected")
            mbwManager.getWalletManager(false).isNetworkConnected = connected
            mbwManager.wapi.setNetworkConnected(connected)
            mbwManager.btcvWapi.setNetworkConnected(connected)
            MbwManager.getEventBus().post(NetworkConnectionStateChanged(connected))
        }
    }
}

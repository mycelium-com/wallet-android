package com.mycelium.wallet.modularisation

import android.app.Activity
import android.app.IntentService
import android.app.Notification
import android.content.*
import android.net.Uri
import android.os.Build
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R.string.partner_commas_url

private const val TBM_PACKAGE = "com.castor.threecommas.tradingbotsmodule"
private const val MESSAGE = "message"
private const val REQUEST_SIGNATURE = "com.mycelium.wallet.signData"
private const val PERMISSION = "${BuildConfig.APPLICATION_ID}.permission.M2W_MESSAGE"

class TBMHelper(private val context: Context) {

    companion object {
        private const val RECEIVE_TX_ID = "receive_tx_id"
        private const val TRANSACTION_ID = "transaction_id"
        private const val OPEN_COMMAS = "com.mycelium.action.OPEN_COMMAS_MODULE"
    }

    init {
        context.registerReceiver(TBMRequestReceiver(), IntentFilter(REQUEST_SIGNATURE), PERMISSION, null)
    }

    fun sendTxIdToModule(id: String) {
        val transferIntent = Intent(RECEIVE_TX_ID).also {
            it.putExtra(TRANSACTION_ID, id)
            it.`package` = TBM_PACKAGE
        }
        context.sendBroadcast(transferIntent, PERMISSION)
    }

    fun openModule(activity: Activity?) {
        try {
            activity?.startActivityForResult(Intent(OPEN_COMMAS), 1)
        } catch (e: ActivityNotFoundException) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.data = Uri.parse(context.getString(partner_commas_url))
            activity?.startActivity(installIntent)
        }
    }
}

class TBMRequestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(MESSAGE)
        val serviceIntent = Intent(context, TBMService::class.java).also {
            it.putExtra(MESSAGE, message)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}

class TBMService : IntentService("TBMService") {
    companion object {
        private const val NOTIFICATION_ID = 1278468
        private const val RECEIVE_SIGNATURE = "com.mycelium.wallet.transferSignedData"
        private const val SIGNATURE = "signature"
        private const val MYCELIUM_ID = "mycelium_Id"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = Notification()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val mbwManager = MbwManager.getInstance(this)
        val messageToSign = intent?.getStringExtra(MESSAGE)

        messageToSign?.let {
            val signature = mbwManager.signMessage(messageToSign)
            val myceliumId = mbwManager.myceliumId
            val transferIntent = Intent(RECEIVE_SIGNATURE).also {
                it.putExtra(SIGNATURE, signature)
                it.putExtra(MYCELIUM_ID, myceliumId)
                it.`package` = TBM_PACKAGE
            }
            sendBroadcast(transferIntent, PERMISSION)
        }
    }
}
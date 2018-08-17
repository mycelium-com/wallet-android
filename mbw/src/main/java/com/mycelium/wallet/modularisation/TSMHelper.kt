package com.mycelium.wallet.modularisation

import android.app.Activity
import android.content.*
import android.net.Uri
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R.string.partner_commas_url

private const val TSM_PACKAGE = BuildConfig.appIdTsm
private const val PERMISSION = "${BuildConfig.APPLICATION_ID}.permission.M2W_MESSAGE"

class TSMHelper(private val context: Context) {

    companion object {
        private const val REQUEST_SIGNATURE = "com.mycelium.wallet.signData"
        private const val OPEN_COMMAS = "com.mycelium.action.OPEN_MODULE"
    }

    init {
        context.registerReceiver(TBMRequestReceiver(), IntentFilter(REQUEST_SIGNATURE), PERMISSION, null)
    }

    fun openModule(activity: Activity?) {
        try {
            val intent = Intent(OPEN_COMMAS)
            intent.`package` = TSM_PACKAGE
            intent.putExtra("callingPackage", context.packageName)
            activity?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.data = Uri.parse(context.getString(partner_commas_url))
            activity?.startActivity(installIntent)
        }
    }
}

class TBMRequestReceiver : BroadcastReceiver() {

    companion object {
        private const val RECEIVE_SIGNATURE = "com.mycelium.wallet.transferSignedData"
        private const val SIGNATURE = "signature"
        private const val MYCELIUM_ID = "mycelium_Id"
        private const val MESSAGE = "message"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val mbwManager = MbwManager.getInstance(context)

        val message = intent.getStringExtra(MESSAGE)
        val signature = mbwManager.signMessage(message)
        val myceliumId = mbwManager.myceliumId

        val transferIntent = Intent(RECEIVE_SIGNATURE).also {
            it.putExtra(SIGNATURE, signature)
            it.putExtra(MYCELIUM_ID, myceliumId)
            it.`package` = TSM_PACKAGE
        }
        context.sendBroadcast(transferIntent, PERMISSION)
    }
}

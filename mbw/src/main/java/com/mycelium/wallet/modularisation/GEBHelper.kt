package com.mycelium.wallet.modularisation

import android.app.Activity
import android.content.*
import android.net.Uri
import com.mycelium.modularizationtools.CommunicationManager
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R.string.get_extra_btc_url

class GEBHelper(private val context: Context) {

    private val OPEN_GEB = "com.mycelium.action.OPEN_MODULE"
    private val GEB_PACKAGE = BuildConfig.appIdGeb

    init {
        CommunicationManager.getInstance().requestPair(GEB_PACKAGE)
    }

    fun openModule(activity: Activity?) {
        try {
            val intent = Intent(OPEN_GEB)
            intent.`package` = GEB_PACKAGE
            intent.putExtra("callingPackage", context.packageName)
            activity?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.data = Uri.parse(context.getString(get_extra_btc_url))
            activity?.startActivity(installIntent)
        }
    }
}


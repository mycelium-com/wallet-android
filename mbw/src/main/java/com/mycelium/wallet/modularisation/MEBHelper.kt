package com.mycelium.wallet.modularisation

import android.app.Activity
import android.content.*
import android.net.Uri
import com.mycelium.modularizationtools.CommunicationManager
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R.string.make_extra_btc_url

class MEBHelper(private val context: Context) {

    private val OPEN_MEB = "com.mycelium.action.OPEN_MODULE"
    private val MEB_PACKAGE = BuildConfig.appIdMeb

    init {
        CommunicationManager.getInstance().requestPair(MEB_PACKAGE)
    }

    fun openModule(activity: Activity?) {
        try {
            val intent = Intent(OPEN_MEB)
            intent.`package` = MEB_PACKAGE
            intent.putExtra("callingPackage", context.packageName)
            activity?.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val installIntent = Intent(Intent.ACTION_VIEW)
            installIntent.data = Uri.parse(context.getString(make_extra_btc_url))
            activity?.startActivity(installIntent)
        }
    }
}


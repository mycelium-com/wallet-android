package com.mycelium.wallet.modularisation

import android.app.Activity
import android.content.*
import android.net.Uri
import com.mycelium.modularizationtools.CommunicationManager
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R.string.partner_commas_url

class TSMHelper(private val context: Context) {

    private val OPEN_TSM = "com.mycelium.action.OPEN_MODULE"
    private val TSM_PACKAGE = BuildConfig.appIdTsm

    init {
        CommunicationManager.getInstance().requestPair(TSM_PACKAGE)
    }

    fun openModule(activity: Activity?) {
        try {
            val intent = Intent(OPEN_TSM)
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


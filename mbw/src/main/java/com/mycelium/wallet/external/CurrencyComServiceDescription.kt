package com.mycelium.wallet.external

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wapi.wallet.GenericAddress


class CurrencyComServiceDescription :
        BuySellServiceDescriptor(R.string.currencycom_title, R.string.currencycom_description, R.string.currencycom_settings_description, R.drawable.ic_currencycom) {

    val appPackageName = "com.currency.exchange.prod"

    override fun launchService(activity: Activity?, mbwManager: MbwManager?, activeReceivingAddress: GenericAddress?) {

        try {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (anfe: ActivityNotFoundException) {
            activity?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

    override fun isEnabled(mbwManager: MbwManager?): Boolean = SettingsPreference.currencycomEnabled

    override fun setEnabled(mbwManager: MbwManager?, enabledState: Boolean) {
        SettingsPreference.currencycomEnabled = enabledState
    }
}
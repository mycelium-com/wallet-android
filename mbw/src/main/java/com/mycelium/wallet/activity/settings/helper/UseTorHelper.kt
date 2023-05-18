package com.mycelium.wallet.activity.settings.helper

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.appcompat.app.AlertDialog
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import info.guardianproject.netcipher.proxy.OrbotHelper

object UseTorHelper {
    @JvmStatic
    fun promptToInstall(activity: Activity) {
        // show dialog - install from market, f-droid or direct APK
        showDownloadDialog(activity, activity.getString(R.string.install_orbot_),
                activity.getString(R.string.you_must_have_orbot),
                activity.getString(R.string.yes), activity.getString(R.string.no), OrbotHelper.ORBOT_MARKET_URI)
    }

    private fun showDownloadDialog(activity: Activity,
                                   stringTitle: CharSequence, stringMessage: CharSequence, stringButtonYes: CharSequence,
                                   stringButtonNo: CharSequence, uriString: String) {
        AlertDialog.Builder(activity)
                .setTitle(stringTitle)
                .setMessage(stringMessage)
                .setPositiveButton(stringButtonYes) { dialogInterface, i ->
                    try {
                        OrbotHelper.get(activity).installOrbot(activity)
                    } catch (e: ActivityNotFoundException) {
                        Toaster(activity).toast(R.string.no_google_play_installed, false)
                    }
                }
                .setNegativeButton(stringButtonNo) { dialogInterface, i -> }
                .create()
                .show()
    }
}
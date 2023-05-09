package com.mycelium.wallet.activity.modern

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo

/**
 * Helper class that makes it easy to let a new toast replace another if they
 * come in rapid succession
 */
class Toaster(val context: Context) {

    private var fragment: Fragment? = null

    constructor(fragment: Fragment) : this(fragment.requireContext()) {
        this.fragment = fragment
    }


    fun toast(resourceId: Int, shortDuration: Boolean) {
        // Resolve the message from the resource id
        try {
            toast(context.resources.getString(resourceId), shortDuration)
        } catch (ignore: Exception) {
        }
    }

    fun toast(message: String, shortDuration: Boolean) {
        cancelCurrentToast()
        if(!MbwManager.getInstance(context).isAppInForeground) {
            return
        }
        if (fragment != null && !fragment!!.isAdded) {
            return
        }
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply {
            duration = if (shortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            show()
        }
    }

    @SuppressLint("StringFormatInvalid")
    @JvmOverloads
    fun toastConnectionError(additionInfo: String? = "") {
        if (Utils.isConnected(context)) {
            toast(context.getString(R.string.no_server_connection, additionInfo), false)
        } else {
            toast(R.string.no_network_connection, true)
        }
    }

    fun toastSyncFailed(syncStatusInfo: SyncStatusInfo? = null) {
        toast(when(syncStatusInfo?.status) {
            SyncStatus.INTERRUPT -> {
                context.getString(R.string.sync_failed_reason_s, context.getString(R.string.interrupted_by_application))
            }
            SyncStatus.ERROR -> {
                context.getString(R.string.sync_failed_reason_s, context.getString(R.string.no_server_connection, ""))
            }
            SyncStatus.ERROR_INTERNET_CONNECTION -> {
                context.getString(R.string.sync_failed_reason_s, context.getString(R.string.no_network_connection))
            }
            else -> {
                context.getString(R.string.sync_failed_s, "")
            }
        }, false)
    }

    companion object {
        private var currentToast: Toast? = null

        @JvmStatic
        fun onStop() {
            cancelCurrentToast()
        }

        private fun cancelCurrentToast() {
            currentToast?.cancel()
            currentToast = null
        }
    }
}
package com.mycelium.wallet.external.partner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.Toaster

fun Fragment.startContentLink(link: String?, data: Bundle? = null) {
    startContentLink(link) {
        data?.apply { it.putExtras(data) }
        startActivity(it)
    }
}

fun Activity.startContentLink(link: String?, data: Bundle? = null) {
    startContentLink(link) {
        data?.apply { it.putExtras(data) }
        startActivity(it)
    }
}

fun Context.startContentLink(link: String?, data: Bundle? = null) {
    startContentLink(link) {
        data?.apply { it.putExtras(data) }
        startActivity(it)
    }
}

private fun startContentLink(link: String?, startAction: (Intent) -> Unit) {
    if (link != null) {
        try {
            if (link.startsWith("mycelium://action.")) {
                startAction(Intent(Uri.parse(link).host).apply {
                    setPackage(WalletApplication.getInstance().packageName)
                }.addFlags(FLAG_ACTIVITY_SINGLE_TOP))
            } else {
                startAction(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (ignored: Exception) {
        }
    }
}

fun Fragment.openLink(link: String?) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    } catch (e: Exception) {
        Toaster(this).toast("Can't open ${link}", true)
    }
}

fun Context.openLink(link: String?) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    } catch (e: Exception) {
        Toaster(this).toast("Can't open ${link}", true)
    }
}

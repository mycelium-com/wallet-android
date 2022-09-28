package com.mycelium.wallet.external.partner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.external.changelly2.ExchangeFragment

fun Fragment.startContentLink(link: String?) {
    startContentLink(link) {
        startActivity(it)
    }
}

fun Activity.startContentLink(link: String?) {
    startContentLink(link) {
        startActivity(it)
    }
}

private fun startContentLink(link: String?, startAction: (Intent) -> Unit) {
    if (link != null) {
        try {
            if (link.startsWith("mycelium://action.")) {
                startAction(Intent(Uri.parse(link).host).apply {
                    setPackage(WalletApplication.getInstance().packageName)
                })
            } else {
                startAction(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}

fun Fragment.openLink(link: String?) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    } catch (e: ActivityNotFoundException) {
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

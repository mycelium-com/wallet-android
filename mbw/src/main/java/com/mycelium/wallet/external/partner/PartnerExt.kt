package com.mycelium.wallet.external.partner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

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
                startAction(Intent(Uri.parse(link).host))
            } else {
                startAction(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}




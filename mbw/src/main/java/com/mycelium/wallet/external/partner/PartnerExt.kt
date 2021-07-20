package com.mycelium.wallet.external.partner

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

fun Fragment.startContentLink(link: String?) {
    if (link != null) {
        try {
            if (link.startsWith("action.")) {
                startActivity(Intent(link))
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}

fun Activity.startContentLink(link: String?) {
    if (link != null) {
        try {
            if (link.startsWith("action.")) {
                startActivity(Intent(link))
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}




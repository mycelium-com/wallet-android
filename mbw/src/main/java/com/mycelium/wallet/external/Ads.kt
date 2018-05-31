package com.mycelium.wallet.external

import android.content.Context
import android.content.Intent
import android.net.Uri


object Ads {
    fun openApex(context: Context?) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.apextokenfund.com/?utm_source=mycelium&utm_medium=banner")))
    }

    fun openMydfs(context: Context?) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mydfs.net/?ref=mycelium")))
    }
}

package com.mycelium.wallet.external

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mycelium.wallet.R


object Ads {
    fun openFiopresale(context: Context?) {
        context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.partner_fiopresale_url))))
    }
}

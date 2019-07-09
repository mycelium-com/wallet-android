package com.mycelium.wallet.content.actions

import android.net.Uri
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action
import java.util.*


class WebsiteAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        if (!content.toLowerCase(Locale.US).startsWith("http")) {
            return false
        }

        val uri = Uri.parse(content)
        if (uri != null) {
            handlerActivity.finishOk(uri)
            return true
        }
        return false
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return content.toLowerCase().startsWith("http")
    }
}
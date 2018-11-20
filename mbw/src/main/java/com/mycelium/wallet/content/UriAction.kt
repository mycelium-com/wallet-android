package com.mycelium.wallet.content

import android.net.Uri
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StringHandlerActivity


class UriAction @JvmOverloads constructor(private val onlyWithAddress: Boolean = false) : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val manager = MbwManager.getInstance(handlerActivity)
        val uri = manager.contentResolver.resolveUri(content)
        if (uri != null) {
            if (onlyWithAddress && uri.address == null) {
                return false
            }
            handlerActivity.finishOk(uri)
        } else {
            handlerActivity.finishError(R.string.unrecognized_format)
            //started with bitcoin: but could not be parsed, was handled
        }
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return Uri.parse(content) != null
    }
}
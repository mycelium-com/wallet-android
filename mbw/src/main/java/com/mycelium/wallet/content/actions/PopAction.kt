package com.mycelium.wallet.content.actions

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action
import com.mycelium.wallet.pop.PopRequest


class PopAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        if (!isBtcpopURI(content)) {
            return false
        }
        val popRequest: PopRequest
        return try {
            popRequest = PopRequest(content)
            handlerActivity.finishOk(popRequest)
            true
        } catch (e: IllegalArgumentException) {
            handlerActivity.finishError(R.string.pop_invalid_pop_uri)
            false
        }
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isBtcpopURI(content)
    }

    private fun isBtcpopURI(content: String): Boolean {
        return content.startsWith("btcpop:")
    }
}
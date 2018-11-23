package com.mycelium.wallet.content.actions

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class AddressAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        handlerActivity.finishOk(content)
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return true
    }
}
package com.mycelium.wallet.content.actions

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class AddressAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val address = MbwManager.getInstance(handlerActivity).getWalletManager(false).parseAddress(content)
        return if(address.isNotEmpty()) {
            handlerActivity.finishOk(address[0])
            true
        } else {
            false
        }
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return true
    }
}
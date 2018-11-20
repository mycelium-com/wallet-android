package com.mycelium.wallet.content

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity


class HdNodeAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        try {
            val hdKey = HdKeyNode.parse(content, handlerActivity.network)
            handlerActivity.finishOk(hdKey)
            return true
        } catch (ex: HdKeyNode.KeyGenerationException) {
            return false
        }
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isKeyNode(network, content)
    }

    companion object {
        fun isKeyNode(network: NetworkParameters, content: String): Boolean {
            return try {
                HdKeyNode.parse(content, network)
                true
            } catch (ex: HdKeyNode.KeyGenerationException) {
                false
            }
        }
    }
}
package com.mycelium.wallet.content.actions

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class PrivateKeyAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val key = getPrivateKey(handlerActivity.network, content)
                ?: return false
        handlerActivity.finishOk(key)
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isPrivKey(network, content)
    }

    companion object {
        @JvmStatic
        fun getPrivateKey(network: NetworkParameters, content: String) =
                InMemoryPrivateKey.fromBase58String(content, network)
                        ?: InMemoryPrivateKey.fromBase58MiniFormat(content, network)

        private fun isPrivKey(network: NetworkParameters, content: String) =
                getPrivateKey(network, content) != null
    }
}
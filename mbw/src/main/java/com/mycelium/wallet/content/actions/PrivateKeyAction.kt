package com.mycelium.wallet.content.actions

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class PrivateKeyAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val key = getPrivateKey(handlerActivity.network, content)
        if (!key.isPresent) return false
        handlerActivity.finishOk(key.get())
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isPrivKey(network, content)
    }

    companion object {
        fun getPrivateKey(network: NetworkParameters, content: String): Optional<InMemoryPrivateKey> {
            var key = InMemoryPrivateKey.fromBase58String(content, network)
            if (key.isPresent) return key
            key = InMemoryPrivateKey.fromBase58MiniFormat(content, network)
            return if (key.isPresent) key else Optional.absent()

            //no match
        }

        private fun isPrivKey(network: NetworkParameters, content: String): Boolean {
            return getPrivateKey(network, content).isPresent
        }
    }
}
package com.mycelium.wallet.content.actions

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class MasterSeedAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        if (content.length % 2 != 0) {
            return false
        }
        try {
            val masterSeed = Bip39.MasterSeed.fromBytes(HexUtils.toBytes(content), false)
            if (masterSeed.isPresent) {
                handlerActivity.finishOk(masterSeed.get())
                return true
            }
        } catch (ignore: RuntimeException) {
        }
        return false
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isMasterSeed(content)
    }

    private fun isMasterSeed(content: String): Boolean {
        return try {
            val bytes = HexUtils.toBytes(content)
            Bip39.MasterSeed.fromBytes(bytes, false).isPresent
        } catch (ex: RuntimeException) {
            // HexUtils.toBytes will throw a RuntimeException if the string contains invalid characters
            false
        }
    }
}
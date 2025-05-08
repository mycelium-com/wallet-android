package com.mycelium.wallet.content.actions

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class WordListAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val words = content.split()
        if (!Bip39.isValidWordList(words)) {
            return false
        }
        handlerActivity.finishOk(words)
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean =
        Bip39.isValidWordList(content.split())

    private fun String.split() =
        this.split("[ ,;]".toRegex()).filter { it.isNotEmpty() }.toTypedArray()
}
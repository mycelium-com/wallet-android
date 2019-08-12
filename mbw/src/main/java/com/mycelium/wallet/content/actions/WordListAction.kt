package com.mycelium.wallet.content.actions

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.InstantMasterseedActivity
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class WordListAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        val words = content.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (!Bip39.isValidWordList(words)) {
            return false
        }
        InstantMasterseedActivity.callMe(handlerActivity, words, null)
        handlerActivity.finishOk()
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        val words = content.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return Bip39.isValidWordList(words)
    }
}
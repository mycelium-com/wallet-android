package com.mycelium.wallet.content

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.activity.StringHandlerActivity
import java.io.Serializable


interface Action : Serializable {
    /**
     * @return true if it was handled
     */
    fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean

    fun canHandle(network: NetworkParameters, content: String): Boolean
}

object NONE : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        return false
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return false
    }
}
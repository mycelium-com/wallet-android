package com.mycelium.wallet.content.actions

import com.mrd.bitlib.crypto.BipSss
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.content.Action


class SssShareAction : Action {
    override fun handle(handlerActivity: StringHandlerActivity, content: String): Boolean {
        if (!content.startsWith(BipSss.Share.SSS_PREFIX)) {
            return false
        }
        val share = BipSss.Share.fromString(content)
        if (null == share) {
            handlerActivity.finishError(R.string.error_invalid_sss_share)
        } else {
            handlerActivity.finishOk(share)
        }
        return true
    }

    override fun canHandle(network: NetworkParameters, content: String): Boolean {
        return isShare(content)
    }

    private fun isShare(content: String): Boolean {
        return content.startsWith(BipSss.Share.SSS_PREFIX)
    }
}
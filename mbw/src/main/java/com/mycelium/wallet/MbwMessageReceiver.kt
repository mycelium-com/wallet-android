package com.mycelium.wallet

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mycelium.modularizationtools.ModuleMessageReceiver
import com.mycelium.wallet.WalletApplication.getSpvModuleName
import com.mycelium.wapi.wallet.WalletAccount.Type.BCHBIP44

class MbwMessageReceiver constructor(private val context: Context) : ModuleMessageReceiver {
    override fun onMessage(callingPackageName: String, intent: Intent) {
        when (callingPackageName) {
            getSpvModuleName(BCHBIP44) -> onMessageFromSpvModuleBch(intent)
            else -> Log.e(TAG, "Ignoring unexpected package $callingPackageName calling with intent $intent.")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onMessageFromSpvModuleBch(intent: Intent) {
        TODO("implement")
    }

    companion object {
        private val TAG = MbwMessageReceiver::class.java.canonicalName
        @JvmStatic val TRANSACTION_NOTIFICATION_ID = -553794088
    }
}

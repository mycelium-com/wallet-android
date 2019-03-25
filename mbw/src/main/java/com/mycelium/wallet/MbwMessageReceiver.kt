package com.mycelium.wallet

import android.content.Context
import android.content.Intent
import android.util.Log
import com.mycelium.modularizationtools.ModuleMessageReceiver

class MbwMessageReceiver(private val context: Context) : ModuleMessageReceiver {
    override fun getIcon() = R.drawable.ic_launcher

    override fun onMessage(callingPackageName: String, intent: Intent) {
        Log.e(TAG, "Ignoring unexpected package $callingPackageName calling with intent $intent.")
    }

    companion object {
        private val TAG = MbwMessageReceiver::class.java.canonicalName
    }
}

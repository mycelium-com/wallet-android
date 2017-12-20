package com.mycelium.modularizationtools

import android.app.IntentService
import android.content.Intent
import android.util.Log

class MessageReceiver : IntentService("MessageReceiverThread") {
    private val LOG_TAG: String? = this.javaClass.canonicalName

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call [.stopSelf].

     * @param intent The value passed to [               ][android.content.Context.startService].
     * *               This may be null if the service is being restarted after
     * *               its process has gone away; see
     * *               [android.app.Service.onStartCommand]
     * *               for details.
     */
    override fun onHandleIntent(intent: Intent?) {
        if (null == intent || null == intent.action || !intent.hasExtra("key")) {
            Log.e(LOG_TAG, "onStartCommand failed: Intent was $intent")
            return
        }

        //Log.d(LOG_TAG, "onStartCommand: Intent is $intent")

        val key = intent.getLongExtra("key", 0)
        intent.removeExtra("key") // no need to share the key with other packages that might leak it
        val callerPackage: String
        try {
            // verify sender and get sending package name
            callerPackage = CommunicationManager.getInstance(this).getPackageName(key)
        } catch (e: SecurityException) {
            Log.e(LOG_TAG, "onStartCommand failed: ${e.message}")
            return
        }
        if(application !is ModuleMessageReceiver) {
            // TODO: The application should not be required to implement anything. This would probably be better solved with Annotations on the ModuleMessageReceiver class.
            throw Error("onStartCommand failed: The current Application does not implement ModuleMessageReceiver!")
        }
        val moduleMessageReceiver = application as ModuleMessageReceiver
        moduleMessageReceiver.onMessage(callerPackage, intent)
    }
}

interface ModuleMessageReceiver {
    fun onMessage(callingPackageName: String, intent: Intent)
}

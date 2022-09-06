package com.mycelium.wallet.fio

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity
import com.mycelium.wallet.fio.FioRequestNotificator.FIO_REQUEST_ACTION

class FioRequestService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
// TODO may be we need update fio request before move forward
        intent?.let {
            val activityClass =
                if (MbwManager.getInstance(applicationContext).isAppInForeground) ApproveFioRequestActivity::class.java
                else StartupActivity::class.java
            startActivity(
                Intent(applicationContext, activityClass)
                    .setAction(FIO_REQUEST_ACTION)
                    .putExtras(it)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return START_STICKY
    }
}
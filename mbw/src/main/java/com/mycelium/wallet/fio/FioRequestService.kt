package com.mycelium.wallet.fio

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity
import com.mycelium.wallet.fio.FioRequestNotificator.FIO_REQUEST_ACTION
import com.mycelium.wallet.fio.FioRequestNotificator.context


class FioRequestService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
// TODO may be we need update fio request before move forward
        startActivity(Intent(this,
                if (MbwManager.getInstance(context).isAppInForeground) ApproveFioRequestActivity::class.java else StartupActivity::class.java)
                .setAction(FIO_REQUEST_ACTION)
                .putExtras(intent)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return START_STICKY
    }
}
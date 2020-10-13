package com.mycelium.wallet.fio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioGroup
import com.mycelium.wapi.wallet.fio.getActiveFioAccounts
import com.squareup.otto.Subscribe
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent


object FioRequestNotificator {
    const val FIO_REQUEST_ACTION = "fio_request_action"
    private const val chanelId = "FIORequest"
    private const val fioRequestNotificationGroup = "FIO Requests"
    private const val fioRequestNotificationId = 24563487

    lateinit var context: Context
    lateinit var preferences: SharedPreferences


    @JvmStatic
    fun initialize(context: Context) {
        this.context = context
        preferences = context.getSharedPreferences("FioRequestNotificator", Context.MODE_PRIVATE)
        createNotificationChannel(context)
        MbwManager.getEventBus().register(this)
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        handleRequests()
    }

    private fun handleRequests() {
        notifyRequest(MbwManager.getInstance(context).getWalletManager(false)
                .getActiveFioAccounts()
                .asSequence()
                .map(FioAccount::getRequestsGroups)
                .flatten()
                .filter { it.status == FioGroup.Type.PENDING }
                .map { it.children }
                .flatten()
                .filter { !preferences.getBoolean(it.fioRequestId.toString(), false) }
                .toList())

    }

    private fun notifyRequest(requests: List<FIORequestContent>) {
        requests.forEach {
            NotificationManagerCompat.from(context).notify(fioRequestNotificationId + it.fioRequestId.toInt(),
                    createNotification(context)
                            .setContentTitle(context.getString(R.string.transaction_from_address_prefix, it.payerFioAddress))
                            .setContentText(it.deserializedContent?.memo)
                            .setTicker("asdfasdfsdf")
//                            .setContent(RemoteViews(context.packageName, R.layout.layout_fio_request_notification).apply {
//                                setTextViewText(R.id.fromFioName, context.getString(R.string.transaction_from_address_prefix, it.payerFioAddress))
//                                setTextViewText(R.id.amount, "Amount: ${it.deserializedContent?.amount}")
//                                setTextViewText(R.id.memo, it.deserializedContent?.memo)
//                            })
                            .setContentIntent(PendingIntent.getActivity(context, 0,
                                    createSingleFIORequestIntent(context, it), PendingIntent.FLAG_CANCEL_CURRENT))
                            .addAction(NotificationCompat.Action.Builder(null, "CONFIRM REQUEST",
                                    PendingIntent.getActivity(context, 0,
                                            createSingleFIORequestIntent(context, it), PendingIntent.FLAG_CANCEL_CURRENT))
                                    .build())
                            .setGroup(fioRequestNotificationGroup)
                            .build())
        }
    }

    private fun createSingleFIORequestIntent(context: Context, request: FIORequestContent): Intent {
        val clazz = if (MbwManager.getInstance(context).isAppInForeground) ApproveFioRequestActivity::class.java else StartupActivity::class.java
        return Intent(context, clazz)
                .setAction(FIO_REQUEST_ACTION)
                .putExtra(ApproveFioRequestActivity.CONTENT, request.toJson())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FIO Request"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(chanelId, name, importance).apply {
                description = name
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
        }
    }

    private fun createNotification(context: Context): NotificationCompat.Builder =
            NotificationCompat.Builder(context, chanelId)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
}
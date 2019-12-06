package com.mycelium.wallet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


object WorkerUtils {
    private val TAG = WorkerUtils::class.java.simpleName

    private const val VERBOSE_NOTIFICATION_CHANNEL_NAME = "Verbose WorkManager Notifications"
    private const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
    private const val NOTIFICATION_TITLE = "Update Starting"
    private const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
    private const val NOTIFICATION_ID = 1

    val DELAY_TIME_MILLIS: Long = 3000


    fun makeStatusNotification(message: String, context: Context) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
            val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(LongArray(0))

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    fun hide(context: Context){
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun sleep() {
        try {
            Thread.sleep(DELAY_TIME_MILLIS, 0)
        } catch (e: InterruptedException) {
            Log.d(TAG, e.message)
        }

    }
}
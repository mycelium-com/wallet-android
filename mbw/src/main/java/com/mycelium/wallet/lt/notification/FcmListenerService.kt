package com.mycelium.wallet.lt.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mycelium.lt.api.LtApi
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.ActionActivity
import com.mycelium.wallet.activity.PinProtectedActivity
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.settings.SettingsPreference.mediaFlowEnabled
import com.mycelium.wallet.checkPushPermission
import com.mycelium.wallet.external.mediaflow.NewsSyncUtils.handle
import com.mycelium.wallet.lt.activity.LtMainActivity

class FcmListenerService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        MbwManager.getInstance(this).localTraderManager.storeGcmRegistrationId(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data: Map<*, *> = remoteMessage.data
        val messageType = remoteMessage.messageType // null for firebase
        val key = remoteMessage.collapseKey
        Log.d(TAG, "Message type: $messageType")
        Log.d(TAG, "Message key: $key")
        Log.d(TAG, "Message data: $data")

        // Check if message contains a data payload.
        if (MEDIA_TOPIC.equals(remoteMessage.from, ignoreCase = true)
                && mediaFlowEnabled) {
            handle(this, remoteMessage)
        } else if (data.isNotEmpty() && key != null) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            when {
                LtApi.TRADE_ACTIVITY_NOTIFICATION_KEY == key -> {
                    handleTradeActivityNotification(data)
                }
                LtApi.AD_ACTIVITY_NOTIFICATION_KEY == key -> {
                    handleAdActivityNotification(data)
                }
                TYPE_ADS_NOTIFICATION == data["type"] -> {
                    showNotification(remoteMessage)
                }
            }
        } else {
            Log.d(TAG, "empty message received, ignoring")
        }
    }

    fun showNotification(remoteMessage: RemoteMessage) {
        val link = remoteMessage.data["action"] ?: ""
        createNotificationChannel(this, TYPE_ADS_NOTIFICATION, "Advertise messages")
        val builder = NotificationCompat.Builder(this, TYPE_ADS_NOTIFICATION)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(remoteMessage.notification?.title)
                .setContentText(remoteMessage.notification?.body)
                .apply {
                    if (link.startsWith("mycelium://action.")) {
                        setContentIntent(PendingIntent.getActivity(this@FcmListenerService, 0,
                                Intent(this@FcmListenerService, ActionActivity::class.java).apply {
                                    setPackage(WalletApplication.getInstance().packageName)
                                    putExtra("action", link)
                                }, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    } else if (link.isNotEmpty()) {
                        setContentIntent(PendingIntent.getActivity(this@FcmListenerService, 0,
                                Intent(Intent.ACTION_VIEW, Uri.parse(link)),
                                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    }
                }
        val image = remoteMessage.toIntent().getStringExtra("gcm.notification.image")
        if (image?.isNotEmpty() == true) {
            Handler(Looper.getMainLooper()).post {
                Glide.with(applicationContext)
                        .asBitmap()
                        .load(image)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                builder.setStyle(NotificationCompat.BigPictureStyle()
                                        .setBigContentTitle(remoteMessage.notification?.title)
                                        .bigPicture(resource))
                                this@FcmListenerService.checkPushPermission({
                                    NotificationManagerCompat.from(this@FcmListenerService)
                                        .notify(ID_ADS_NOTIFICATION, builder.build())
                                })

                            }
                        })
            }
        } else {
            this@FcmListenerService.checkPushPermission({
                NotificationManagerCompat.from(this).notify(ID_ADS_NOTIFICATION, builder.build())
            })
        }
    }

    private fun handleTradeActivityNotification(data: Map<*, *>) {
        val ltManager = MbwManager.getInstance(this).localTraderManager
        if (!ltManager.isLocalTraderEnabled || !ltManager.hasLocalTraderAccount()) {
            Log.d(TAG, "Local trader not enabled while getting trader activity notification")
            return
        }
        val trader = data["trader"].toString()
        if (ltManager.localTraderAddress.toString() != trader) {
            Log.d(TAG, "Local trader received notification for a different trader than the currently active")
            return
        }
        val lastChange = try {
            val lastChangeString = data["lastChange"].toString()
            if (lastChangeString == null) {
                Log.d(TAG, "Local trader received notification without lastChange")
                return
            }
            lastChangeString.toLong()
        } catch (e: NumberFormatException) {
            Log.d(TAG, "Local trader received notification invalid lastChange")
            return
        }
        if (lastChange == 0L) {
            Log.d(TAG, "Local trader last change is zero")
            return
        }
        val type = data["type"].toString()

        // Let local trader know what the latest trader change timestamp is
        if (ltManager.setLastTraderNotification(lastChange) && ltManager.areNotificationsEnabled()) {
            // We got GC notification that was news to us, make a notification
            showTradeNotification(type, lastChange)
        }
    }

    private fun handleAdActivityNotification(data: Map<*, *>) {
        val ltManager = MbwManager.getInstance(this).localTraderManager
        if (!ltManager.isLocalTraderEnabled || !ltManager.hasLocalTraderAccount()) {
            Log.d(TAG, "Local trader not enabled while getting trader activity notification")
            return
        }
        val trader = data["trader"].toString()
        if (ltManager.localTraderAddress.toString() != trader) {
            Log.d(TAG, "Local trader received notification for a different trader than the currently active")
            return
        }
        val type = data["type"].toString()
        if (ltManager.areNotificationsEnabled()) {
            showAdNotification(type)
        }
    }

    private fun showTradeNotification(type: String, lastChange: Long) {
        createNotificationChannel(this, LT_CHANNEL_ID, "Trade messages")
        val intent = if (LtApi.TRADE_FINAL_NOTIFICATION_TYPE == type) {
            LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.TRADE_HISTORY)
        } else {
            LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.ACTIVE_TRADES)
        }
        val pinProtectedIntent = PinProtectedActivity.createIntent(this, intent)

        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntent(Intent(this, ModernMain::class.java))
        stackBuilder.addNextIntent(pinProtectedIntent)
        val pIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = resources.getString(R.string.lt_mycelium_local_trader_title)
        val message = resources.getString(R.string.lt_new_trading_activity_message)
        val builder = NotificationCompat.Builder(this, LT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true)

        // Add ticker
        builder.setTicker(message)

        // Tell other listeners that we have taken care of audibly notifying up
        // till this timestamp
        val ltManager = MbwManager.getInstance(this).localTraderManager
        ltManager.lastNotificationSoundTimestamp = lastChange

        // Vibrate
        val pattern = longArrayOf(500, 500)
        builder.setVibrate(pattern)

        // Make a sound
        if (ltManager.playSoundOnTradeNotification) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alarmSound != null) {
                builder.setSound(alarmSound)
            }
        }

        // Notify
        this.checkPushPermission({
            NotificationManagerCompat.from(this).notify(ID_TRADE_NOTIFICATION, builder.build())
        })
    }

    private fun showAdNotification(type: String) {
        createNotificationChannel(this, LT_CHANNEL_ID, "Trade messages")
        val intent = if (LtApi.AD_TIME_OUT_NOTIFICATION_TYPE == type) {
            PinProtectedActivity.createIntent(this,
                    LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.MY_ADS)
            )
        } else {
            // We don't know this type, so we ignore it
            return
        }
        val pIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = resources.getString(R.string.lt_mycelium_local_trader_title)
        val message = resources.getString(R.string.lt_ad_deactivating_message)
        val builder = NotificationCompat.Builder(this, LT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true)

        // Add ticker
        builder.setTicker(message)
        val ltManager = MbwManager.getInstance(this).localTraderManager

        // Vibrate
        val pattern = longArrayOf(500, 500)
        builder.setVibrate(pattern)

        // Make a sound
        if (ltManager.playSoundOnTradeNotification) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alarmSound != null) {
                builder.setSound(alarmSound)
            }
        }

        // Notify
        this.checkPushPermission({
            NotificationManagerCompat.from(this).notify(ID_TRADE_AD_ACTIVITY_NOTIFICATION, builder.build())
        })
    }

    companion object {
        private fun createNotificationChannel(context: Context, channelId: String, name: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(channelId, name, importance).apply {
                    description = name
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private const val TAG = "firebasenotificationlog"
        private const val LT_CHANNEL_ID = "LT notification channel"
        const val MEDIA_TOPIC = "/topics/all"
        private const val TYPE_ADS_NOTIFICATION = "advertise"
        private const val ID_ADS_NOTIFICATION = 726463
        const val ID_TRADE_NOTIFICATION = 1726460
        const val ID_TRADE_AD_ACTIVITY_NOTIFICATION = 1726461
    }
}
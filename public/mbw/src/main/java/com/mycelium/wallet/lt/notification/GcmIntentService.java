package com.mycelium.wallet.lt.notification;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mycelium.lt.api.LtApi;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.activity.LtMainActivity;

public class GcmIntentService extends IntentService {

   private static final String TAG = "GcmIntentService";

   public GcmIntentService() {
      super("GcmIntentService");
   }

   @Override
   protected void onHandleIntent(Intent intent) {
      Bundle extras = intent.getExtras();
      GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
      // The getMessageType() intent parameter must be the intent you received
      // in your BroadcastReceiver.
      String messageType = gcm.getMessageType(intent);

      if (!extras.isEmpty()) { // has effect of unparcelling Bundle
         /*
          * Filter messages based on message type. Since it is likely that GCM
          * will be extended in the future with new message types, just ignore
          * any message types you're not interested in, or that you don't
          * recognize.
          */
         if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            Log.i(TAG, "SEND ERROR: " + extras.toString());
         } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            Log.i(TAG, "DELETED: " + extras.toString());
         } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            Log.i(TAG, "Received: " + extras.toString());
            if (LtApi.TRADE_ACTIVITY_NOTIFICATION_KEY.equals(extras.getString("collapse_key"))) {
               handleTradeActivityNotification(extras);
            }
         }
      }
      // Release the wake lock provided by the WakefulBroadcastReceiver.
      GcmBroadcastReceiver.completeWakefulIntent(intent);
   }

   private void handleTradeActivityNotification(Bundle extras) {
      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();
      if (ltManager.isLocalTraderDisabled() || !ltManager.hasLocalTraderAccount()) {
         Log.w(TAG, "Local trader not enabled while getting trader activity notification");
         return;
      }
      String trader = extras.getString("trader");
      if (!ltManager.getLocalTraderAddress().toString().equals(trader)) {
         Log.w(TAG, "Local trader received notification for a different trader than the currently active");
         return;
      }
      long lastChange;
      try {
         String lastChangeString = extras.getString("lastChange");
         if (lastChangeString == null) {
            Log.w(TAG, "Local trader received notification without lastChange");
            return;
         }
         lastChange = Long.parseLong(lastChangeString);
      } catch (NumberFormatException e) {
         Log.w(TAG, "Local trader received notification invalid lastChange");
         return;
      }
      if (lastChange == 0) {
         Log.w(TAG, "Local trader last change is zero");
         return;
      }
      String type = extras.getString("type");

      // Let local trader know what the latest trader change timestamp is
      if (ltManager.setLastTraderNotification(lastChange) && ltManager.areNotificationsEnabled()) {
         // We got GC notification that was news to us, make a notification
         showNotification(type);
      }
   }

   private void showNotification(String type) {
      Intent intent;
      if (LtApi.TRADE_FINAL_NOTIFICATION_TYPE.equals(type)) {
         intent = LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.TRADE_HISTORY);
      } else {
         intent = LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.ACTIVE_TRADES);
      }
      PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
      String title = getResources().getString(R.string.lt_mycelium_local_trader_title);
      String message = getResources().getString(R.string.lt_new_trading_activity_message);

      NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle(title)
            .setContentText(message).setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent).setAutoCancel(true);

      // Add ticker
      builder.setTicker(message);

      // Vibrate
      long[] pattern = { 500, 500 };
      builder.setVibrate(pattern);

      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();

      // Make a sound
      if (ltManager.playSounfOnTradeNotification()) {
         Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
         if (alarmSound != null) {
            builder.setSound(alarmSound);
         }
      }

      // Notify
      NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      notificationManager.notify(0, builder.build());
   }

}

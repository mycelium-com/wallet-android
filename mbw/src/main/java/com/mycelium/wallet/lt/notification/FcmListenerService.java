/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.lt.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mycelium.lt.api.LtApi;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.PinProtectedActivity;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.activity.LtMainActivity;

import java.util.Map;

public class FcmListenerService extends FirebaseMessagingService {
   private static final String TAG = "firebasenotificationlog";
   private static final String LT_CHANNEL_ID = "LT notification channel";

   @Override
   public void onMessageReceived(RemoteMessage remoteMessage) {
      Map data = remoteMessage.getData();
      String messageType = remoteMessage.getMessageType();  // null for firebase
      String key = remoteMessage.getCollapseKey();

      Log.d(TAG, "Message type: " + messageType);
      Log.d(TAG, "Message key: " + key);
      Log.d(TAG, "Message data: " + data);

      // Check if message contains a data payload.
      if (data.size() > 0 && key != null) {
         Log.d(TAG, "Message data payload: " + remoteMessage.getData());

//         if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
//            Log.d(TAG, "DELETED: " + data.toString());
//         } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
//            Log.d(TAG, "DELETED: " + data.toString());
//         } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            if (LtApi.TRADE_ACTIVITY_NOTIFICATION_KEY.equals(key)) {
               handleTradeActivityNotification(data);
            } else if (LtApi.AD_ACTIVITY_NOTIFICATION_KEY.equals(key)) {
               handleAdActivityNotification(data);
            }
//         }
      } else {
         Log.d(TAG, "empty message received, ignoring");
      }
   }

   private void handleTradeActivityNotification(Map data) {
      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();
      if (!ltManager.isLocalTraderEnabled() || !ltManager.hasLocalTraderAccount()) {
         Log.d(TAG, "Local trader not enabled while getting trader activity notification");
         return;
      }
      String trader = String.valueOf(data.get("trader"));
      if (!ltManager.getLocalTraderAddress().toString().equals(trader)) {
         Log.d(TAG, "Local trader received notification for a different trader than the currently active");
         return;
      }
      long lastChange;
      try {
         String lastChangeString = String.valueOf(data.get("lastChange"));
         if (lastChangeString == null) {
            Log.d(TAG, "Local trader received notification without lastChange");
            return;
         }
         lastChange = Long.parseLong(lastChangeString);
      } catch (NumberFormatException e) {
         Log.d(TAG, "Local trader received notification invalid lastChange");
         return;
      }
      if (lastChange == 0) {
         Log.d(TAG, "Local trader last change is zero");
         return;
      }
      String type = String.valueOf(data.get("type"));

      // Let local trader know what the latest trader change timestamp is
      if (ltManager.setLastTraderNotification(lastChange) && ltManager.areNotificationsEnabled()) {
         // We got GC notification that was news to us, make a notification
         showTradeNotification(type, lastChange);
      }
   }

   private void handleAdActivityNotification(Map data) {
      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();
      if (!ltManager.isLocalTraderEnabled() || !ltManager.hasLocalTraderAccount()) {
         Log.d(TAG, "Local trader not enabled while getting trader activity notification");
         return;
      }
      String trader = String.valueOf(data.get("trader"));
      if (!ltManager.getLocalTraderAddress().toString().equals(trader)) {
         Log.d(TAG, "Local trader received notification for a different trader than the currently active");
         return;
      }
      String type = String.valueOf(data.get("type"));

      if (ltManager.areNotificationsEnabled()) {
         showAdNotification(type);
      }
   }

   private void showTradeNotification(String type, long lastChange) {
      Intent intent;
      if (LtApi.TRADE_FINAL_NOTIFICATION_TYPE.equals(type)) {
         intent = LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.TRADE_HISTORY);
      } else {
         intent = LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.ACTIVE_TRADES);
      }

      Intent pinProtectedIntent = PinProtectedActivity.createIntent(this, intent);

      PendingIntent pIntent = PendingIntent.getActivity(this, 0, pinProtectedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
      String title = getResources().getString(R.string.lt_mycelium_local_trader_title);
      String message = getResources().getString(R.string.lt_new_trading_activity_message);

      NotificationCompat.Builder builder = new NotificationCompat
              .Builder(this, LT_CHANNEL_ID)
              .setContentTitle(title)
              .setContentText(message)
              .setSmallIcon(R.drawable.ic_launcher)
              .setContentIntent(pIntent)
              .setAutoCancel(true);

      // Add ticker
      builder.setTicker(message);

      // Tell other listeners that we have taken care of audibly notifying up
      // till this timestamp
      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();
      ltManager.setLastNotificationSoundTimestamp(lastChange);

      // Vibrate
      long[] pattern = {500, 500};
      builder.setVibrate(pattern);

      // Make a sound
      if (ltManager.getPlaySoundOnTradeNotification()) {
         Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
         if (alarmSound != null) {
            builder.setSound(alarmSound);
         }
      }

      // Notify
      NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      notificationManager.notify(0, builder.build());
   }

   private void showAdNotification(String type) {
      Intent intent;
      if (LtApi.AD_TIME_OUT_NOTIFICATION_TYPE.equals(type)) {
         intent = PinProtectedActivity.createIntent(this,
                 LtMainActivity.createIntent(this, LtMainActivity.TAB_TYPE.MY_ADS)
         );
      } else {
         // We don't know this type, so we ignore it
         return;
      }
      PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
      String title = getResources().getString(R.string.lt_mycelium_local_trader_title);
      String message = getResources().getString(R.string.lt_ad_deactivating_message);

      NotificationCompat.Builder builder = new NotificationCompat
              .Builder(this, LT_CHANNEL_ID)
              .setContentTitle(title)
              .setContentText(message)
              .setSmallIcon(R.drawable.ic_launcher)
              .setContentIntent(pIntent)
              .setAutoCancel(true);

      // Add ticker
      builder.setTicker(message);

      LocalTraderManager ltManager = MbwManager.getInstance(this).getLocalTraderManager();

      // Vibrate
      long[] pattern = {500, 500};
      builder.setVibrate(pattern);

      // Make a sound
      if (ltManager.getPlaySoundOnTradeNotification()) {
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

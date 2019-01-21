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

package com.mycelium.wallet.lt;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mycelium.lt.ApiUtils;
import com.mycelium.lt.ChatMessageEncryptionKey;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.LtSession;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.GpsLocationFetcher.GpsLocationEx;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.lt.api.CreateAd;
import com.mycelium.wallet.lt.api.CreateTrade;
import com.mycelium.wallet.lt.api.Request;
import com.mycelium.wallet.persistence.TradeSessionDb;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LocalTraderManager {

   public static final String GCM_SENDER_ID = "1025080855849";

   private static final String TAG = "LocalTraderManager";
   public static final String LT_DERIVATION_SEED = "lt.mycelium.com";

   final private Context context;
   final private TradeSessionDb db;
   final private LtApi api;
   final private MbwManager mbwManager;
   final private Set<LocalTraderEventSubscriber> subscribers;
   private LtSession session;
   final private List<Request> requestList;
   private boolean isLoggedIn;
   private Address localTraderAddress;
   private long lastTraderSynchronization;
   private long lastTraderNotification;
   private GpsLocationEx currentLocation;
   private String nickname;
   private boolean localTraderEnabled;
   private boolean playSoundOnTradeNotification;
   private boolean usemiles;
   private TraderChangeMonitor traderChangeMonitor;
   private TradeSessionChangeMonitor tradeSessionChangeMonitor;
   private boolean notificationsEnabled;
   private TraderInfo cachedTraderInfo;
   private long lastNotificationSoundTimestamp;
   private String localTraderPrivateKeyString;
   private UUID localTraderAccountId;
   private InMemoryPrivateKey localTraderPrivateKey;

   public LocalTraderManager(Context context, TradeSessionDb db, LtApi api, MbwManager mbwManager) {
      notificationsEnabled = true;
      this.context = context;
      this.db = db;
      this.api = api;
      this.mbwManager = mbwManager;
      subscribers = new HashSet<>();
      requestList = new LinkedList<>();

      // Preferences
      SharedPreferences preferences = this.context.getSharedPreferences(Constants.LOCAL_TRADER_SETTINGS_NAME,
            Activity.MODE_PRIVATE);

      // Nick name
      nickname = preferences.getString(Constants.LOCAL_TRADER_NICKNAME_SETTING, null);

      // Address
      String addressString = preferences.getString(Constants.LOCAL_TRADER_ADDRESS_SETTING, null);
      if (addressString != null) {
         localTraderAddress = Address.fromString(addressString, this.mbwManager.getNetwork());
         // May be null
      }
      // Private key, may be null even if we have an address. This happens in the upgrade scenario where it is set later
      localTraderPrivateKeyString = preferences.getString(Constants.LOCAL_TRADER_KEY_SETTING, null);
      // Account ID, may be null even if we have an address. This happens in the upgrade scenario where it is set later
      String localTraderAccountIdString = preferences.getString(Constants.LOCAL_TRADER_ACCOUNT_ID_SETTING, null); // May be null
      if (localTraderAccountIdString != null) {
         localTraderAccountId = UUID.fromString(localTraderAccountIdString);
      }

      // Load location from preferences or use default
      currentLocation = new GpsLocationEx(
            preferences.getFloat(Constants.LOCAL_TRADER_LATITUDE_SETTING, (float) Constants.LOCAL_TRADER_DEFAULT_LOCATION.latitude),
            preferences.getFloat(Constants.LOCAL_TRADER_LONGITUDE_SETTING, (float) Constants.LOCAL_TRADER_DEFAULT_LOCATION.longitude),
            preferences.getString(Constants.LOCAL_TRADER_LOCATION_NAME_SETTING, Constants.LOCAL_TRADER_DEFAULT_LOCATION.name),
            preferences.getString(Constants.LOCAL_TRADER_LOCATION_COUNTRY_CODE_SETTING, Constants.LOCAL_TRADER_DEFAULT_LOCATION.name));

      localTraderEnabled = preferences.getBoolean(Constants.LT_ENABLED, !preferences.getBoolean(Constants.LT_DISABLED, false));
      playSoundOnTradeNotification = preferences.getBoolean(Constants.LOCAL_TRADER_PLAY_SOUND_ON_TRADE_NOTIFICATION_SETTING, true);
      usemiles = preferences.getBoolean(Constants.LOCAL_TRADER_USE_MILES_SETTING, false);
      lastTraderSynchronization = preferences.getLong(Constants.LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING, 0);
      lastTraderNotification = preferences.getLong(Constants.LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING, 0);

      Thread thread = new Thread(new Executor());
      thread.setDaemon(true);
      thread.start();

      traderChangeMonitor = new TraderChangeMonitor(this, this.api);
      tradeSessionChangeMonitor = new TradeSessionChangeMonitor(this, this.api);
   }

   public void subscribe(LocalTraderEventSubscriber listener) {
      synchronized (subscribers) {
         subscribers.add(listener);
         if (subscribers.size() > 5) {
            Log.w("LocalTraderManager", "subscriber size seems large: " + subscribers.size());
         }
      }
   }

   public void unsubscribe(LocalTraderEventSubscriber listener) {
      synchronized (subscribers) {
         boolean removed = subscribers.remove(listener);
         if (!removed) {
            Log.e("LocalTraderManager", "SUBSCRIBER NOT REMOVED");
         }
      }
   }

   public void makeRequest(Request request) {
      if (request.requiresLogin() && !hasLocalTraderAccount()) {
         throw new RuntimeException("Cannot make login request when trading is disabled");
      }
      synchronized (requestList) {
         requestList.add(request);
         requestList.notify();
      }
   }

   public void startMonitoringTrader() {
      traderChangeMonitor.startMonitoring();
   }

   public void stopMonitoringTrader() {
      traderChangeMonitor.stopMonitoring();
   }

   public void startMonitoringTradeSession(TradeSessionChangeMonitor.Listener listener) {
      if (session == null) {
         Log.e(TAG, "Trying to monitor trade session without having a session");
         return;
      }
      tradeSessionChangeMonitor.startMonitoring(session.id, listener);
   }

   public void stopMonitoringTradeSession() {
      tradeSessionChangeMonitor.stopMonitoring();
   }

   public void enableNotifications(boolean enabled) {
      notificationsEnabled = enabled;
   }

   public boolean areNotificationsEnabled() {
      return notificationsEnabled;
   }

   public UUID getLocalTraderAccountId() {
      return localTraderAccountId;
   }

   public interface LocalManagerApiContext {
      void handleErrors(Request request, int errorCode);

      void updateLocalTradeSessions(Collection<TradeSession> collection);

      void updateSingleTradeSession(TradeSession tradeSession);

      void cacheTraderInfo(TraderInfo traderInfo);

      void unsetLocalTraderAccount();
   }

   private class Executor implements Runnable, LocalManagerApiContext {
      @Override
      public void run() {
         String currentSessionLanguage = null;

         while (true) {

            // Grab a request or wait
            Request request;
            synchronized (requestList) {
               if (requestList.size() == 0) {
                  try {
                     requestList.wait();
                  } catch (InterruptedException e) {
                     break;
                  }
               }
               request = requestList.remove(0);
            }

            // If the request requires a session and we don't got one or if the
            // language was changed, get a session
            if (request.requiresSession()) {
               if (session == null || !mbwManager.getLanguage().equals(currentSessionLanguage)) {
                  if (renewSession()) {
                     currentSessionLanguage = mbwManager.getLanguage();
                  } else {
                     continue;
                  }
               }
            }

            // If the request requires a login and we don't are not logged in,
            // login
            if (request.requiresLogin() && !isLoggedIn) {
               if (!login()) {
                  continue;
               }
            }
            request.execute(this, api, session.id, subscribers);
         }
      }

      private boolean renewSession() {
         try {
            // Get new session
            session = api.createSession(LtApi.VERSION, mbwManager.getLanguage(),
                  mbwManager.getBitcoinDenomination().getAsciiName()).getResult();
            isLoggedIn = false;
            return true;
         } catch (LtApiException e) {
            // Handle errors
            handleErrors(null, e.errorCode);
            return false;
         }
      }

      private boolean login() {
         checkNotNull(session.id);
         // Sign session ID with private key
         InMemoryPrivateKey privateKey = getLocalTraderPrivateKey();
         if (privateKey == null) {
            handleErrors(null, LtApi.ERROR_CODE_TRADER_DOES_NOT_EXIST);
            return false;
         }
         String sigHashSessionId = ApiUtils.generateUuidHashSignature(privateKey, session.id
         );
         try {
            // Login
            LoginParameters params = new LoginParameters(getLocalTraderAddress(), sigHashSessionId);
            params.setGcmId(getGcmRegistrationId());
            api.traderLogin(session.id, params).getResult();
            isLoggedIn = true;
            return true;
         } catch (LtApiException e) {
            if (e.errorCode == LtApi.ERROR_CODE_INVALID_SESSION) {
               return renewSession() && login();
            } else {
               handleErrors(null, e.errorCode);
               return false;
            }
         }
      }

      public void updateLocalTradeSessions(Collection<TradeSession> collection) {
         LocalTraderManager.this.updateLocalTradeSessions(collection);
      }

      public void unsetLocalTraderAccount() {
         LocalTraderManager.this.unsetLocalTraderAccount();
      }

      public void updateSingleTradeSession(TradeSession tradeSession) {
         LocalTraderManager.this.updateSingleTradeSession(tradeSession);
      }

      @Override
      public void cacheTraderInfo(TraderInfo traderInfo) {
         LocalTraderManager.this.cacheTraderInfo(traderInfo);
      }

      public void handleErrors(Request request, int errorCode) {
         switch (errorCode) {
            case LtApi.ERROR_CODE_INVALID_SESSION:
               if (renewSession()) {
                  if (login()) {
                     synchronized (requestList) {
                        requestList.add(request);
                        requestList.notify();
                     }
                  }
               }
               break;
            case LtApi.ERROR_CODE_NO_SERVER_CONNECTION:
               notifyNoConnection(errorCode);
               break;
            case LtApi.ERROR_CODE_INCOMPATIBLE_API_VERSION:
               notifyIncompatibleApiVersion(errorCode);
               break;
            case LtApi.ERROR_CODE_TRADER_DOES_NOT_EXIST:
               isLoggedIn = false;
               session = null;
               // Disconnect trader account
               unsetLocalTraderAccount();
               notifyNoTraderAccount(errorCode);
               break;
            default:
               isLoggedIn = false;
               session = null;
               notifyError(errorCode);
               break;
         }
      }
   }

   private void notifyNoConnection(final int errorCode) {
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  if (!s.onNoLtConnection()) {
                     s.onLtError(errorCode);
                  }
               }
            });
         }
      }
   }

   private void notifyIncompatibleApiVersion(final int errorCode) {
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  if (!s.onLtNoIncompatibleVersion()) {
                     s.onLtError(errorCode);
                  }
               }
            });
         }
      }
   }

   private void notifyNoTraderAccount(final int errorCode) {
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  if (!s.onLtNoTraderAccount()) {
                     s.onLtError(errorCode);
                  }
               }
            });
         }
      }
   }

   private void notifyError(final int errorCode) {
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  s.onLtError(errorCode);
               }
            });
         }
      }
   }

   private void notifyTraderActivity(final long timestamp) {
      synchronized (subscribers) {
         for (final LocalTraderEventSubscriber s : subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  s.onLtTraderActicityNotification(timestamp);
               }
            });
         }
      }
   }

   /**
    * May return null
    */
   public synchronized TradeSession getLocalTradeSession(UUID tradeSessionId) {
      return db.get(tradeSessionId);
   }

   public synchronized Collection<TradeSession> getLocalTradeSessions() {
      return db.getAll();
   }

   public synchronized Collection<TradeSession> getLocalBuyTradeSessions() {
      return db.getBuyTradeSessions();
   }

   public synchronized Collection<TradeSession> getLocalSellTradeSessions() {
      return db.getSellTradeSessions();
   }

   public synchronized int countLocalTradeSessions() {
      return db.countTradeSessions();
   }

   public synchronized int countLocalBuyTradeSessions() {
      return db.countBuyTradeSessions();
   }

   public synchronized int countLocalSellTradeSessions() {
      return db.countSellTradeSessions();
   }

   public synchronized boolean isViewed(TradeSession tradeSession) {
      return db.getViewTimeById(tradeSession.id) >= tradeSession.lastChange;
   }

   public synchronized void markViewed(TradeSession tradeSession) {
      db.markViewed(tradeSession);
   }

   private synchronized void updateLocalTradeSessions(Collection<TradeSession> remoteList) {
      // Get all the local sessions
      Collection<TradeSession> localList = db.getAll();

      // Iterate over local items to find records to delete or update locally
      for (TradeSession localItem : localList) {
         TradeSession remoteItem = findAndEliminate(localItem, remoteList);
         if (remoteItem == null) {
            // A local item is not in the remote list, remove it locally
            db.delete(localItem.id);
         } else {
            // A local item is in the new list, see if it needs to be updated
            if (needsUpdate(localItem, remoteItem)) {
               db.update(remoteItem);
            }
         }
      }

      // Iterate over remaining remote items and insert them
      for (TradeSession remoteItem : remoteList) {
         db.insert(remoteItem);
      }
   }

   private synchronized void updateSingleTradeSession(TradeSession item) {
      db.insert(item);
   }

   public void cacheTraderInfo(TraderInfo traderInfo) {
      cachedTraderInfo = traderInfo;
   }

   public TraderInfo getCachedTraderInfo() {
      return cachedTraderInfo;
   }

   private TradeSession findAndEliminate(TradeSession item, Collection<TradeSession> list) {
      Iterator<TradeSession> it = list.iterator();
      while (it.hasNext()) {
         TradeSession t = it.next();
         if (t.equals(item)) {
            it.remove();
            return t;
         }
      }
      return null;
   }

   private boolean needsUpdate(TradeSession oldValue, TradeSession newValue) {
      checkArgument(oldValue.id.equals(newValue.id));
      return oldValue.lastChange < newValue.lastChange;
   }

   private SharedPreferences.Editor getEditor() {
      return context.getSharedPreferences(Constants.LOCAL_TRADER_SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
   }

   public boolean hasLocalTraderAccount() {
      return getLocalTraderPrivateKey() != null;
   }

   public String getNickname() {
      return nickname;
   }

   public Address getLocalTraderAddress() {
      return localTraderAddress;
   }

   private InMemoryPrivateKey getLocalTraderPrivateKey() {
      if (localTraderPrivateKeyString == null) {
         return null;
      }
      if (localTraderPrivateKey == null) {
         localTraderPrivateKey = new InMemoryPrivateKey(localTraderPrivateKeyString, mbwManager.getNetwork());
      }
      return localTraderPrivateKey;
   }

   public ChatMessageEncryptionKey generateChatMessageEncryptionKey(PublicKey foreignPublicKey, UUID tradeSessionId) {
      InMemoryPrivateKey myPrivateKey = checkNotNull(getLocalTraderPrivateKey());
      return ChatMessageEncryptionKey.fromEcdh(foreignPublicKey, myPrivateKey, tradeSessionId);
   }

   public void unsetLocalTraderAccount() {
      session = null;
      localTraderAddress = null;
      localTraderAccountId = null;
      localTraderPrivateKey = null;
      localTraderPrivateKeyString = null;
      nickname = null;
      setLastTraderSynchronization(0);
      db.deleteAll();
      getEditor()
              .remove(Constants.LOCAL_TRADER_KEY_SETTING)
              .remove(Constants.LOCAL_TRADER_ACCOUNT_ID_SETTING)
              .remove(Constants.LOCAL_TRADER_ADDRESS_SETTING)
              .remove(Constants.LOCAL_TRADER_NICKNAME_SETTING)
              .apply();
   }

   public void setLocalTraderData(UUID accountId, InMemoryPrivateKey privateKey, Address address, String nickname) {
      session = null;
      localTraderAddress = checkNotNull(address);
      localTraderAccountId = checkNotNull(accountId);
      localTraderPrivateKey = checkNotNull(privateKey);
      localTraderPrivateKeyString = privateKey.getBase58EncodedPrivateKey(mbwManager.getNetwork());
      this.nickname = checkNotNull(nickname);
      getEditor()
            .putString(Constants.LOCAL_TRADER_KEY_SETTING, localTraderPrivateKeyString)
            .putString(Constants.LOCAL_TRADER_ACCOUNT_ID_SETTING, accountId.toString())
            .putString(Constants.LOCAL_TRADER_ADDRESS_SETTING, address.toString())
            .putString(Constants.LOCAL_TRADER_NICKNAME_SETTING, nickname)
            .apply();
   }

   public synchronized void setLastTraderSynchronization(long timestamp) {
      lastTraderSynchronization = timestamp;
      getEditor()
              .putLong(Constants.LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING, timestamp)
              .apply();
   }

   public synchronized long getLastTraderSynchronization() {
      return lastTraderSynchronization;
   }

   public synchronized boolean setLastTraderNotification(long timestamp) {
      if (timestamp <= lastTraderNotification) {
         return false;
      }
      lastTraderNotification = timestamp;
      getEditor()
              .putLong(Constants.LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING, timestamp)
              .apply();
      Log.i(TAG, "Updated trader notification timestamp to: " + timestamp);
      if (needsTraderSynchronization()) {
         notifyTraderActivity(lastTraderNotification);
      }
      return true;
   }

   /**
    * Has the Local Trader server reported that it has more recent trader data
    * than what the app has seen?
    */
   public synchronized boolean needsTraderSynchronization() {
      return lastTraderSynchronization < lastTraderNotification;
   }

   public void setLocation(GpsLocationEx location) {
      currentLocation = location;
      getEditor().putFloat(Constants.LOCAL_TRADER_LATITUDE_SETTING, (float) location.latitude)
            .putFloat(Constants.LOCAL_TRADER_LONGITUDE_SETTING, (float) location.longitude)
            .putString(Constants.LOCAL_TRADER_LOCATION_NAME_SETTING, location.name)
            .putString(Constants.LOCAL_TRADER_LOCATION_COUNTRY_CODE_SETTING, location.countryCode)
            .apply();
   }

   public GpsLocationEx getUserLocation() {
      return currentLocation;
   }

   public void setLocalTraderEnabled(boolean enabled) {
      localTraderEnabled = enabled;
      getEditor()
              .putBoolean(Constants.LT_ENABLED, enabled)
              .apply();
   }

   public boolean isLocalTraderEnabled() {
      return localTraderEnabled;
   }

   public void setPlaySoundOnTradeNotification(boolean enabled) {
      playSoundOnTradeNotification = enabled;
      getEditor()
              .putBoolean(Constants.LOCAL_TRADER_PLAY_SOUND_ON_TRADE_NOTIFICATION_SETTING, enabled)
              .apply();
   }

   public boolean getPlaySoundOnTradeNotification() {
      return playSoundOnTradeNotification;
   }

   public void setLastNotificationSoundTimestamp(long timestamp) {
      if (timestamp > lastNotificationSoundTimestamp) {
         lastNotificationSoundTimestamp = timestamp;
      }
   }

   public long getLastNotificationSoundTimestamp() {
      return lastNotificationSoundTimestamp;
   }

   public void setUseMiles(boolean enabled) {
      usemiles = enabled;
      getEditor()
              .putBoolean(Constants.LOCAL_TRADER_USE_MILES_SETTING, enabled)
              .apply();
   }

   public boolean useMiles() {
      return usemiles;
   }

   public boolean isCaptchaRequired(Request request) {
      if (request instanceof CreateAd) {
         return session == null || session.captcha.contains(LtSession.CaptchaCommands.CREATE_SELL_ORDER);
      } else if (request instanceof CreateTrade) {
         return session == null || session.captcha.contains(LtSession.CaptchaCommands.CREATE_INSTANT_BUY_ORDER);
      }
      return false;
   }

   public void initializeGooglePlayServices() {
      if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
         return;
      }
      if (getGcmRegistrationId() == null) {
         // Get the GCM ID in a background thread
         new Thread(new Runnable() {
            @Override
            public void run() {
               GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
               try {
                  String regId = gcm.register(LocalTraderManager.GCM_SENDER_ID);
                  storeGcmRegistrationId(regId);
               } catch (IOException e) {
                  Log.w(TAG, "IO exception while getting GCM ID:" + e.getMessage());
               }
            }
         }).start();
      }
   }

   private synchronized String getGcmRegistrationId() {
      final SharedPreferences prefs = getGcmPreferences();
      String registrationId = prefs.getString("gcmid", null);
      if (registrationId == null) {
         Log.i(TAG, "GCM registration not found.");
         return null;
      }
      // Check if app was updated; if so, it must clear the registration ID
      // since the existing regID is not guaranteed to work with the new
      // app version.
      int registeredVersion = prefs.getInt("appVersion", Integer.MIN_VALUE);
      int currentVersion = getAppVersion();
      if (registeredVersion != currentVersion) {
         Log.i(TAG, "App version changed.");
         return null;
      }
      return registrationId;
   }

   /**
    * Stores the registration ID and app versionCode in the application's
    * {@code SharedPreferences}.
    *
    * @param regId registration ID
    */
   private synchronized void storeGcmRegistrationId(String regId) {
      int appVersion = getAppVersion();
      Log.i(TAG, "Saving regId on app version " + appVersion);
      getGcmPreferences().edit()
              .putString("gcmid", regId)
              .putInt("appVersion", appVersion)
              .apply();
   }

   /**
    * @return Application's version code from the {@code PackageManager}.
    */
   private int getAppVersion() {
      try {
         PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
         return packageInfo.versionCode;
      } catch (NameNotFoundException e) {
         // should never happen
         throw new RuntimeException("Could not get package name: " + e);
      }
   }

   private SharedPreferences getGcmPreferences() {
      return context.getSharedPreferences(Constants.LOCAL_TRADER_GCM_SETTINGS_NAME, Activity.MODE_PRIVATE);
   }

   public LtApi getApi(){
      return api;
   }

   public LtSession getSession(){
      return session;
   }

   public Bitcoins getMinerFeeEstimation(){
      // choose a fee to get included within the next two blocks - our estimation for next block ist often too high
      return mbwManager.getWalletManager(false).getLastFeeEstimations().getEstimation(2);
   }
}

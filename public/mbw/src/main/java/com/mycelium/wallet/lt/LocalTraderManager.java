package com.mycelium.wallet.lt;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mycelium.lt.ApiUtils;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.model.LtSession;
import com.mycelium.lt.api.model.TradeSessionStatus;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.lt.api.params.LoginParameters;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.lt.api.CreateInstantBuyOrder;
import com.mycelium.wallet.lt.api.CreateSellOrder;
import com.mycelium.wallet.lt.api.Request;
import com.mycelium.wallet.persistence.TradeSessionDb;

public class LocalTraderManager {

   public static final String GCM_SENDER_ID = "1025080855849";

   private static final String TAG = "LocalTraderManager";

   final private Context _context;
   final private RecordManager _recordManager;
   final private TradeSessionDb _db;
   final private LtApi _api;
   final private MbwManager _mbwManager;
   final private Set<LocalTraderEventSubscriber> _subscribers;
   final private Thread _executer;
   private LtSession _session;
   final private List<Request> _requests;
   private boolean _isLoggedIn;
   private Address _localTraderAddress;
   private long _lastTraderSynchronization;
   private long _lastTraderNotification;
   private GpsLocation _currentLocation;
   private String _nickname;
   private boolean _isLocalTraderDisabled;
   private TraderChangeMonitor _traderChangeMonitor;
   private TradeSessionChangeMonitor _tradeSessionChangeMonitor;
   private boolean _notificationsEnabled;
   private TraderInfo _cachedTraderInfo;

   public LocalTraderManager(Context context, RecordManager recordManager, TradeSessionDb db, LtApi api,
         MbwManager mbwManager) {
      _notificationsEnabled = true;
      _context = context;
      _recordManager = recordManager;
      _db = db;
      _api = api;
      _mbwManager = mbwManager;
      _subscribers = new HashSet<LocalTraderEventSubscriber>();
      _requests = new LinkedList<Request>();

      // Preferences
      SharedPreferences preferences = _context.getSharedPreferences(Constants.LOCAL_TRADER_SETTINGS_NAME,
            Activity.MODE_PRIVATE);

      _nickname = preferences.getString(Constants.LOCAL_TRADER_NICKNAME_SETTING, null);
      String addressString = preferences.getString(Constants.LOCAL_TRADER_ADDRESS_SETTING, null);
      if (addressString != null) {
         _localTraderAddress = Address.fromString(addressString, _mbwManager.getNetwork());
         // May be null
      }

      // Load location from preferences or use default
      // _currentLocation = new
      // GpsLocation(Constants.LOCAL_TRADER_DEFAULT_LOCATION.latitude,
      // (float) Constants.LOCAL_TRADER_DEFAULT_LOCATION.longitude,
      // Constants.LOCAL_TRADER_DEFAULT_LOCATION.name);

      _currentLocation = new GpsLocation(preferences.getFloat(Constants.LOCAL_TRADER_LATITUDE_SETTING,
            (float) Constants.LOCAL_TRADER_DEFAULT_LOCATION.latitude), preferences.getFloat(
            Constants.LOCAL_TRADER_LONGITUDE_SETTING, (float) Constants.LOCAL_TRADER_DEFAULT_LOCATION.longitude),
            preferences.getString(Constants.LOCAL_TRADER_LOCATION_NAME_SETTING,
                  Constants.LOCAL_TRADER_DEFAULT_LOCATION.name));

      _isLocalTraderDisabled = preferences.getBoolean(Constants.LOCAL_TRADER_DISABLED_SETTING, false);

      _lastTraderSynchronization = preferences.getLong(Constants.LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING, 0);
      _lastTraderNotification = preferences.getLong(Constants.LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING, 0);

      _executer = new Thread(new Executor());
      _executer.setDaemon(true);
      _executer.start();

      _traderChangeMonitor = new TraderChangeMonitor(this, _api);
      _tradeSessionChangeMonitor = new TradeSessionChangeMonitor(this, _api);
   }

   public void subscribe(LocalTraderEventSubscriber listener) {
      synchronized (_subscribers) {
         _subscribers.add(listener);
         if (_subscribers.size() > 5) {
            Log.w("LocalTraderManager", "subscriber size seems large: " + _subscribers.size());
         }
      }
   }

   public void unsubscribe(LocalTraderEventSubscriber listener) {
      synchronized (_subscribers) {
         boolean removed = _subscribers.remove(listener);
         if (!removed) {
            Log.e("LocalTraderManager", "SUBSCRIBER NOT REMOVED");
         }
      }
   }

   public void makeRequest(Request request) {
      if (request.requiresLogin() && !hasLocalTraderAccount()) {
         throw new RuntimeException("Cannot make login request when trading is disabled");
      }
      synchronized (_requests) {
         _requests.add(request);
         _requests.notify();
      }
   }

   public void startMonitoringTrader() {
      _traderChangeMonitor.startMonitoring();
   }

   public void stopMonitoringTrader() {
      _traderChangeMonitor.stopMonitoring();
   }

   public void startMonitoringTradeSession(TradeSessionChangeMonitor.Listener listener) {
      if (_session == null) {
         Log.e(TAG, "Trying to monitor trade session without having a session");
         return;
      }
      _tradeSessionChangeMonitor.startMonitoring(_session.id, listener);
   }

   public void stopMonitoringTradeSession() {
      _tradeSessionChangeMonitor.stopMonitoring();
   }

   public void enableNotifications(boolean enabled) {
      _notificationsEnabled = enabled;
   }

   public boolean areNotificationsEnabled() {
      return _notificationsEnabled;
   }

   public interface LocalManagerApiContext {
      public void handleErrors(Request request, int errorCode);

      public void updateLocalTradeSessions(Collection<TradeSessionStatus> collection);

      public void updateSingleTradeSession(TradeSessionStatus tradeSession);

      public void cacheTraderInfo(TraderInfo traderInfo);
   }

   private class Executor implements Runnable, LocalManagerApiContext {

      @Override
      public void run() {
         while (true) {

            // Grab a request or wait
            Request request;
            synchronized (_requests) {
               if (_requests.size() == 0) {
                  try {
                     _requests.wait();
                  } catch (InterruptedException e) {
                     break;
                  }
               }
               request = _requests.remove(0);
            }

            // If the request requires a session and we don't got one, get one
            if (request.requiresSession() && _session == null) {
               if (!renewSession()) {
                  continue;
               }
            }

            // If the request requires a login and we don't are not logged in,
            // login
            if (request.requiresLogin() && !_isLoggedIn) {
               if (!login()) {
                  continue;
               }
            }
            request.execute(this, _api, _session.id, _subscribers);
         }

      }

      private boolean renewSession() {
         try {
            // Get new session
            _session = _api.createSession(LtApi.VERSION, _mbwManager.getLanguage(),
                  _mbwManager.getBitcoinDenomination().getAsciiName()).getResult();
            _isLoggedIn = false;
            return true;
         } catch (LtApiException e) {
            // Handle errors
            handleErrors(null, e.errorCode);
            return false;
         }
      }

      private boolean login() {
         Preconditions.checkNotNull(_session.id);
         // Sign session ID with private key
         InMemoryPrivateKey privateKey = getLocalTraderPrivateKey();
         if (privateKey == null) {
            handleErrors(null, LtApi.ERROR_CODE_TRADER_DOES_NOT_EXIST);
            return false;
         }
         String sigHashSessionId = ApiUtils.generateUuidHashSignature(privateKey, _session.id,
               new AndroidRandomSource());
         try {
            // Login
            LoginParameters params = new LoginParameters(getLocalTraderAddress(), sigHashSessionId);
            params.setGcmId(getGcmRegistrationId());
            _api.traderLogin(_session.id, params).getResult();
            _isLoggedIn = true;
            return true;
         } catch (LtApiException e) {
            if (e.errorCode == LtApi.ERROR_CODE_INVALID_SESSION) {
               if (renewSession()) {
                  return login();
               } else {
                  return false;
               }
            } else {
               handleErrors(null, e.errorCode);
               return false;
            }
         }
      }

      public void updateLocalTradeSessions(Collection<TradeSessionStatus> collection) {
         LocalTraderManager.this.updateLocalTradeSessions(collection);
      }

      public void updateSingleTradeSession(TradeSessionStatus tradeSession) {
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
                  synchronized (_requests) {
                     _requests.add(request);
                     _requests.notify();
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
            _isLoggedIn = false;
            _session = null;
            // Disconnect trader account
            unsetLocalTraderAccount();
            notifyNoTraderAccount(errorCode);
            break;
         default:
            _isLoggedIn = false;
            _session = null;
            notifyError(errorCode);
            break;
         }
      }

   }

   private void notifyNoConnection(final int errorCode) {
      synchronized (_subscribers) {
         for (final LocalTraderEventSubscriber s : _subscribers) {
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
      synchronized (_subscribers) {
         for (final LocalTraderEventSubscriber s : _subscribers) {
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
      synchronized (_subscribers) {
         for (final LocalTraderEventSubscriber s : _subscribers) {
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
      synchronized (_subscribers) {
         for (final LocalTraderEventSubscriber s : _subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  s.onLtError(errorCode);
               }
            });
         }
      }
   }

   private void notifyTraderActivity() {
      synchronized (_subscribers) {
         for (final LocalTraderEventSubscriber s : _subscribers) {
            s.getHandler().post(new Runnable() {

               @Override
               public void run() {
                  s.onLtTraderActicityNotification();
               }
            });
         }
      }
   }

   /**
    * May return null
    */
   public synchronized TradeSessionStatus getLocalTradeSession(UUID tradeSessionId) {
      return _db.get(tradeSessionId);
   }

   public synchronized Collection<TradeSessionStatus> getLocalTradeSessions() {
      return _db.getAll();
   }

   public synchronized Collection<TradeSessionStatus> getLocalBuyTradeSessions() {
      return _db.getBuyTradeSessions();
   }

   public synchronized Collection<TradeSessionStatus> getLocalSellTradeSessions() {
      return _db.getSellTradeSessions();
   }

   public synchronized int countLocalTradeSessions() {
      return _db.countTradeSessions();
   }

   public synchronized int countLocalBuyTradeSessions() {
      return _db.countBuyTradeSessions();
   }

   public synchronized int countLocalSellTradeSessions() {
      return _db.countSellTradeSessions();
   }

   public synchronized boolean isViewed(TradeSessionStatus tradeSession) {
      return _db.getViewTimeById(tradeSession.id) >= tradeSession.lastChange;
   }

   public synchronized void markViewed(TradeSessionStatus tradeSession) {
      _db.markViewed(tradeSession);
   }

   private synchronized void updateLocalTradeSessions(Collection<TradeSessionStatus> remoteList) {
      // Get all the local sessions
      Collection<TradeSessionStatus> localList = _db.getAll();

      // Iterate over local items to find records to delete or update locally
      Iterator<TradeSessionStatus> localIt = localList.iterator();
      while (localIt.hasNext()) {
         TradeSessionStatus localItem = localIt.next();
         TradeSessionStatus remoteItem = findAndEliminate(localItem, remoteList);
         if (remoteItem == null) {
            // A local item is not in the remote list, remove it locally
            _db.delete(localItem.id);
         } else {
            // A local item is in the new list, see if it needs to be updated
            if (needsUpdate(localItem, remoteItem)) {
               _db.update(remoteItem);
            }
         }
      }

      // Iterate over remaining remote items and insert them
      Iterator<TradeSessionStatus> remoteIt = remoteList.iterator();
      while (remoteIt.hasNext()) {
         TradeSessionStatus remoteItem = remoteIt.next();
         _db.insert(remoteItem);
      }

   }

   private synchronized void updateSingleTradeSession(TradeSessionStatus item) {
      _db.insert(item);
   }

   public void cacheTraderInfo(TraderInfo traderInfo) {
      _cachedTraderInfo = traderInfo;
   }

   public TraderInfo getCachedTraderInfo() {
      return _cachedTraderInfo;
   }

   private TradeSessionStatus findAndEliminate(TradeSessionStatus item, Collection<TradeSessionStatus> list) {
      Iterator<TradeSessionStatus> it = list.iterator();
      while (it.hasNext()) {
         TradeSessionStatus t = it.next();
         if (t.equals(item)) {
            it.remove();
            return t;
         }
      }
      return null;
   }

   private boolean needsUpdate(TradeSessionStatus oldValue, TradeSessionStatus newValue) {
      Preconditions.checkArgument(oldValue.id.equals(newValue.id));
      return oldValue.lastChange < newValue.lastChange;
   }

   private SharedPreferences.Editor getEditor() {
      return _context.getSharedPreferences(Constants.LOCAL_TRADER_SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
   }

   public boolean hasLocalTraderAccount() {
      return getLocalTraderPrivateKey() != null;
   }

   public String getNickname() {
      return _nickname;
   }

   public Address getLocalTraderAddress() {
      return _localTraderAddress;
   }

   private InMemoryPrivateKey getLocalTraderPrivateKey() {
      Record record = _recordManager.getRecord(_localTraderAddress);
      if (record != null && record.hasPrivateKey()) {
         return record.key;
      }
      if (_localTraderAddress != null) {
         unsetLocalTraderAccount();
      }
      return null;
   }

   public void unsetLocalTraderAccount() {
      _session = null;
      _localTraderAddress = null;
      _nickname = null;
      SharedPreferences.Editor editor = getEditor();
      editor.remove(Constants.LOCAL_TRADER_ADDRESS_SETTING);
      editor.remove(Constants.LOCAL_TRADER_NICKNAME_SETTING);
      setLastTraderSynchronization(0);
      _db.deleteAll();
      editor.commit();
   }

   public void setLocalTraderData(Address address, String nickname) {
      _session = null;
      _localTraderAddress = Preconditions.checkNotNull(address);
      _nickname = Preconditions.checkNotNull(nickname);
      SharedPreferences.Editor editor = getEditor();
      editor.putString(Constants.LOCAL_TRADER_ADDRESS_SETTING, address.toString());
      editor.putString(Constants.LOCAL_TRADER_NICKNAME_SETTING, nickname);
      editor.commit();
   }

   public synchronized void setLastTraderSynchronization(long timestamp) {
      _lastTraderSynchronization = timestamp;
      SharedPreferences.Editor editor = getEditor();
      editor.putLong(Constants.LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING, timestamp);
      editor.commit();
   }

   public synchronized long getLastTraderSynchronization() {
      return _lastTraderSynchronization;
   }

   public synchronized boolean setLastTraderNotification(long timestamp) {
      if (timestamp <= _lastTraderNotification) {
         return false;
      }
      _lastTraderNotification = timestamp;
      SharedPreferences.Editor editor = getEditor();
      editor.putLong(Constants.LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING, timestamp);
      editor.commit();
      Log.i(TAG, "Updated trader notification timestamp to: " + timestamp);
      if (needsTraderSynchronization()) {
         notifyTraderActivity();
      }
      return true;
   }

   /**
    * Has the Local Trader server reported that it has more recent trader data
    * than what the app has seen?
    */
   public synchronized boolean needsTraderSynchronization() {
      return _lastTraderSynchronization < _lastTraderNotification;
   }

   public void setLocation(GpsLocation location) {
      SharedPreferences.Editor editor = getEditor();
      _currentLocation = location;
      editor.putFloat(Constants.LOCAL_TRADER_LATITUDE_SETTING, (float) location.latitude);
      editor.putFloat(Constants.LOCAL_TRADER_LONGITUDE_SETTING, (float) location.longitude);
      editor.putString(Constants.LOCAL_TRADER_LOCATION_NAME_SETTING, location.name);
      editor.commit();
   }

   public GpsLocation getUserLocation() {
      return _currentLocation;
   }

   public void setLocalTraderDisabled(boolean disabled) {
      SharedPreferences.Editor editor = getEditor();
      _isLocalTraderDisabled = disabled;
      editor.putBoolean(Constants.LOCAL_TRADER_DISABLED_SETTING, disabled);
      editor.commit();
   }

   public boolean isLocalTraderDisabled() {
      return _isLocalTraderDisabled;
   }

   public boolean isCaptchaRequired(Request request) {
      if (request instanceof CreateSellOrder) {
         return _session == null ? true : _session.captcha.contains(LtSession.CaptchaCommands.CREATE_SELL_ORDER);
      } else if (request instanceof CreateInstantBuyOrder) {
         return _session == null ? true : _session.captcha.contains(LtSession.CaptchaCommands.CREATE_INSTANT_BUY_ORDER);
      }
      return false;
   }

   public void initializeGooglePlayServices() {
      if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(_context) != ConnectionResult.SUCCESS) {
         return;
      }
      if (getGcmRegistrationId() == null) {
         // Get the GCM ID in a background thread
         new Thread(new Runnable() {
            @Override
            public void run() {
               GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(_context);
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
    * @param context
    *           application's context.
    * @param regId
    *           registration ID
    */
   private synchronized void storeGcmRegistrationId(String regId) {
      final SharedPreferences prefs = getGcmPreferences();
      int appVersion = getAppVersion();
      Log.i(TAG, "Saving regId on app version " + appVersion);
      SharedPreferences.Editor editor = prefs.edit();
      editor.putString("gcmid", regId);
      editor.putInt("appVersion", appVersion);
      editor.commit();
   }

   /**
    * @return Application's version code from the {@code PackageManager}.
    */
   private int getAppVersion() {
      try {
         PackageInfo packageInfo = _context.getPackageManager().getPackageInfo(_context.getPackageName(), 0);
         return packageInfo.versionCode;
      } catch (NameNotFoundException e) {
         // should never happen
         throw new RuntimeException("Could not get package name: " + e);
      }
   }

   private SharedPreferences getGcmPreferences() {
      return _context.getSharedPreferences(Constants.LOCAL_TRADER_GCM_SETTINGS_NAME, Activity.MODE_PRIVATE);
   }

   public static float calculate5StarRating(int successfulSales, int abortedSales, int successfulBuys, int abortedBuys,
         long traderAgeMs) {
      float traderAgeDays = ((float) traderAgeMs) / 1000 / 60 / 60 / 24;

      int successful = successfulSales + successfulBuys;

      int aborted = abortedSales + abortedBuys;

      float ageComponent = getAgeRatingComponent(traderAgeDays);
      float successComponent = getVolumeRatingComponent(successful + aborted)
            * getRatingMultiplierBySuccess(successful, aborted);
      float rating = ageComponent + successComponent;

      // Rating should now be a number between -1 and 6

      rating = Math.min(5.0F, rating);
      rating = Math.max(0F, rating);
      return rating;
   }

   /**
    * The number of trades done with a maximum of 4
    */
   private static float getVolumeRatingComponent(int totalTrades) {
      return Math.min((float) totalTrades, 4F);
   }

   private static float getAgeRatingComponent(float traderAgeDays) {
      if (traderAgeDays < 0.1F) {
         // rating is 0 stars if the trader is brand new
         return 0F;
      } else if (traderAgeDays < 0.5) {
         // 0.5 stars if the trader has been around for less than half a day
         return 0.5F;
      } else if (traderAgeDays < 1) {
         // 1 star if the trader has been around for less than 1 day
         return 1F;
      } else if (traderAgeDays < 2) {
         // 1.25 stars if the trader has been around for less than 2 days
         return 1.25F;
      } else if (traderAgeDays < 3) {
         // 1.5 stars if the trader has been around for less than 3 days
         return 1.5F;
      } else if (traderAgeDays < 14) {
         // 1.75 stars if the trader has been around for less than 14 days
         return 1.75F;
      } else {
         // 2 stars if the trader is older than 14 days
         return 2F;
      }
   }

   /**
    * The success multiplier is a number between -1 and 1
    */
   private static float getRatingMultiplierBySuccess(int success, int abort) {
      int total = success + abort;
      if (total == 0) {
         return 0F;
      }
      // Make multiplier a number between 0 and 1 based on success ratio
      float multiplier = ((float) success) / total;

      // make multiplier a number between -1 and 1

      multiplier = (2 * multiplier) - 1;
      return multiplier;
   }

}

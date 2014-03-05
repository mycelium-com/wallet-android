package com.mycelium.wallet.lt;

import java.util.UUID;

import android.os.Handler;
import android.util.Log;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.model.TradeSessionStatus;

public class TradeSessionChangeMonitor {
   private static final String TAG = "TradeSessionChangeMonitor";

   public static abstract class Listener {
      private final Handler _handler;
      private final UUID _tradeSessionId;
      private final long _lastChange;

      protected Listener(UUID tradeSessionId, long lastChange) {
         _tradeSessionId = tradeSessionId;
         _lastChange = lastChange;
         _handler = new Handler();
      }

      public UUID getTradeSessionId() {
         return _tradeSessionId;
      }

      public long getLastChange() {
         return _lastChange;
      }

      public abstract void onTradeSessionChanged(TradeSessionStatus s);

   }

   private static class Monitor implements Runnable {

      private static final long RETRY_TIMEOUT_MS = 5000;
      private LocalTraderManager _ltManager;
      private LtApi _api;
      private Thread _thread;
      private UUID _sessionId;
      private Listener _listener;
      private boolean _isCancelled;

      public Monitor(LocalTraderManager ltManager, LtApi api, Listener listener, UUID sessionId) {
         _ltManager = ltManager;
         _api = api;
         _listener = listener;
         _sessionId = sessionId;
         _thread = new Thread(this);
         _thread.setDaemon(true);
         _thread.setName(TAG);
         _thread.start();
      }

      @Override
      public void run() {
         long timestamp = _listener.getLastChange();
         Log.i(TAG, "Monitor Starting with timestamp " + timestamp);
         try {
            while (!_isCancelled) {
               if (!_ltManager.hasLocalTraderAccount()) {
                  // The user might be about to enable local trader, sleep a
                  // while and try again
                  Thread.sleep(10000);
                  continue;
               }
               try {
                  // Wait for session change
                  final TradeSessionStatus result = _api.waitForTradeSessionChange(_sessionId,
                        _listener.getTradeSessionId(), timestamp).getResult();

                  if (_isCancelled) {
                     return;
                  }
                  // Update the timestamp we we trigger on the next update
                  timestamp = result.lastChange;
                  // Success, post update to caller
                  _listener._handler.post(new Runnable() {

                     @Override
                     public void run() {
                        if (!_isCancelled) {
                           _listener.onTradeSessionChanged(result);
                        }
                     }
                  });

               } catch (final LtApiException e) {
                  if (e.errorCode == LtApi.ERROR_CODE_WAIT_TIMEOUT) {
                     // Timeout, try again
                     continue;
                  } else {
                     // Something went wrong, sleep on it and try again
                     if (!_isCancelled) {
                        Log.w(TAG, "Monitoring failed, retrying in " + (RETRY_TIMEOUT_MS / 1000) + " seconds");
                        Thread.sleep(RETRY_TIMEOUT_MS);
                     }
                  }
               } // catch
            } // while
            Log.i(TAG, "Monitor stopping with timestamp " + timestamp);
         } catch (Exception e) {
            Log.e(TAG, "Caught exception in monitor, exiting.", e);
            _isCancelled = true;
         }
      }

      public void cancel() {
         if (!_isCancelled) {
            Log.i(TAG, "Cancelling monitor");
            _isCancelled = true;
            // Stop long polling from the server side by making an API call in a
            // separate thread
            Thread t = new Thread(new Runnable() {

               @Override
               public void run() {
                  try {
                     _api.stopWaitingForTradeSessionChange(_sessionId);
                  } catch (Exception e) {
                     // Ignore
                  }
               }
            });
            t.setDaemon(true);
            t.start();
         }
      }

   }

   private LocalTraderManager _ltManager;
   private LtApi _api;
   private Monitor _monitor;

   public TradeSessionChangeMonitor(LocalTraderManager ltManager, LtApi api) {
      _ltManager = ltManager;
      _api = api;
   }

   public void startMonitoring(UUID sessionId, Listener listener) {
      stopMonitoring();
      _monitor = new Monitor(_ltManager, _api, listener, sessionId);

   }

   public void stopMonitoring() {
      if (_monitor != null) {
         _monitor.cancel();
      }
      _monitor = null;
   }
}

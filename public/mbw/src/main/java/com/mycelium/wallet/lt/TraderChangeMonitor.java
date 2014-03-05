package com.mycelium.wallet.lt;

import java.util.UUID;

import android.util.Log;

import com.mrd.bitlib.model.Address;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiException;

public class TraderChangeMonitor {
   private static final String TAG = "TraderChangeMonitor";

   private static class Monitor implements Runnable {

      private static final long RETRY_TIMEOUT_MS = 5000;
      private LocalTraderManager _ltManager;
      private LtApi _api;
      private Thread _thread;
      private volatile boolean _isCancelled;
      private UUID _token;

      public Monitor(LocalTraderManager ltManager, LtApi api) {
         _ltManager = ltManager;
         _api = api;
         _token = UUID.randomUUID();
         _thread = new Thread(this);
         _thread.setDaemon(true);
         _thread.setName(TAG);
         _thread.start();
      }

      @Override
      public void run() {
         long timestamp = _ltManager.getLastTraderSynchronization();
         Log.i(TAG, "Monitor Starting with timestamp " + timestamp);
         try {
            while (!_isCancelled) {
               if (!_ltManager.hasLocalTraderAccount()) {
                  // The user might be about to enable local trader, sleep a
                  // while and try again
                  Thread.sleep(10000);
                  continue;
               }
               Address address = _ltManager.getLocalTraderAddress();
               try {
                  // Wait for session change
                  final Long result = _api.waitForTraderChange(address, _token, timestamp).getResult();

                  if (_isCancelled) {
                     return;
                  }
                  // Update the timestamp we we trigger on the next update
                  timestamp = result;
                  _ltManager.setLastTraderNotification(timestamp);

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
            Log.i(TAG, "Monitor stopping with timestamp "+timestamp);
         } catch (Exception e) {
            Log.e(TAG, "Caught exception in monitor, exiting.", e);
            _isCancelled = true;
         }
      }

      public void cancel() {
         if (!_isCancelled) {
            Log.i(TAG, "CAncelling monitor");
            _isCancelled = true;
            // Stop long polling from the server side by making an API call in a
            // separate thread
            Thread t = new Thread(new Runnable() {

               @Override
               public void run() {
                  _api.stopWaitingForTraderChange(_token);
               }
            });
            t.setDaemon(true);
            t.start();
         }
      }

   }

   LocalTraderManager _ltManager;
   LtApi _api;
   private Monitor _monitor;

   public TraderChangeMonitor(LocalTraderManager ltManager, LtApi api) {
      _ltManager = ltManager;
      _api = api;
   }

   public void startMonitoring() {
      stopMonitoring();
      _monitor = new Monitor(_ltManager, _api);

   }

   public void stopMonitoring() {
      if (_monitor != null) {
         _monitor.cancel();
      }
      _monitor = null;
   }
}

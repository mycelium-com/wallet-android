package com.mycelium.wallet;

import android.util.Log;
import com.mrd.mbwapi.api.ApiException;
import com.mycelium.wallet.activity.StartupActivity;

public class HttpErrorCollector implements Thread.UncaughtExceptionHandler {
   private final Thread.UncaughtExceptionHandler orig;

   public HttpErrorCollector(Thread.UncaughtExceptionHandler orig) {
      this.orig = orig;
   }

   @Override
   public void uncaughtException(Thread thread, Throwable throwable) {
      try {
         Constants.bccapi.collectError(throwable, StartupActivity.version);
      } catch (RuntimeException e) {
         Log.e(Constants.TAG, "error while sending error", e);
      } catch (ApiException e) {
         Log.e(Constants.TAG, "error while sending error", e);
      } finally {
         orig.uncaughtException(thread, throwable);
      }
   }
}

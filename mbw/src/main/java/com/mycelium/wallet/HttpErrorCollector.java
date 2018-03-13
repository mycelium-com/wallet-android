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

package com.mycelium.wallet;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.mycelium.wapi.api.WapiClient;
import com.mycelium.wapi.api.lib.ErrorMetaData;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.request.ErrorCollectorRequest;

public class HttpErrorCollector implements Thread.UncaughtExceptionHandler {
   private final Thread.UncaughtExceptionHandler orig;
   private final Wapi api;
   private final String version;
   private final ErrorMetaData metaData;

   private HttpErrorCollector(Thread.UncaughtExceptionHandler orig, Wapi api, String version, ErrorMetaData metaData) {
      this.orig = orig;
      this.api = api;
      this.version = version;
      this.metaData = metaData;
   }

   public static HttpErrorCollector registerInVM(Context applicationContext, WapiClient wapi) {
      // Initialize error collector
      if (BuildConfig.DEBUG) {
         return null;
      }
      Thread.UncaughtExceptionHandler orig = Thread.getDefaultUncaughtExceptionHandler();
      if (orig instanceof HttpErrorCollector) {
         return (HttpErrorCollector) orig;
      }
      String version = BuildConfig.VERSION_NAME + " / " + BuildConfig.VERSION_CODE;
      Log.i(Constants.TAG, "registering exception handler from thread " + Thread.currentThread().getName());
      HttpErrorCollector ret = new HttpErrorCollector(orig, wapi, version, buildMetaData(applicationContext));
      Thread.setDefaultUncaughtExceptionHandler(ret);
      return ret;
   }

   private static ErrorMetaData buildMetaData(Context applicationContext) {
      ActivityManager am = (ActivityManager) applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
      int memoryClass = am.getMemoryClass();
/*
         long maxMemory = Runtime.getRuntime().maxMemory();
*/
      return new ErrorMetaData(memoryClass, Build.VERSION.SDK_INT, Build.MODEL, Build.BRAND, Build.VERSION.RELEASE, Build.DEVICE);
   }

   @Override
   public void uncaughtException(final Thread thread, final Throwable throwable) {
      reportErrorToServer(throwable);
      orig.uncaughtException(thread, throwable);
   }

   /**
    * use this method, if we expect an error,
    * but we want to provide a meaningful error message instead of blowing up.
    * in most cases we should blow up, though.
    */
   public void reportErrorToServer(final Throwable throwable) {
      new Thread() {
         @Override
         public void run() {
            try {
               api.collectError(new ErrorCollectorRequest(throwable, version, metaData));
            } catch (RuntimeException e) {
               Log.e(Constants.TAG, "error while sending error", e);
            } finally {
               Log.e(Constants.TAG, "uncaught exception", throwable);
            }
         }
      }.start();
   }
}

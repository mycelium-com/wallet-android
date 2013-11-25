/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.HttpErrorCollector;
import com.mycelium.wallet.MbwEnvironment;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.pdf.ExportDistiller;
import com.mycelium.wallet.pdf.ExportDistiller.ExportProgressTracker;
import com.mycelium.wallet.pdf.ExportPdfParameters;

import java.io.IOException;
import java.io.Serializable;

public class ExportService extends Service {


   public static class Status implements Serializable {
      private static final long serialVersionUID = 1L;
      public final boolean isExporting;
      public final double progress;
      public final boolean isComplete;
      public final String errorMessage;

      public Status(boolean isExporting, double progress, boolean isComplete, String errorMessage) {
         this.isExporting = isExporting;
         this.progress = progress;
         this.isComplete = isComplete;
         this.errorMessage = errorMessage;
      }
   }

   public static final int MSG_START = 1;
   public static final int MSG_GET_STATUS = 2;
   public static final int MSG_STATUS = 3;
   public static final int MSG_TERMINATE = 6;
   private Messenger _serviceMessenger;
   private ExportPdfParameters _parameters;
   private String _filePath;
   private ExportProgressTracker _progress;
   private Thread _thread;
   private boolean _isComplete;
   private String _error;
   private Context _applicationContext;
   private HttpErrorCollector _httpErrorCollector;

   @SuppressLint("HandlerLeak")
   private class IncomingHandler extends Handler {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case MSG_START:
               Bundle bundle = Preconditions.checkNotNull(msg.getData());
               ExportPdfParameters parameters =
                     (ExportPdfParameters) Preconditions.checkNotNull(bundle.getSerializable("parameters"));
               String filePath = Preconditions.checkNotNull(bundle.getString("filePath"));
               start(parameters, filePath);
               break;
            case MSG_GET_STATUS:
               sendStatus(msg.replyTo);
               break;
            case MSG_TERMINATE:
               terminate();
               stopSelf();
               break;
            default:
               super.handleMessage(msg);
         }
      }

      private void start(ExportPdfParameters parameters, String filePath) {
         // We may have a stretching going on already, stop it
         terminate();
         _parameters = parameters;
         _filePath = filePath;
         _progress = ExportDistiller.createExportProgressTracker(parameters.active, parameters.active);
         _thread = new Thread(new Runner());
         _thread.start();
      }

      private void sendStatus(Messenger messenger) {
         Message msg = Preconditions.checkNotNull(Message.obtain(null, MSG_STATUS));
         double progress = _progress == null ? 0 : _progress.getProgress();
         Bundle b = new Bundle();
         b.putSerializable("status", new Status(_thread == null, progress, _isComplete, _error));
         msg.setData(b);
         try {
            messenger.send(msg);
         } catch (RemoteException e) {
            _httpErrorCollector.reportErrorToServer(e);
            // We ignore any exceptions from the caller, they asked for an
            // update
            // and we don't care if they are not around anymore
         }
      }

      private void terminate() {
         Thread t = _thread;
         if (t != null) {
            try {
               t.interrupt();
               t.join();
            } catch (InterruptedException e) {
               // Ignore
            }
         }
         _thread = null;
         _error = null;
         _isComplete = false;
      }

   }

   @Override
   public IBinder onBind(Intent intent) {
      return _serviceMessenger.getBinder();
   }

   @Override
   public void onCreate() {
      _applicationContext = this.getApplicationContext();
      MbwEnvironment env = MbwEnvironment.determineEnvironment(_applicationContext);
      String version = MbwManager.determineVersion(_applicationContext);
      _httpErrorCollector = HttpErrorCollector.registerInVM(_applicationContext, version, env.getMwsApi());
      _serviceMessenger = new Messenger(new IncomingHandler());
   }

   private class Runner implements Runnable {

      @Override
      public void run() {
         try {
            ExportDistiller.exportPrivateKeysToFile(_applicationContext, _parameters, _progress, _filePath);
         } catch (OutOfMemoryError e) {
            Log.e("ExportService", "OOM while exporting", e);
            _error = "Insufficient memory to create document";
            reportIgnoredError(e);
         } catch (IOException e) {
            Log.e("ExportService", "IOException while exporting", e);
            _error = "An IO error occurred: " + e.getMessage();
            reportIgnoredError(e);
         }
         _isComplete = true;
         _parameters = null;
         _filePath = null;
         _thread = null;
      }
   }

   private void reportIgnoredError(Throwable e) {
      RuntimeException msg = new RuntimeException("we caught an exception and displayed a message to the user: \n" + _error, e);
      _httpErrorCollector.reportErrorToServer(msg);
   }

}

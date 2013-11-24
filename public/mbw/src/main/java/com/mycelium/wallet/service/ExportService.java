package com.mycelium.wallet.service;

import java.io.Serializable;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.HttpErrorCollector;
import com.mycelium.wallet.pdf.ExportDestiller;
import com.mycelium.wallet.pdf.ExportDestiller.ExportProgressTracker;

public class ExportService extends Service {

   public static class Status implements Serializable {
      private static final long serialVersionUID = 1L;
      public boolean isExporting;
      public double progress;
      public boolean isComplete;
      public String errorMessage;

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
   public static final int MSG_GET_RESULT = 4;
   public static final int MSG_RESULT = 5;
   public static final int MSG_TERMINATE = 6;
   private Messenger _serviceMessenger;
   private ExportDestiller.ExportPdfParameters _parameters;
   private ExportProgressTracker _progress;
   private Thread _thread;
   private boolean _isComplete;
   private String _result;
   private String _error;
   public Context _applicationContext;

   @SuppressLint("HandlerLeak")
   private class IncomingHandler extends Handler {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case MSG_START:
               ExportDestiller.ExportPdfParameters parameters = (ExportDestiller.ExportPdfParameters) Preconditions
                     .checkNotNull(msg.getData().getSerializable("parameters"));
               start(parameters);
               break;
            case MSG_GET_STATUS:
               sendStatus(msg.replyTo);
               break;
            case MSG_GET_RESULT:
               sendResult(msg.replyTo);
               break;
            case MSG_TERMINATE:
               terminate();
               stopSelf();
               break;
            default:
               super.handleMessage(msg);
         }
      }

      private void start(ExportDestiller.ExportPdfParameters parameters) {
         Log.e("INFO", "Stretch start");
         // We may have a stretching going on already, stop it
         terminate();
         _parameters = parameters;
         _progress = ExportDestiller.createExportProgressTracker(parameters.active, parameters.active);
         _thread = new Thread(new Runner());
         _thread.start();
      }

      private void sendStatus(Messenger messenger) {
         Message msg = Message.obtain(null, MSG_STATUS);
         double progress = _progress == null ? 0 : _progress.getProgress();
         Bundle b = new Bundle();
         b.putSerializable("status", new Status(_thread == null, progress, _isComplete, _error));
         msg.setData(b);
         try {
            messenger.send(msg);
         } catch (RemoteException e) {
            // We ignore any exceptions from the caller, they asked for an
            // update
            // and we don't care if they are not around anymore
         }
      }

      private void sendResult(Messenger replyTo) {
         Message msg = Message.obtain(null, MSG_RESULT);
         Bundle b = new Bundle();
         b.putString("result", _result);
         msg.setData(b);
         try {
            replyTo.send(msg);
         } catch (RemoteException e) {
            // We ignore any exceptions from the caller, they asked for an
            // update
            // and we don't care if they are not around anymore
         }
      }

      private void terminate() {
         Log.e("INFO", "Stretch stop");
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
      Log.e("INFO", "Binding");
      return _serviceMessenger.getBinder();
   }

   @Override
   public void onCreate() {
      _applicationContext = this.getApplicationContext();
      HttpErrorCollector.registerInVM(_applicationContext);
      Log.e("INFO", "Service Started from onCreate!");
      _serviceMessenger = new Messenger(new IncomingHandler());
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   public class MyBinder extends Binder {
      ExportService getService() {
         return ExportService.this;
      }
   }

   private class Runner implements Runnable {

      @Override
      public void run() {
         try {
            _result = ExportDestiller.exportPrivateKeys(_applicationContext, _parameters, _progress);
         } catch (OutOfMemoryError e) {
            _error = "Insufficient memory to create document";
         }
         _isComplete = true;
         _parameters = null;
         _thread = null;
      }
   }

}

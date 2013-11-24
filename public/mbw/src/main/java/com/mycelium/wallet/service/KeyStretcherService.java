package com.mycelium.wallet.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.HttpErrorCollector;

import java.io.Serializable;

public class KeyStretcherService extends Service {

   public static class Status implements Serializable {
      private static final long serialVersionUID = 1L;

      public final boolean isStretching;
      public final double progress;
      public final boolean hasResult;
      public final boolean error;

      public Status(boolean isStretching, double progress, boolean hasResult, boolean error) {
         this.isStretching = isStretching;
         this.progress = progress;
         this.hasResult = hasResult;
         this.error = error;
      }
   }

   public static final int MSG_START = 1;
   public static final int MSG_GET_STATUS = 2;
   public static final int MSG_STATUS = 3;
   public static final int MSG_GET_RESULT = 3;
   public static final int MSG_RESULT = 4;
   public static final int MSG_TERMINATE = 5;
   private Messenger _serviceMessenger;
   private KdfParameters _kdfParameters;
   private Thread _stretcherThread;
   private MrdExport.V1.EncryptionParameters _result;
   private boolean _error;

   //@SuppressLint("HandlerLeak")
   private class IncomingHandler extends Handler {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case MSG_START:
               Bundle bundle = Preconditions.checkNotNull(msg.getData());
               KdfParameters kdfParameters = (KdfParameters) bundle.getSerializable("kdfParameters");
               start(Preconditions.checkNotNull(kdfParameters));
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

      private void start(KdfParameters kdfParameters) {
         // We may have a stretching going on already, stop it
         terminate();
         _kdfParameters = kdfParameters;
         _stretcherThread = new Thread(new Runner());
         _stretcherThread.start();
      }

      private void sendStatus(Messenger messenger) {
         Message msg = Message.obtain(null, MSG_STATUS);
         double progress = _kdfParameters == null ? 0 : _kdfParameters.getProgress();
         Bundle b = new Bundle();
         b.putSerializable("status", new Status(_stretcherThread == null, progress, _result != null, _error));
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
         b.putSerializable("result", _result);
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
         Thread t = _stretcherThread;
         if (t != null) {
            KdfParameters params = _kdfParameters;
            if (params != null) {
               params.terminate();
            }
            try {
               t.join();
            } catch (InterruptedException e) {
               // Ignore
            }
         }
         _stretcherThread = null;
         _error = false;
         _result = null;
      }

   }

   @Override
   public IBinder onBind(Intent intent) {
      return _serviceMessenger.getBinder();
   }

   @Override
   public void onCreate() {
      HttpErrorCollector.registerInVM(getApplicationContext());
      _serviceMessenger = new Messenger(new IncomingHandler());
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   public class MyBinder extends Binder {
      KeyStretcherService getService() {
         return KeyStretcherService.this;
      }
   }

   private class Runner implements Runnable {

      @Override
      public void run() {
         try {
            _result = MrdExport.V1.EncryptionParameters.generate(_kdfParameters);
            _error = false;
         } catch (OutOfMemoryError e) {
            _result = null;
            _error = true;
         } catch (InterruptedException e) {
            // This happens when it is terminated
            return;
         }
         _kdfParameters = null;
         _stretcherThread = null;
      }
   }

}

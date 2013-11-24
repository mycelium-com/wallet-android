package com.mycelium.wallet.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.service.KeyStretcherService.Status;

public class KeyStretcherServiceController {

   public interface KeyStretcherServiceCallback {
      public void onStatusReceived(KeyStretcherService.Status status);

      public void onResultReceived(MrdExport.V1.EncryptionParameters parameters);
   }

   private class MyServiceConnection extends Handler implements ServiceConnection {
      private Messenger _serviceMessenger;
      private Messenger _myMessenger;
      private boolean _sendStop;
      private KdfParameters _kdfParameters;

      public MyServiceConnection() {
         _myMessenger = new Messenger(this);
      }

      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case KeyStretcherService.MSG_STATUS:
            Status status = (Status) msg.getData().getSerializable("status");
            _callbackHandler.onStatusReceived(status);
            break;
         case KeyStretcherService.MSG_RESULT:
            MrdExport.V1.EncryptionParameters result = (MrdExport.V1.EncryptionParameters) msg.getData()
                  .getSerializable("result");
            _callbackHandler.onResultReceived(result);
            break;
         default:
            super.handleMessage(msg);
         }
      }

      public void onServiceConnected(ComponentName name, IBinder binder) {
         _serviceMessenger = new Messenger(binder);
         if (_sendStop) {
            terminate();
         }
         if (_kdfParameters != null) {
            _error = start(_kdfParameters);
         }
      }

      public void onServiceDisconnected(ComponentName name) {
         _serviceMessenger = null;
      }

      public boolean start(KdfParameters kdfParameters) {
         if (_serviceMessenger == null) {
            // Not yet connected, send it when we get a connection
            _kdfParameters = kdfParameters;
            return false;
         }
         try {
            Message msg = Message.obtain(null, KeyStretcherService.MSG_START);
            Bundle b = new Bundle();
            b.putSerializable("kdfParameters", kdfParameters);
            msg.setData(b);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            _kdfParameters = null;
            return true;
         } catch (RemoteException e) {
            Log.e("KeyStretcherService", "Remote exception: " + e.getMessage());
            return false;
         }
      }

      public boolean requestStatus() {
         if (_serviceMessenger == null) {
            // Not yet connected
            return false;
         }
         try {
            Message msg = Message.obtain(null, KeyStretcherService.MSG_GET_STATUS);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            return true;
         } catch (RemoteException e) {
            Log.e("KeyStretcherService", "Remote exception: " + e.getMessage());
            return false;
         }
      }

      public boolean requestResult() {
         if (_serviceMessenger == null) {
            // Not yet connected
            return false;
         }
         try {
            Message msg = Message.obtain(null, KeyStretcherService.MSG_GET_RESULT);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            return true;
         } catch (RemoteException e) {
            Log.e("KeyStretcherService", "Remote exception: " + e.getMessage());
            return false;
         }
      }

      public boolean terminate() {
         if (_serviceMessenger == null) {
            // Not yet connected
            _sendStop = true;
            return false;
         }
         try {
            Message msg = Message.obtain(null, KeyStretcherService.MSG_TERMINATE);
            _serviceMessenger.send(msg);
            _sendStop = false;
            return true;
         } catch (RemoteException e) {
            Log.e("KeyStretcherService", "Remote exception: " + e.getMessage());
            return false;
         }
      }

   }

   private MyServiceConnection _connection;
   private KeyStretcherServiceCallback _callbackHandler;
   private boolean _error;

   @SuppressLint("InlinedApi")
   public KeyStretcherServiceController() {
   }

   public boolean hasError() {
      return _error;
   }

   public void bind(Activity activity, KeyStretcherServiceCallback callbackHandler) {
      _connection = new MyServiceConnection();
      _callbackHandler = callbackHandler;
      Intent intent = new Intent(activity, KeyStretcherService.class);
      activity.bindService(intent, _connection, Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
   }

   public void unbind(Activity activity) {
      if (_connection == null) {
         // Not yet bound, no need to unbind
         return;
      }
      activity.unbindService(_connection);
   }

   public void start(KdfParameters kdfParameters) {
      _connection.start(kdfParameters);
   }

   public void requestStatus() {
      _connection.requestStatus();
   }

   public void requestResult() {
      _connection.requestResult();
   }

   public void terminate() {
      if (_connection == null) {
         // Not yet bound, no need to terminate
         return;
      }
      _connection.terminate();
   }

}

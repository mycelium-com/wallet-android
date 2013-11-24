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

import com.mycelium.wallet.pdf.ExportDestiller.ExportPdfParameters;

public class ExportServiceController {

   public interface ExportServiceCallback {
      public void onExportStatusReceived(ExportService.Status status);

      public void onExportResultReceived(String result);
   }

   private class MyServiceConnection extends Handler implements ServiceConnection {
      private Messenger _serviceMessenger;
      private Messenger _myMessenger;
      private boolean _sendStop;
      private ExportPdfParameters _parameters;
      private String _filePath;

      public MyServiceConnection() {
         _myMessenger = new Messenger(this);
      }

      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case ExportService.MSG_STATUS:
            ExportService.Status status = (ExportService.Status) msg.getData().getSerializable("status");
            _callbackHandler.onExportStatusReceived(status);
            Log.e("INFO", "Got status: " + status.progress);
            break;
         case ExportService.MSG_RESULT:
            String result= msg.getData().getString("result");
            _callbackHandler.onExportResultReceived(result);
            Log.e("INFO", "Got result");
            break;
         default:
            super.handleMessage(msg);
         }
      }

      public void onServiceConnected(ComponentName name, IBinder binder) {
         Log.e("INFO", "Conection: " + name + " connected 1");
         _serviceMessenger = new Messenger(binder);
         if (_sendStop) {
            terminate();
         }
         if (_parameters != null) {
            _error = start(_parameters, _filePath);
         }
         Log.e("INFO", "Conection: " + name + " connected 2");
      }

      public void onServiceDisconnected(ComponentName name) {
         Log.e("INFO", "Conection: " + name + " disconnected");
         _serviceMessenger = null;
      }

      public boolean start(ExportPdfParameters parameters, String filePath) {
         if (_serviceMessenger == null) {
            // Not yet connected, send it when we get a connection
            _parameters = parameters;
            _filePath = filePath;
            return false;
         }
         try {
            Message msg = Message.obtain(null, ExportService.MSG_START);
            Bundle b = new Bundle();
            b.putSerializable("parameters", parameters);
            b.putString("filePath", filePath);
            msg.setData(b);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            _parameters = null;
            _filePath = null;
            return true;
         } catch (RemoteException e) {
            Log.e("ExportServiceController", "Remote exception: " + e.getMessage());
            return false;
         }
      }

      public boolean requestStatus() {
         if (_serviceMessenger == null) {
            // Not yet connected
            return false;
         }
         try {
            Message msg = Message.obtain(null, ExportService.MSG_GET_STATUS);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            return true;
         } catch (RemoteException e) {
            Log.e("ExportServiceController", "Remote exception: " + e.getMessage());
            return false;
         }
      }

      public boolean requestResult() {
         if (_serviceMessenger == null) {
            // Not yet connected
            return false;
         }
         try {
            Message msg = Message.obtain(null, ExportService.MSG_GET_RESULT);
            msg.replyTo = _myMessenger;
            _serviceMessenger.send(msg);
            return true;
         } catch (RemoteException e) {
            Log.e("ExportServiceController", "Remote exception: " + e.getMessage());
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
            Message msg = Message.obtain(null, ExportService.MSG_TERMINATE);
            _serviceMessenger.send(msg);
            _sendStop = false;
            return true;
         } catch (RemoteException e) {
            Log.e("ExportServiceController", "Remote exception: " + e.getMessage());
            return false;
         }
      }

   }

   private MyServiceConnection _connection;
   private ExportServiceCallback _callbackHandler;
   private boolean _error;

   @SuppressLint("InlinedApi")
   public ExportServiceController() {
   }

   public boolean hasError() {
      return _error;
   }

   public void bind(Activity activity, ExportServiceCallback callbackHandler) {
      Intent intent = new Intent(activity, ExportService.class);
      _connection = new MyServiceConnection();
      _callbackHandler = callbackHandler;
      activity.bindService(intent, _connection, Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
   }

   public void unbind(Activity activity) {
      if (_connection == null) {
         // Not yet bound, no need to unbind
         return;
      }
      activity.unbindService(_connection);
   }

   public void start(ExportPdfParameters parameters, String filePath) {
      _connection.start(parameters, filePath);
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

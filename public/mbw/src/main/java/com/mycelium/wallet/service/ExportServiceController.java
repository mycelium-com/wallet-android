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

import com.mycelium.wallet.pdf.ExportPdfParameters;

public class ExportServiceController {

   public interface ExportServiceCallback {
      public void onExportStatusReceived(ExportService.Status status);
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
         if (_parameters != null) {
            _error = start(_parameters, _filePath);
         }
      }

      public void onServiceDisconnected(ComponentName name) {
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
            Log.e("ExportServiceController", "Remote exception", e);
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
            Log.e("ExportServiceController", "Remote exception", e);
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
            Log.e("ExportServiceController", "Remote exception", e);
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

   public void terminate() {
      if (_connection == null) {
         // Not yet bound, no need to terminate
         return;
      }
      _connection.terminate();
   }

}

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

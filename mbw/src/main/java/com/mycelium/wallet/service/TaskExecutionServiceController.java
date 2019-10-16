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

public class TaskExecutionServiceController {
   private static final String TAG = "TaskExecutionService";
   private MyServiceConnection connection;
   private TaskExecutionServiceCallback callbackHandler;

   public interface TaskExecutionServiceCallback {
      void onStatusReceived(ServiceTaskStatusEx status);

      void onResultReceived(ServiceTask<?> result);
   }

   @SuppressLint("HandlerLeak")
   private class MyServiceConnection extends Handler implements ServiceConnection {
      private Messenger serviceMessenger;
      private Messenger myMessenger;
      private boolean sendStop;
      private ServiceTask<?> task;

      MyServiceConnection() {
         myMessenger = new Messenger(this);
      }

      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case TaskExecutionService.MSG_STATUS:
            ServiceTaskStatusEx status = (ServiceTaskStatusEx) msg.getData().getSerializable("status");
            callbackHandler.onStatusReceived(status);
            break;
         case TaskExecutionService.MSG_RESULT:
            ServiceTask<?> result = (ServiceTask<?>) msg.getData().getSerializable("result");
            callbackHandler.onResultReceived(result);
            break;
         default:
            super.handleMessage(msg);
         }
      }

      public void onServiceConnected(ComponentName name, IBinder binder) {
         serviceMessenger = new Messenger(binder);
         if (sendStop) {
            terminate();
         }
         if (task != null) {
            start(task);
         }
      }

      public void onServiceDisconnected(ComponentName name) {
         serviceMessenger = null;
      }

      public boolean start(ServiceTask<?> task) {
         if (serviceMessenger == null) {
            // Not yet connected, send it when we get a connection
            this.task = task;
            return false;
         }
         try {
            Message msg = Message.obtain(null, TaskExecutionService.MSG_START);
            Bundle b = new Bundle();
            b.putSerializable("task", this.task);
            msg.setData(b);
            msg.replyTo = myMessenger;
            serviceMessenger.send(msg);
            this.task = null;
            return true;
         } catch (RemoteException e) {
            Log.e(TAG, "Remote exception: " + e.getMessage());
            return false;
         }
      }

      void requestStatus() {
         if (serviceMessenger == null) {
            // Not yet connected
            return;
         }
         try {
            Message msg = Message.obtain(null, TaskExecutionService.MSG_GET_STATUS);
            msg.replyTo = myMessenger;
            serviceMessenger.send(msg);
         } catch (RemoteException e) {
            Log.e(TAG, "Remote exception: " + e.getMessage());
         }
      }

      void requestResult() {
         if (serviceMessenger == null) {
            // Not yet connected
            return;
         }
         try {
            Message msg = Message.obtain(null, TaskExecutionService.MSG_GET_RESULT);
            msg.replyTo = myMessenger;
            serviceMessenger.send(msg);
         } catch (RemoteException e) {
            Log.e(TAG, "Remote exception: " + e.getMessage());
         }
      }

      public boolean terminate() {
         if (serviceMessenger == null) {
            // Not yet connected
            sendStop = true;
            return false;
         }
         try {
            Message msg = Message.obtain(null, TaskExecutionService.MSG_TERMINATE);
            serviceMessenger.send(msg);
            sendStop = false;
            return true;
         } catch (RemoteException e) {
            Log.e(TAG, "Remote exception: " + e.getMessage());
            return false;
         }
      }
   }

   public void bind(Activity activity, TaskExecutionServiceCallback callbackHandler) {
      connection = new MyServiceConnection();
      this.callbackHandler = callbackHandler;
      Intent intent = new Intent(activity, TaskExecutionService.class);
      activity.bindService(intent, connection, Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
   }

   public void unbind(Activity activity) {
      if (connection == null) {
         // Not yet bound, no need to unbind
         return;
      }
      activity.unbindService(connection);
   }

   public void start(ServiceTask<?> task) {
      connection.start(task);
   }

   public void requestStatus() {
      connection.requestStatus();
   }

   public void requestResult() {
      connection.requestResult();
   }

   public void terminate() {
      if (connection == null) {
         // Not yet bound, no need to terminate
         return;
      }
      connection.terminate();
   }
}

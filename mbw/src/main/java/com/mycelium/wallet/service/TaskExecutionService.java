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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.HttpErrorCollector;
import com.mycelium.wallet.MbwManager;

public class TaskExecutionService extends Service {
   private HttpErrorCollector _httpErrorCollector;

   public static final int MSG_START = 1;
   public static final int MSG_GET_STATUS = 2;
   public static final int MSG_STATUS = 3;
   public static final int MSG_GET_RESULT = 3;
   public static final int MSG_RESULT = 4;
   public static final int MSG_TERMINATE = 5;
   private Messenger _serviceMessenger;
   private Context _applicationContext;
   private Thread _taskThread;
   ServiceTaskStatusEx.State _state;
   private ServiceTask<?> _currentTask;

   @SuppressLint("HandlerLeak")
   private class IncomingHandler extends Handler {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case MSG_START:
            Bundle bundle = Preconditions.checkNotNull(msg.getData());
            ServiceTask<?> task = (ServiceTask<?>) bundle.getSerializable("task");
            start(Preconditions.checkNotNull(task));
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

      private void start(ServiceTask<?> task) {
         // We may have a stretching going on already, stop it
         terminate();
         _state = ServiceTaskStatusEx.State.STARTING;
         _currentTask = task;
         _taskThread = new Thread(new Runnable() {

            @Override
            public void run() {
               _state = ServiceTaskStatusEx.State.RUNNING;
               _currentTask.run(_applicationContext);
               _taskThread = null;
               _state = ServiceTaskStatusEx.State.FINISHED;
            }
         });
         _taskThread.start();
      }

      private void sendStatus(Messenger messenger) {
         Message msg = Preconditions.checkNotNull(Message.obtain(null, MSG_STATUS));
         Bundle b = new Bundle();
         if (_currentTask == null) {
            b.putSerializable("status", null);
         } else {
            ServiceTaskStatus status = _currentTask.getStatus();
            ServiceTaskStatusEx statusEx = new ServiceTaskStatusEx(status.statusMessage, status.progress, _state);
            b.putSerializable("status", statusEx);
         }
         msg.setData(b);
         try {
            messenger.send(msg);
         } catch (RemoteException e) {
            // We ignore any exceptions from the caller, they asked for an
            // update
            // and we don't care if they are not around anymore
            _httpErrorCollector.reportErrorToServer(e);
         }
      }

      private void sendResult(Messenger replyTo) {
         Message msg = Preconditions.checkNotNull(Message.obtain(null, MSG_RESULT));
         Bundle b = new Bundle();
         if (_currentTask == null || _state != ServiceTaskStatusEx.State.FINISHED) {
            b.putSerializable("result", null);
         } else {
            b.putSerializable("result", _currentTask);
         }
         msg.setData(b);
         try {
            replyTo.send(msg);
         } catch (RemoteException e) {
            // We ignore any exceptions from the caller, they asked for an
            // update
            // and we don't care if they are not around anymore
            _httpErrorCollector.reportErrorToServer(e);
         }
      }

      private void terminate() {
         Thread t = _taskThread;
         if (t != null) {
            if (_currentTask != null) {
               _currentTask.terminate();
            }
            try {
               t.join();
            } catch (InterruptedException ignored) {
               // Ignore
            }
         }
         _state = ServiceTaskStatusEx.State.NOTRUNNING;
         _taskThread = null;
         _currentTask = null;
      }

   }

   @Override
   public IBinder onBind(Intent intent) {
      return _serviceMessenger.getBinder();
   }

   @Override
   public void onCreate() {
      _state = ServiceTaskStatusEx.State.NOTRUNNING;
      _applicationContext = this.getApplicationContext();

      _httpErrorCollector = HttpErrorCollector.registerInVM(getApplicationContext(), MbwManager.getInstance(_applicationContext).getWapi());

      _serviceMessenger = new Messenger(new IncomingHandler());
   }

}

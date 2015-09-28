/*
*******************************************************************************    
*   Ledger Bitcoin Hardware Wallet Java API
*   (c) 2014-2015 Ledger - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.ledger.tbase.comm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.btchip.comm.BTChipTransport;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.ledger.wallet.service.ILedgerWalletService;

public class LedgerTransportTEEProxyFactory implements BTChipTransportFactory {

   private Context context;
   private ILedgerWalletService service;
   private LedgerTransportTEEProxy transport;
   private BTChipTransportFactoryCallback callback;
   private ServiceConnection serviceConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
         Log.d(TAG, "Service connected");
         service = ILedgerWalletService.Stub.asInterface(serviceBinder);
         if (transport != null) {
            transport.setService(service);
         }
         if (callback != null) {
            BTChipTransportFactoryCallback currentCallback = callback;
            callback = null;
            currentCallback.onConnected(true);
         }
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
         Log.d(TAG, "Service disconnected");
         if (callback != null) {
            BTChipTransportFactoryCallback currentCallback = callback;
            callback = null;
            currentCallback.onConnected(false);
         }
         if (transport != null) {
            transport.setService(null);
         }
      }
   };

   public static final String TAG = "LedgerTEEProxy";

   public LedgerTransportTEEProxyFactory(Context context) {
      this.context = context;
   }

   @Override
   public BTChipTransport getTransport() {
      if (transport == null) {
         transport = new LedgerTransportTEEProxy(context);
      }
      return transport;
   }

   @Override
   public boolean isPluggedIn() {
      return true;
   }

   public static boolean isServiceAvailable(final Context context) {
      LedgerTransportTEEProxyFactory ledgerTransportTEEProxyFactory = new LedgerTransportTEEProxyFactory(context);
      try {
         return ledgerTransportTEEProxyFactory.connect(context, null);
      } finally {
         ledgerTransportTEEProxyFactory.close(context);
      }
   }

   private void close(final Context context) {
      context.unbindService(serviceConnection);
   }

   @Override
   public boolean connect(final Context context, final BTChipTransportFactoryCallback callback) {
      if (service != null) {
         callback.onConnected(true);
         return true;
      }
      this.callback = callback;
      Intent intent = new Intent(ILedgerWalletService.class.getName());
      intent.setPackage("com.ledger.wallet.service");
      try {
         boolean result = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
         Log.d(TAG, "Request to bind service " + result);
         return result;
      } catch (Exception e) {
         Log.d(TAG, "Error binding service", e);
         return false;
      }
   }
}

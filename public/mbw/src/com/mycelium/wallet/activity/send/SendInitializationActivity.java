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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;
import com.mycelium.wallet.Wallet.WalletUpdateHandler;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;

public class SendInitializationActivity extends Activity {

   private AsyncTask _task;
   private MbwManager _mbwManager;
   private Wallet _wallet;
   private Long _amountToSend;
   private Address _receivingAddress;
   private boolean _isColdStorage;
   private Handler _synchnozingHandler;
   private Handler _slowNetworkHandler;
   private SpendableOutputs _spendable;
   private Double _oneBtcInFiat;

   public static void callMe(Activity currentActivity, Wallet wallet, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendInitializationActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("isColdStorage", isColdStorage);
      currentActivity.startActivity(intent);
   }

   public static void callMe(Activity currentActivity, Wallet wallet, Long amountToSend, Address receivingAddress, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendInitializationActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      intent.putExtra("isColdStorage", isColdStorage);
      currentActivity.startActivity(intent);
   }
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_initialization_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _wallet = (Wallet) getIntent().getSerializableExtra("wallet");
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");
      _isColdStorage = getIntent().getBooleanExtra("isColdStorage", false);
      
      // Synchronize wallet
      _task = _wallet.requestUpdate(_mbwManager.getBlockChainAddressTracker(), new MyWalletUpdateHandler());
   }

   @Override
   protected void onResume() {
      _synchnozingHandler = new Handler();
      _synchnozingHandler.postDelayed(showSynchronizing, 2000);
      _slowNetworkHandler = new Handler();
      _slowNetworkHandler.postDelayed(showSlowNetwork, 6000);
      super.onResume();
   }

   @Override
   protected void onPause() {
      if (_synchnozingHandler != null) {
         _synchnozingHandler.removeCallbacks(showSynchronizing);
      }
      if (_slowNetworkHandler != null) {
         _slowNetworkHandler.removeCallbacks(showSlowNetwork);
      }
      super.onPause();
   };

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   private Runnable showSynchronizing = new Runnable() {

      @Override
      public void run() {
         findViewById(R.id.tvSynchronizing).setVisibility(View.VISIBLE);
      }
   };

   private Runnable showSlowNetwork = new Runnable() {

      @Override
      public void run() {
         findViewById(R.id.tvSlowNetwork).setVisibility(View.VISIBLE);
      }
   };

   class MyWalletUpdateHandler implements WalletUpdateHandler {

      @Override
      public void walletUpdatedCallback(Wallet wallet, boolean success) {
         Activity me = SendInitializationActivity.this;
         if (!success) {
            Utils.toastConnectionError(me);
            _task = null;
            me.finish();
         } else {
            _spendable = wallet.getLocalSpendableOutputs(_mbwManager.getBlockChainAddressTracker());
            AndroidAsyncApi api = _mbwManager.getAsyncApi();
            _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());
         }
      }

   }

   class QueryExchangeSummaryHandler implements AbstractCallbackHandler<ExchangeSummary[]> {

      @Override
      public void handleCallback(ExchangeSummary[] response, ApiError exception) {
         _task = null;
         Activity me = SendInitializationActivity.this;
         if (exception != null) {
            // Bail out
            Utils.toastConnectionError(me);
         } else {
            _oneBtcInFiat = Utils.getLastTrade(response); // May return null
            // Call next activity
            SendMainActivity.callMe(me, _wallet, _spendable, _oneBtcInFiat, _amountToSend, _receivingAddress, _isColdStorage);
         }
         finish();
      }
   }

}
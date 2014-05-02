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
import android.view.Window;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.ExchangeRate;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;
import com.mycelium.wallet.event.BlockchainError;
import com.mycelium.wallet.event.BlockchainReady;
import com.squareup.otto.Subscribe;

public class SendInitializationActivity extends Activity {
   private MbwManager _mbwManager;
   private Wallet _wallet;
   private Long _amountToSend;
   private Address _receivingAddress;
   private boolean _isColdStorage;
   private Double _oneBtcInFiat;
   private Handler _synchnozingHandler;
   private Handler _slowNetworkHandler;
   private SpendableOutputs _spendable;
   private boolean _done;
   private boolean _ignoreExchangeRates;

   public static void callMe(Activity currentActivity, Wallet wallet, boolean isColdStorage, Double oneBtcInFiat) {
      Intent intent = new Intent(currentActivity, SendInitializationActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("isColdStorage", isColdStorage);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   public static void callMeWithResult(Activity currentActivity, Wallet wallet, Long amountToSend,
         Address receivingAddress, boolean isColdStorage, int request) {
      Intent intent = prepareSendingIntent(currentActivity, wallet, amountToSend, receivingAddress, isColdStorage);
      currentActivity.startActivityForResult(intent, request);

   }

   public static void callMe(Activity currentActivity, Wallet wallet, Long amountToSend, Address receivingAddress,
         boolean isColdStorage) {
      Intent intent = prepareSendingIntent(currentActivity, wallet, amountToSend, receivingAddress, isColdStorage);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   private static Intent prepareSendingIntent(Activity currentActivity, Wallet wallet, Long amountToSend,
         Address receivingAddress, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, SendInitializationActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      intent.putExtra("isColdStorage", isColdStorage);
      return intent;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.send_initialization_activity);
      _mbwManager = MbwManager.getInstance(getApplication());
      // Get intent parameters
      _wallet = (Wallet) getIntent().getSerializableExtra("wallet");
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");
      // May be null
      _oneBtcInFiat = (Double) getIntent().getSerializableExtra("oneBtcInFiat");
      _isColdStorage = getIntent().getBooleanExtra("isColdStorage", false);

      // Load saved state if any
      if (savedInstanceState != null) {
         _oneBtcInFiat = (Double) savedInstanceState.getSerializable("oneBtcInFiat");
         _spendable = (SpendableOutputs) savedInstanceState.getSerializable("spendable");
         _done = savedInstanceState.getBoolean("done");
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("oneBtcInFiat", _oneBtcInFiat);
      outState.putSerializable("spendable", _spendable);
      outState.putBoolean("done", _done);
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);
      _mbwManager.getExchamgeRateManager().subscribe(excahngeSubscriber);
      _synchnozingHandler = new Handler();
      _synchnozingHandler.postDelayed(showSynchronizing, 2000);
      _slowNetworkHandler = new Handler();
      _slowNetworkHandler.postDelayed(showSlowNetwork, 6000);
      continueIfDoneOrSynchronize();
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
      _mbwManager.getExchamgeRateManager().unsubscribe(excahngeSubscriber);
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
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

   @Subscribe
   public void blockChainReady(BlockchainReady blockchainReady) {
      _spendable = _wallet.getLocalSpendableOutputs(_mbwManager.getBlockChainAddressTracker());
      // Check whether we are done
      continueIfDoneOrSynchronize();
   }

   @Subscribe
   public void syncFailed(BlockchainError blockchainError) {
      Utils.toastConnectionError(this);
      finish();
   }

   private ExchangeRateManager.EventSubscriber excahngeSubscriber = new ExchangeRateManager.EventSubscriber(
         new Handler()) {

      @Override
      public void refreshingExchangeRatesFailed() {
         Utils.toastConnectionError(SendInitializationActivity.this);
         finish();
      }

      @Override
      public void refreshingEcahngeRatesSuccedded() {
         ExchangeRate rate = _mbwManager.getExchamgeRateManager().getExchangeRate();
         if (rate != null) {
            _oneBtcInFiat = rate.price; // price may still be null, in that case
                                        // we continue without
            _ignoreExchangeRates = true;
         }
         // Check whether we are done
         continueIfDoneOrSynchronize();
      }
   };

   private void continueIfDoneOrSynchronize() {
      if (_done) {
         return;
      }
      if (_spendable == null) {
         // Request block chain sync
         _wallet.requestUpdate(_mbwManager.getBlockChainAddressTracker());
         return;
      }

      if (_oneBtcInFiat == null && !_ignoreExchangeRates) {
         ExchangeRate rate = _mbwManager.getExchamgeRateManager().getExchangeRate();
         if (rate == null) {
            // We need a refresh
            _mbwManager.getExchamgeRateManager().requestRefresh();
            return;
         }
         _oneBtcInFiat = rate.price; // price may still be null, in that case we
                                     // just proceed and do without
      }

      // We are done call next activity
      _done = true;
      if (_isColdStorage) {
         ColdStorageSummaryActivity.callMe(this, _wallet, _spendable, _oneBtcInFiat);
      } else {
         SendMainActivity.callMe(this, _wallet, _spendable, _oneBtcInFiat, _amountToSend, _receivingAddress,
               _isColdStorage);
      }
      finish();
   }
}

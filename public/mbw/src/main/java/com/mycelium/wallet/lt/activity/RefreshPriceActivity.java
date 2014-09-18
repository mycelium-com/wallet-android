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

package com.mycelium.wallet.lt.activity;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.AssessBtcSellPrice;

public class RefreshPriceActivity extends Activity {

   public static void callMeForResult(Activity currentActivity, TradeSession tradeSession, int requestCode) {
      Intent intent = new Intent(currentActivity, RefreshPriceActivity.class);
      intent.putExtra("tradeSession", tradeSession);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private TradeSession _tradeSession;
   private Button _btRefresh;
   private BtcSellPrice _newBtcSellPrice;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_refresh_price_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _btRefresh = (Button) findViewById(R.id.btRefresh);
      _btRefresh.setOnClickListener(refreshClickListener);

      // Load intent arguments
      _tradeSession = (TradeSession) getIntent().getSerializableExtra("tradeSession");

      fetchNewPrice();
   }

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      updateUi();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private void fetchNewPrice() {
      BtcSellPriceParameters params = new BtcSellPriceParameters(_tradeSession.ownerId, _tradeSession.peerId,
            _tradeSession.currency, _tradeSession.fiatTraded, _tradeSession.priceFormula.id, _tradeSession.premium);
      AssessBtcSellPrice request = new AssessBtcSellPrice(params);
      _ltManager.makeRequest(request);
   }

   private OnClickListener refreshClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         enableUi(false);
         Intent result = new Intent();
         setResult(RESULT_OK, result);
         finish();
      }
   };

   private void enableUi(boolean enabled) {
      _btRefresh.setEnabled(enabled);
   }

   private void updateUi() {
      populateLabels();
      populateNewPrices();
      populateCurrentPrices();
   }

   private void populateCurrentPrices() {
      Locale locale = new Locale("en", "US");

      long mySatoshisTraded;
      if (_tradeSession.isBuyer) {
         mySatoshisTraded = _tradeSession.satoshisForBuyer;
      } else {
         mySatoshisTraded = _tradeSession.satoshisFromSeller;
      }

      // My Price
      ((TextView) findViewById(R.id.myCurrentPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded,
            mySatoshisTraded, _tradeSession.currency));

      String fiatAmountString = String.format(locale, "%s %s", _tradeSession.fiatTraded, _tradeSession.currency);

      // You Pay / Get Values
      String myBtcAmountString = _mbwManager.getBtcValueString(mySatoshisTraded);
      if (_tradeSession.isBuyer) {
         ((TextView) findViewById(R.id.myCurrentPay)).setText(fiatAmountString);
         ((TextView) findViewById(R.id.myCurrentGet)).setText(myBtcAmountString);
      } else {
         ((TextView) findViewById(R.id.myCurrentPay)).setText(myBtcAmountString);
         ((TextView) findViewById(R.id.myCurrentGet)).setText(fiatAmountString);
      }

   }

   private void populateLabels() {
      // You sell/buy at
      ((TextView) findViewById(R.id.yourPriceLabel)).setText(getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_you_buy_at_label : R.string.lt_you_sell_at_label));
   }

   private void populateNewPrices() {

      if (_newBtcSellPrice == null) {
         ((TextView) findViewById(R.id.myNewPrice)).setText("");
         ((TextView) findViewById(R.id.myNewPay)).setText("");
         ((TextView) findViewById(R.id.myNewGet)).setText("");
         _btRefresh.setEnabled(false);
         return;
      }

      _btRefresh.setEnabled(true);

      Locale locale = new Locale("en", "US");

      long mySatoshisTraded;
      if (_tradeSession.isBuyer) {
         mySatoshisTraded = _newBtcSellPrice.satoshisForBuyer;
      } else {
         mySatoshisTraded = _newBtcSellPrice.satoshisFromSeller;
      }

      // My Price
      ((TextView) findViewById(R.id.myNewPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded, mySatoshisTraded,
            _tradeSession.currency));

      String fiatAmountString = String.format(locale, "%s %s", _tradeSession.fiatTraded, _tradeSession.currency);

      // You Pay / Get Values
      String myBtcAmountString = _mbwManager.getBtcValueString(mySatoshisTraded);
      if (_tradeSession.isBuyer) {
         ((TextView) findViewById(R.id.myNewPay)).setText(fiatAmountString);
         ((TextView) findViewById(R.id.myNewGet)).setText(myBtcAmountString);
      } else {
         ((TextView) findViewById(R.id.myNewPay)).setText(myBtcAmountString);
         ((TextView) findViewById(R.id.myNewGet)).setText(fiatAmountString);
      }

   }

   private String getBtcPriceString(int fiatTraded, long satoshis, String currency) {
      double oneBtcPrice = (double) fiatTraded * Constants.ONE_BTC_IN_SATOSHIS / (double) satoshis;
      String price = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, oneBtcPrice);
      return this.getResources().getString(R.string.lt_btc_price, price, currency);
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         Toast.makeText(RefreshPriceActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         finish();
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(RefreshPriceActivity.this);
         finish();
         return true;
      }

      @Override
      public void onLtBtcSellPriceAssesed(BtcSellPrice btcSellPrice, AssessBtcSellPrice request) {
         _newBtcSellPrice = btcSellPrice;
         updateUi();
      }
   };

}
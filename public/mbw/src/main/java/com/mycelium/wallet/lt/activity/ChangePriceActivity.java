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

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.TradeSession;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.lt.api.params.TradeChangeParameters;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.LtAndroidUtils.PremiumChoice;
import com.mycelium.wallet.lt.LtAndroidUtils.PriceFormulaChoice;
import com.mycelium.wallet.lt.api.AssessBtcSellPrice;
import com.mycelium.wallet.lt.api.GetPriceFormulas;

public class ChangePriceActivity extends Activity {

   public static final String RESULT_STRING = "result";

   public static void callMeForResult(Activity currentActivity, TradeSession tradeSession, int requestCode) {
      Intent intent = new Intent(currentActivity, ChangePriceActivity.class);
      intent.putExtra("tradeSession", tradeSession);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private TradeSession _tradeSession;
   private Spinner _spPriceFormula;
   private Spinner _spPremium;
   private Button _btChange;
   private ArrayList<PriceFormula> _priceFormulas;
   private BtcSellPrice _newBtcSellPrice;

   /** Called when the activity is first created. */
   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_change_price_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _spPriceFormula = (Spinner) findViewById(R.id.spPriceFormula);
      _spPremium = (Spinner) findViewById(R.id.spPremium);
      _btChange = (Button) findViewById(R.id.btChangePrice);
      _btChange.setOnClickListener(changeClickListener);

      // Load intent arguments
      _tradeSession = (TradeSession) getIntent().getSerializableExtra("tradeSession");

      double premium = _tradeSession.premium;
      PriceFormula priceFormula = _tradeSession.priceFormula;

      // Load saved state
      if (savedInstanceState != null) {
         _priceFormulas = (ArrayList<PriceFormula>) savedInstanceState.getSerializable("priceformulas");
         if (_priceFormulas != null) {
            priceFormula = (PriceFormula) savedInstanceState.getSerializable("priceFormula");
         }
         premium = savedInstanceState.getInt("premium");
      }
      LtAndroidUtils.populatePremiumSpinner(this, _spPremium, premium);
      if (_priceFormulas != null) {
         LtAndroidUtils.populatePriceFormulaSpinner(this, _spPriceFormula, _priceFormulas, priceFormula);
         fetchNewPrice();
      }

      _spPremium.setOnItemSelectedListener(spinnerItemSelected);
      _spPriceFormula.setOnItemSelectedListener(spinnerItemSelected);

   }

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      if (_priceFormulas == null) {
         _ltManager.makeRequest(new GetPriceFormulas());
      } else {
         updateUi();
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("priceFormula", getSelectedPriceFormula());
      if (_priceFormulas != null) {
         outState.putSerializable("priceformulas", _priceFormulas);
      }
      outState.putDouble("premium", getSelectedPremium());
      super.onSaveInstanceState(outState);
   }

   private void fetchNewPrice() {
      PriceFormula priceFormula = getSelectedPriceFormula();
      Double premium = getSelectedPremium();
      if (priceFormula == null || premium == null) {
         return;
      } else {
         BtcSellPriceParameters params = new BtcSellPriceParameters(_tradeSession.ownerId, _tradeSession.peerId,
               _tradeSession.currency, _tradeSession.fiatTraded, priceFormula.id, premium);
         AssessBtcSellPrice request = new AssessBtcSellPrice(params);
         _ltManager.makeRequest(request);
         _spPriceFormula.setEnabled(false);
         _spPremium.setEnabled(false);
      }
   }

   private PriceFormula getSelectedPriceFormula() {
      PriceFormulaChoice p = (PriceFormulaChoice) _spPriceFormula.getSelectedItem();
      if (p == null) {
         return null;
      }
      return p.formula;
   }

   private double getSelectedPremium() {
      return ((PremiumChoice) _spPremium.getSelectedItem()).premium;
   }

   private OnClickListener changeClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         enableUi(false);
         final PriceFormula selectedPriceFormula = getSelectedPriceFormula();
         if (selectedPriceFormula == null){
            // cancel if we dont have a price formula available - user can go back and retry it, but dont crash
            return;
         }
         String priceFormulaId = Preconditions.checkNotNull(selectedPriceFormula).id;
         double premium = Preconditions.checkNotNull(getSelectedPremium());
         TradeChangeParameters params = new TradeChangeParameters(_tradeSession.id, priceFormulaId, premium);
         Intent result = new Intent();
         result.putExtra(RESULT_STRING, params);
         setResult(RESULT_OK, result);
         finish();
      }
   };

   private void enableUi(boolean enabled) {
      _spPriceFormula.setEnabled(enabled);
      _btChange.setEnabled(enabled);
   }

   private void updateUi() {

      if (_priceFormulas == null) {
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.svForm).setVisibility(View.GONE);
      } else {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.svForm).setVisibility(View.VISIBLE);
      }
      populateLabels();
      populateNewPrices();
      populateCurrentPrices();
   }

   private void populateCurrentPrices() {
      Locale locale = new Locale("en", "US");

      // Market Price
      ((TextView) findViewById(R.id.currentMarketPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded,
            _tradeSession.satoshisAtMarketPrice, _tradeSession.currency));

      long mySatoshisTraded;
      long foreignSatoshisTraded;
      if (_tradeSession.isBuyer) {
         mySatoshisTraded = _tradeSession.satoshisForBuyer;
         foreignSatoshisTraded = _tradeSession.satoshisFromSeller;
      } else {
         mySatoshisTraded = _tradeSession.satoshisFromSeller;
         foreignSatoshisTraded = _tradeSession.satoshisForBuyer;
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

      // Foreign Price
      ((TextView) findViewById(R.id.foreignCurrentPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded,
            foreignSatoshisTraded, _tradeSession.currency));

      // Foreign Pay / Get Values
      String foreignBtcAmountString = _mbwManager.getBtcValueString(foreignSatoshisTraded);
      if (_tradeSession.isBuyer) {
         ((TextView) findViewById(R.id.foreignCurrentPay)).setText(foreignBtcAmountString);
         ((TextView) findViewById(R.id.foreignCurrentGet)).setText(fiatAmountString);
      } else {
         ((TextView) findViewById(R.id.foreignCurrentPay)).setText(fiatAmountString);
         ((TextView) findViewById(R.id.foreignCurrentGet)).setText(foreignBtcAmountString);
      }

      /*
       * Removing commission until we figure out how we want to display it
       * 
      long commission = _tradeSession.satoshisFromSeller - _tradeSession.satoshisForBuyer;
      String commissionString = _mbwManager.getBtcValueString(commission);
      ((TextView) findViewById(R.id.currentCommission)).setText(commissionString);
      */
   }

   private void populateLabels() {
      // You sell/buy at
      ((TextView) findViewById(R.id.yourPriceLabel)).setText(getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_you_buy_at_label : R.string.lt_you_sell_at_label));

      // Buyer/Seller's price
      ((TextView) findViewById(R.id.foreignPriceLabel)).setText(getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_seller_sells_at_label : R.string.lt_buyer_buys_at_label));

      // Buyer/Seller pays
      ((TextView) findViewById(R.id.foreignPaysLabel)).setText(getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_seller_pays_label : R.string.lt_buyer_pays_label));

      // Buyer/Seller gets
      ((TextView) findViewById(R.id.foreignGetsLabel)).setText(getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_seller_gets_label : R.string.lt_buyer_gets_label));

   }

   private void populateNewPrices() {

      if (_newBtcSellPrice == null) {
         // XXX show spinner
         ((TextView) findViewById(R.id.newMarketPrice)).setText("");
         ((TextView) findViewById(R.id.myNewPrice)).setText("");
         ((TextView) findViewById(R.id.myNewPay)).setText("");
         ((TextView) findViewById(R.id.myNewGet)).setText("");
         /*
          * Removing commission until we figure out how we want to display it
          * 
         ((TextView) findViewById(R.id.newCommission)).setText("");
         ((TextView) findViewById(R.id.commissionDescription)).setText("");
         */
         _btChange.setEnabled(false);
         return;
      }

      _btChange.setEnabled(true);

      Locale locale = new Locale("en", "US");

      // Market Price
      ((TextView) findViewById(R.id.newMarketPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded,
            _newBtcSellPrice.satoshisAtMarketPrice, _tradeSession.currency));

      long mySatoshisTraded;
      long foreignSatoshisTraded;
      if (_tradeSession.isBuyer) {
         mySatoshisTraded = _newBtcSellPrice.satoshisForBuyer;
         foreignSatoshisTraded = _newBtcSellPrice.satoshisFromSeller;
      } else {
         mySatoshisTraded = _newBtcSellPrice.satoshisFromSeller;
         foreignSatoshisTraded = _newBtcSellPrice.satoshisForBuyer;
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

      // Foreign Price
      ((TextView) findViewById(R.id.foreignNewPrice)).setText(getBtcPriceString(_tradeSession.fiatTraded,
            foreignSatoshisTraded, _tradeSession.currency));

      // Foreign Pay / Get Values
      String foreignBtcAmountString = _mbwManager.getBtcValueString(foreignSatoshisTraded);
      if (_tradeSession.isBuyer) {
         ((TextView) findViewById(R.id.foreignNewPay)).setText(foreignBtcAmountString);
         ((TextView) findViewById(R.id.foreignNewGet)).setText(fiatAmountString);
      } else {
         ((TextView) findViewById(R.id.foreignNewPay)).setText(fiatAmountString);
         ((TextView) findViewById(R.id.foreignNewGet)).setText(foreignBtcAmountString);
      }

      /*
       * Removing commission until we figure out how we want to display it
       * 
      // Commission
      long commission = _newBtcSellPrice.satoshisFromSeller - _newBtcSellPrice.satoshisForBuyer;
      String commissionString = _mbwManager.getBtcValueString(commission);
      ((TextView) findViewById(R.id.newCommission)).setText(commissionString);

      // Commission description
      Double commissionPercent = roundDoubleHalfUp(
            calculateCommissionPercent(_newBtcSellPrice.satoshisFromSeller, _newBtcSellPrice.satoshisForBuyer), 2);
      String commissionDesc = getResources().getString(
            _tradeSession.isBuyer ? R.string.lt_buy_commission_description : R.string.lt_sell_commission_description,
            commissionPercent.toString());
      ((TextView) findViewById(R.id.commissionDescription)).setText(commissionDesc);
      */
   }

   /*
   private static double calculateCommissionPercent(long satoshisFromSeller, long satoshisForBuyer) {
      double satoshiCommission = (double) (satoshisFromSeller - satoshisForBuyer);
      double satoshisSent = (double) satoshisFromSeller;
      double commission = satoshiCommission * 100 / satoshisSent;
      return commission;
   }

   private static double roundDoubleHalfUp(double value, int decimals) {
      return BigDecimal.valueOf(value).setScale(decimals, BigDecimal.ROUND_HALF_UP).doubleValue();
   }
  */
   
   private String getBtcPriceString(int fiatTraded, long satoshis, String currency) {
      double oneBtcPrice = (double) fiatTraded * Constants.ONE_BTC_IN_SATOSHIS / (double) satoshis;
      String price = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, oneBtcPrice);
      return this.getResources().getString(R.string.lt_btc_price, price, currency);
   }

   OnItemSelectedListener spinnerItemSelected = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         fetchNewPrice();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
         fetchNewPrice();
      }
   };

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         Toast.makeText(ChangePriceActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         finish();
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(ChangePriceActivity.this);
         finish();
         return true;
      }

      @Override
      public void onLtPriceFormulasFetched(java.util.List<PriceFormula> priceFormulas, GetPriceFormulas request) {
         _priceFormulas = new ArrayList<PriceFormula>(priceFormulas);
         LtAndroidUtils.populatePriceFormulaSpinner(ChangePriceActivity.this, _spPriceFormula, _priceFormulas,
               _tradeSession.priceFormula);
         fetchNewPrice();
         updateUi();
      }

      @Override
      public void onLtBtcSellPriceAssesed(BtcSellPrice btcSellPrice, AssessBtcSellPrice request) {
         _newBtcSellPrice = btcSellPrice;
         _spPriceFormula.setEnabled(true);
         _spPremium.setEnabled(true);
         updateUi();
      }
   };

}
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

package com.mycelium.wallet.lt.activity.sell;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.model.Ad;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.BtcSellPrice;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.params.BtcSellPriceParameters;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.EnterTextDialog;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.LtAndroidConstants;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.LtAndroidUtils.PremiumChoice;
import com.mycelium.wallet.lt.LtAndroidUtils.PriceFormulaChoice;
import com.mycelium.wallet.lt.activity.ChangeLocationActivity;
import com.mycelium.wallet.lt.activity.EnterFiatAmountActivity;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.AssessBtcSellPrice;
import com.mycelium.wallet.lt.api.CreateAd;
import com.mycelium.wallet.lt.api.EditAd;
import com.mycelium.wallet.lt.api.GetPriceFormulas;
import com.mycelium.wallet.lt.api.Request;

public class CreateOrEditAdActivity extends Activity {

   private static final int CHANGE_LOCATION_REQUEST_CODE = 0;
   private static final int ENTER_MAX_AMOUNT_REQUEST_CODE = 1;
   private static final int ENTER_MIN_AMOUNT_REQUEST_CODE = 2;
   private static final int GET_CURRENCY_RESULT_CODE = 3;

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, CreateOrEditAdActivity.class);
      currentActivity.startActivity(intent);
   }

   public static void callMe(Activity currentActivity, Ad ad) {
      Intent intent = new Intent(currentActivity, CreateOrEditAdActivity.class);
      intent.putExtra("ad", ad);
      currentActivity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Spinner _spPriceFormula;
   private Spinner _spPremium;
   private Spinner _spAdType;
   private Button _btCreate;
   private TextView _tvDescription;
   private TextView _tvMinAmount;
   private TextView _tvMaxAmount;
   private ArrayList<PriceFormula> _priceFormulas;
   private Ad _ad;
   private GpsLocation _location;
   private String _currency;
   private int _minAmount;
   private int _maxAmount;
   // hack because the select is fired automatically on startup
   private boolean isFirstAdTypeSelect;
   private BtcSellPrice _btcPrice;
   private boolean _isFetchingPrice;

   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_or_edit_ad_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _spAdType = (Spinner) findViewById(R.id.spAdType);
      _spPriceFormula = (Spinner) findViewById(R.id.spPriceFormula);
      _spPremium = (Spinner) findViewById(R.id.spPremium);
      Button btChange = (Button) findViewById(R.id.btChange);
      btChange.setOnClickListener(changeClickListener);
      Button btCurrency = (Button) findViewById(R.id.btCurrency);
      btCurrency.setOnClickListener(currencyClickListener);
      Button btEdit = (Button) findViewById(R.id.btEdit);
      btEdit.setOnClickListener(editClickListener);
      _btCreate = (Button) findViewById(R.id.btCreate);
      _btCreate.setOnClickListener(createOrEditClickListener);
      _tvMinAmount = (TextView) findViewById(R.id.tvMinAmount);
      _tvMaxAmount = (TextView) findViewById(R.id.tvMaxAmount);
      findViewById(R.id.btEditMin).setOnClickListener(editMinAmountClickListener);
      findViewById(R.id.btEditMax).setOnClickListener(editMaxAmountClickListener);
      _tvDescription = (TextView) findViewById(R.id.tvDescription);

      _ad = (Ad) getIntent().getSerializableExtra("ad");

      AdType adType = isEdit() ? _ad.type : AdType.SELL_BTC;
      double premium = isEdit() ? _ad.premium : LtAndroidConstants.DEFAULT_PREMIUM;
      _minAmount = isEdit() ? _ad.minimumFiat : -1;
      _maxAmount = isEdit() ? _ad.maximumFiat : -1;
      String description = isEdit() ? _ad.description : null;
      PriceFormula priceFormula = isEdit() ? _ad.priceFormula : null;
      _location = isEdit() ? _ad.location : _ltManager.getUserLocation();
      _currency = isEdit() ? _ad.currency : _mbwManager.getFiatCurrency();
      if (_currency.equals("")) {
         //lt without fiat is pointless, if there is none, revert to usd
         _currency = "USD";
      }
      // Load saved state
      if (savedInstanceState != null) {
         adType = AdType.values()[savedInstanceState.getInt("adType")];
         _priceFormulas = (ArrayList<PriceFormula>) savedInstanceState.getSerializable("priceformulas");
         if (_priceFormulas != null) {
            priceFormula = (PriceFormula) savedInstanceState.getSerializable("priceFormula");
         }
         premium = savedInstanceState.getInt("premium", Spinner.INVALID_POSITION);
         _minAmount = savedInstanceState.getInt("minAmount", -1);
         _maxAmount = savedInstanceState.getInt("maxAmount", -1);
         description = savedInstanceState.getString("description");
         _currency = savedInstanceState.getString("currency");
      }

      // Populate premium
      LtAndroidUtils.populatePremiumSpinner(this, _spPremium, premium);

      // Populate description
      if (description != null) {
         _tvDescription.setText(description);
      }

      // Populate price formulas
      if (_priceFormulas != null) {
         LtAndroidUtils.populatePriceFormulaSpinner(this, _spPriceFormula, _priceFormulas, priceFormula);
      }

      // Populate Ad Type
      List<String> adTypes = new ArrayList<String>();
      adTypes.add(getString(R.string.lt_ad_type_sell_btc_label));
      adTypes.add(getString(R.string.lt_ad_type_buy_btc_label));
      ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, adTypes);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      _spAdType.setAdapter(dataAdapter);
      _spAdType.setSelection(adType == AdType.SELL_BTC ? 0 : 1);
      _spAdType.setOnItemSelectedListener(adTypeChanged);
      isFirstAdTypeSelect = true;
      _spPriceFormula.setOnItemSelectedListener(priceFormulaSelected);

      _spPremium.setOnItemSelectedListener(premiumSelected);

      // Set title
      ((TextView) findViewById(R.id.tvTitle)).setText(isEdit() ? R.string.lt_edit_ad_title
            : R.string.lt_create_ad_title);
      _btCreate.setText(isEdit() ? R.string.lt_done_button : R.string.lt_create_button);
      enableUi();
   }

   OnItemSelectedListener adTypeChanged = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

         // Avoid the first time it gets triggered (always happens on create)
         if (isFirstAdTypeSelect) {
            isFirstAdTypeSelect = false;
            return;
         }

         // Negate the premium automatically
         LtAndroidUtils.populatePremiumSpinner(CreateOrEditAdActivity.this, _spPremium, -getSelectedPremium());

         // Fade in/out
         fadeView(_spPremium);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
         // Ignore
      }
   };

   private boolean isEdit() {
      return _ad != null;
   }

   private void fadeView(View view) {
      // Change alpha from fully visible to invisible
      final Animation animation = new AlphaAnimation(1, 0);
      animation.setDuration(500); // duration - half a second
      animation.setInterpolator(new LinearInterpolator()); // do not alter
      // animation rate
      animation.setRepeatCount(1); // Repeat animation
      // Reverse animation at the end so the button will fade back in
      animation.setRepeatMode(Animation.REVERSE);
      view.startAnimation(animation);
   }

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      updateUi();
      if (_priceFormulas == null) {
         _ltManager.makeRequest(new GetPriceFormulas());
      } else {
         fetchNewPrice();
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
      outState.putInt("adType", getSelectedAdType().ordinal());
      outState.putSerializable("priceFormula", getSelectedPriceFormula());
      if (_priceFormulas != null) {
         outState.putSerializable("priceformulas", _priceFormulas);
      }
      outState.putDouble("premium", getSelectedPremium());
      outState.putString("currency", getCurrency());
      outState.putInt("minAmount", getMinAmount());
      outState.putInt("maxAmount", getMaxAmount());
      outState.putString("description", getDescription());
      super.onSaveInstanceState(outState);
   }

   private boolean validateValues() {
      if (_spPriceFormula.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
         return false;
      }
      if (_spPremium.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
         return false;
      }
      if (getMinAmount() < 1) {
         return false;
      }
      if (getMaxAmount() < 1) {
         return false;
      }
      if (getMinAmount() > getMaxAmount()) {
         return false;
      }
      return true;
   }

   private AdType getSelectedAdType() {
      return _spAdType.getSelectedItemPosition() == 0 ? AdType.SELL_BTC : AdType.BUY_BTC;
   }

   private PriceFormula getSelectedPriceFormula() {
      PriceFormulaChoice p = (PriceFormulaChoice) _spPriceFormula.getSelectedItem();
      if (p == null) {
         return null;
      }
      return p.formula;
   }

   private double getSelectedPremium() {
      PremiumChoice p = (PremiumChoice) _spPremium.getSelectedItem();
      if (p == null) {
         return 0;
      }
      return p.premium;
   }

   private String getCurrency() {
      return _currency;
   }

   private int getMinAmount() {
      return _minAmount;
   }

   private int getMaxAmount() {
      return _maxAmount;
   }

   private String getDescription() {
      return _tvDescription.getText().toString();
   }

   private OnClickListener editMinAmountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         EnterFiatAmountActivity.callMe(CreateOrEditAdActivity.this, _currency, _minAmount == -1 ? null : _minAmount,
               ENTER_MIN_AMOUNT_REQUEST_CODE);
      }
   };

   private OnClickListener editMaxAmountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         EnterFiatAmountActivity.callMe(CreateOrEditAdActivity.this, _currency, _maxAmount == -1 ? null : _maxAmount,
               ENTER_MAX_AMOUNT_REQUEST_CODE);
      }
   };

   private OnClickListener changeClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         // Change the current location
         ChangeLocationActivity.callMeForResult(CreateOrEditAdActivity.this, _location, !isEdit(),
               CHANGE_LOCATION_REQUEST_CODE);
      }
   };

   private OnClickListener currencyClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _currency = _mbwManager.getNextCurrency(false);
         fetchNewPrice();
         updateUi();
      }
   };

   private OnClickListener editClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         doEditDescription();
      }
   };

   private OnClickListener createOrEditClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         String priceFormulaId = Preconditions.checkNotNull(getSelectedPriceFormula()).id;
         Request request;
         String title;
         if (isEdit()) {
            request = new EditAd(_ad.id, getSelectedAdType(), _location, _currency, getMinAmount(), getMaxAmount(),
                  priceFormulaId, getSelectedPremium(), getDescription());

            title = getResources().getString(R.string.lt_edit_ad_title);
         } else {
            request = new CreateAd(getSelectedAdType(), _location, _currency, getMinAmount(), getMaxAmount(),
                  priceFormulaId, getSelectedPremium(), getDescription());
            title = getResources().getString(R.string.lt_create_ad_title);

         }
         SendRequestActivity.callMe(CreateOrEditAdActivity.this, request, title);
         finish();
      }
   };

   private void enableUi() {
      _spPriceFormula.setEnabled(_priceFormulas != null);
      _btCreate.setEnabled(validateValues());
   }

   private void updateUi() {

      // Set amount hints
      _tvMinAmount.setHint(String.format("%s %s", Integer.toString(10), _currency));
      _tvMaxAmount.setHint(String.format("%s %s", Integer.toString(1000), _currency));

      if (_priceFormulas == null) {
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.svForm).setVisibility(View.GONE);
      } else {
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.svForm).setVisibility(View.VISIBLE);
         if (_minAmount != -1) {
            _tvMinAmount.setText(String.format("%s %s", Integer.toString(_minAmount), _currency));
         }
         if (_maxAmount != -1) {
            _tvMaxAmount.setText(String.format("%s %s", Integer.toString(_maxAmount), _currency));
         }

         // Set the approximate BTC price
         if (_btcPrice == null) {
            ((TextView) findViewById(R.id.tvFiatPrice)).setText(R.string.question_mark);
         } else {
            String price = getBtcPriceString(_btcPrice.fiatTraded, _btcPrice.satoshisForBuyer, _btcPrice.currency);
            ((TextView) findViewById(R.id.tvFiatPrice)).setText(price);
         }
         ((TextView) findViewById(R.id.tvLocation)).setText(_location.name);
         ((Button) findViewById(R.id.btCurrency)).setText(getCurrency());

      }
   }

   private String getBtcPriceString(int fiatTraded, long satoshis, String currency) {
      double oneBtcPrice = (double) fiatTraded * Constants.ONE_BTC_IN_SATOSHIS / (double) satoshis;
      String price = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, oneBtcPrice);
      return price + " " + currency;
   }

   private void fetchNewPrice() {
      _btcPrice = null;
      PriceFormula priceFormula = getSelectedPriceFormula();
      double premium = getSelectedPremium();
      if (priceFormula == null) {
         return;
      }
      if (_isFetchingPrice) {
         return;
      }
      _isFetchingPrice = true;
      BtcSellPriceParameters params = new BtcSellPriceParameters(_ltManager.getLocalTraderAddress(), null,
            getCurrency(), 1000000, priceFormula.id, premium);
      AssessBtcSellPrice request = new AssessBtcSellPrice(params);
      _ltManager.makeRequest(request);

   }

   TextWatcher textWatcher = new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void afterTextChanged(Editable editable) {
         enableUi();
      }
   };

   OnItemSelectedListener premiumSelected = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         fetchNewPrice();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
         fetchNewPrice();
      }
   };

   OnItemSelectedListener priceFormulaSelected = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         fetchNewPrice();
         enableUi();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
         fetchNewPrice();
         enableUi();
      }
   };

   private void doEditDescription() {
      String hint = getResources().getString(R.string.lt_create_order_description_hint);
      String text = _tvDescription.getText().toString();
      EnterTextDialog.show(this, R.string.lt_description_label, hint, text, false,
            new EnterTextDialog.EnterTextHandler() {

               @Override
               public boolean validateTextOnChange(String newText, String oldText) {
                  return newText.length() < LtApi.MAXIMUM_AD_DESCRIPTION_LENGTH;
               }

               @Override
               public boolean validateTextOnOk(String newText, String oldText) {
                  return newText.length() < LtApi.MAXIMUM_AD_DESCRIPTION_LENGTH;
               }

               @Override
               public String getToastTextOnInvalidOk(String newText, String oldText) {
                  return getResources().getString(R.string.lt_create_order_description_too_long);
               }

               @Override
               public void onNameEntered(String newText, String oldText) {
                  _tvDescription.setText(newText);
               }
            });

   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == CHANGE_LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
         _location = (GpsLocation) intent.getSerializableExtra("location");
      } else if (requestCode == ENTER_MAX_AMOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
         _maxAmount = (Integer) intent.getSerializableExtra("amount");
         enableUi();
      } else if (requestCode == ENTER_MIN_AMOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
         _minAmount = (Integer) intent.getSerializableExtra("amount");
         enableUi();
      }
      // else: We didn't like what we got, bail
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         // if the price source is not available, dont close the current activity - just signal it to the user
         if (errorCode == LtApi.ERROR_CODE_PRICE_FORMULA_NOT_AVAILABLE){
            Toast.makeText(CreateOrEditAdActivity.this, R.string.lt_missing_fx_rate, Toast.LENGTH_LONG).show();
            _isFetchingPrice = false;
            _btcPrice = null;
            updateUi();
         } else {
            Toast.makeText(CreateOrEditAdActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
            finish();
         }
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(CreateOrEditAdActivity.this);
         finish();
         return true;
      };

      @Override
      public void onLtPriceFormulasFetched(java.util.List<PriceFormula> priceFormulas, GetPriceFormulas request) {
         _priceFormulas = new ArrayList<PriceFormula>(priceFormulas);
         LtAndroidUtils.populatePriceFormulaSpinner(CreateOrEditAdActivity.this, _spPriceFormula, priceFormulas,
               isEdit() ? _ad.priceFormula : null);
         enableUi();
         fetchNewPrice();
         updateUi();
      };

      @Override
      public void onLtBtcSellPriceAssesed(BtcSellPrice btcSellPrice, AssessBtcSellPrice request) {
         _btcPrice = btcSellPrice;
         _isFetchingPrice = false;
         updateUi();
      };

   };

}

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

package com.mycelium.wallet.lt.activity.sell;

import java.util.ArrayList;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.model.PriceFormula;
import com.mycelium.lt.api.model.SellOrder;
import com.mycelium.wallet.EnterTextDialog;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.settings.SetLocalCurrencyActivity;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.LtAndroidConstants;
import com.mycelium.wallet.lt.LtAndroidUtils;
import com.mycelium.wallet.lt.LtAndroidUtils.PremiumChoice;
import com.mycelium.wallet.lt.LtAndroidUtils.PriceFormulaChoice;
import com.mycelium.wallet.lt.activity.ChangeLocationActivity;
import com.mycelium.wallet.lt.activity.EnterFiatAmountActivity;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.CreateSellOrder;
import com.mycelium.wallet.lt.api.EditSellOrder;
import com.mycelium.wallet.lt.api.GetPriceFormulas;
import com.mycelium.wallet.lt.api.Request;

public class CreateOrEditSellOrderActivity extends Activity {

   private static final int CHANGE_LOCATION_REQUEST_CODE = 0;
   private static final int ENTER_MAX_AMOUNT_REQUEST_CODE = 1;
   private static final int ENTER_MIN_AMOUNT_REQUEST_CODE = 2;
   private static final int GET_CURRENCY_RESULT_CODE = 3;

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, CreateOrEditSellOrderActivity.class);
      currentActivity.startActivity(intent);
   }

   public static void callMe(Activity currentActivity, SellOrder sellOrder) {
      Intent intent = new Intent(currentActivity, CreateOrEditSellOrderActivity.class);
      intent.putExtra("sellOrder", sellOrder);
      currentActivity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Spinner _spPriceFormula;
   private Spinner _spPremium;
   private Button _btCreate;
   private Button _btChange;
   private Button _btCurrency;
   private Button _btEdit;
   private TextView _tvDescription;
   private TextView _tvMinAmount;
   private TextView _tvMaxAmount;
   private ArrayList<PriceFormula> _priceFormulas;
   private SellOrder _sellOrder;
   private GpsLocation _location;
   private String _currency;
   private int _minAmount;
   private int _maxAmount;

   /** Called when the activity is first created. */
   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_or_edit_sell_order_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _spPriceFormula = (Spinner) findViewById(R.id.spPriceFormula);
      _spPremium = (Spinner) findViewById(R.id.spPremium);
      _btChange = (Button) findViewById(R.id.btChange);
      _btChange.setOnClickListener(changeClickListener);
      _btCurrency = (Button) findViewById(R.id.btCurrency);
      _btCurrency.setOnClickListener(currencyClickListener);
      _btEdit = (Button) findViewById(R.id.btEdit);
      _btEdit.setOnClickListener(editClickListener);
      _btCreate = (Button) findViewById(R.id.btCreate);
      _btCreate.setOnClickListener(createOrEditClickListener);
      _tvMinAmount = (TextView) findViewById(R.id.tvMinAmount);
      _tvMaxAmount = (TextView) findViewById(R.id.tvMaxAmount);
      findViewById(R.id.btEditMin).setOnClickListener(editMinAmountClickListener);
      findViewById(R.id.btEditMax).setOnClickListener(editMaxAmountClickListener);
      _tvDescription = (TextView) findViewById(R.id.tvDescription);

      _sellOrder = (SellOrder) getIntent().getSerializableExtra("sellOrder");

      // Populate premium
      double premium = isEdit() ? _sellOrder.premium : LtAndroidConstants.DEFAULT_PREMIUM;
      _minAmount = isEdit() ? _sellOrder.minimumFiat : -1;
      _maxAmount = isEdit() ? _sellOrder.maximumFiat : -1;
      String description = isEdit() ? _sellOrder.description : null;
      PriceFormula priceFormula = isEdit() ? _sellOrder.priceFormula : null;
      _location = isEdit() ? _sellOrder.location : _ltManager.getUserLocation();
      _currency = isEdit() ? _sellOrder.currency : _mbwManager.getFiatCurrency();
      // Load saved state
      if (savedInstanceState != null) {
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

      LtAndroidUtils.populatePremiumSpinner(this, _spPremium, premium);
      if (description != null) {
         _tvDescription.setText(description);
      }
      if (_priceFormulas != null) {
         LtAndroidUtils.populatePriceFormulaSpinner(this, _spPriceFormula, _priceFormulas, priceFormula);
      }

      _spPriceFormula.setOnItemSelectedListener(spinnerItemSelected);

      // Set title
      ((TextView) findViewById(R.id.tvTitle)).setText(isEdit() ? R.string.lt_edit_sell_order_title
            : R.string.lt_create_sell_order_title);
      _btCreate.setText(isEdit() ? R.string.lt_done_button : R.string.lt_create_button);
      enableUi();
   }

   private boolean isEdit() {
      return _sellOrder != null;
   }

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      updateUi();
      if (_priceFormulas == null) {
         _ltManager.makeRequest(new GetPriceFormulas());
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
         EnterFiatAmountActivity.callMe(CreateOrEditSellOrderActivity.this, _currency, _minAmount == -1 ? null
               : _minAmount, ENTER_MIN_AMOUNT_REQUEST_CODE);
      }
   };

   private OnClickListener editMaxAmountClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         EnterFiatAmountActivity.callMe(CreateOrEditSellOrderActivity.this, _currency, _maxAmount == -1 ? null
               : _maxAmount, ENTER_MAX_AMOUNT_REQUEST_CODE);
      }
   };

   private OnClickListener changeClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         // Change the current location
         ChangeLocationActivity.callMeForResult(CreateOrEditSellOrderActivity.this, _location, !isEdit(),
               CHANGE_LOCATION_REQUEST_CODE);
      }
   };

   private OnClickListener currencyClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         SetLocalCurrencyActivity.callMeForResult(CreateOrEditSellOrderActivity.this, _currency,
               GET_CURRENCY_RESULT_CODE);
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
            request = new EditSellOrder(_sellOrder.id, _location, _currency, getMinAmount(), getMaxAmount(),
                  priceFormulaId, getSelectedPremium(), getDescription());

            title = getResources().getString(R.string.lt_edit_sell_order_title);
         } else {
            request = new CreateSellOrder(_location, _currency, getMinAmount(), getMaxAmount(), priceFormulaId,
                  getSelectedPremium(), getDescription());
            title = getResources().getString(R.string.lt_create_sell_order_title);

         }
         SendRequestActivity.callMe(CreateOrEditSellOrderActivity.this, request, title);
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
         ((TextView) findViewById(R.id.tvLocation)).setText(_location.name);
         ((Button) findViewById(R.id.btCurrency)).setText(getCurrency());

      }
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

   OnItemSelectedListener spinnerItemSelected = new OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         enableUi();
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
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
                  return newText.length() < LtApi.MAXIMUM_SELL_ORDER_DESCRIPTION_LENGTH;
               }

               @Override
               public boolean validateTextOnOk(String newText, String oldText) {
                  return newText.length() < LtApi.MAXIMUM_SELL_ORDER_DESCRIPTION_LENGTH;
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
      } else if (requestCode == GET_CURRENCY_RESULT_CODE && resultCode == RESULT_OK) {
         _currency = Preconditions.checkNotNull(intent.getStringExtra(SetLocalCurrencyActivity.CURRENCY_RESULT_NAME));
      } else {
         // We didn't like what we got, bail
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         Toast.makeText(CreateOrEditSellOrderActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         finish();
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(CreateOrEditSellOrderActivity.this);
         finish();
         return true;
      };

      @Override
      public void onLtPriceFormulasFetched(java.util.List<PriceFormula> priceFormulas, GetPriceFormulas request) {
         _priceFormulas = new ArrayList<PriceFormula>(priceFormulas);
         LtAndroidUtils.populatePriceFormulaSpinner(CreateOrEditSellOrderActivity.this, _spPriceFormula, priceFormulas,
               isEdit() ? _sellOrder.priceFormula : null);
         enableUi();
         updateUi();
      };

   };

}
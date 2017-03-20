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

package com.mycelium.wallet.lt.activity.buy;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mycelium.lt.api.model.AdSearchItem;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.activity.SendRequestActivity;
import com.mycelium.wallet.lt.api.CreateTrade;

public class CreateTradeActivity extends Activity implements NumberEntryListener {

   public static void callMe(Activity currentActivity, AdSearchItem adSearchItem) {
      Intent intent = new Intent(currentActivity, CreateTradeActivity.class);
      intent.putExtra("adSearchItem", adSearchItem);
      currentActivity.startActivity(intent);
   }

   private AdSearchItem _adSearchItem;

   private NumberEntry _numberEntry;
   protected MbwManager _mbwManager;
   private TextView _tvAmount;
   private Button _btStartTrading;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_trade_1_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _adSearchItem = (AdSearchItem) getIntent().getSerializableExtra("adSearchItem");

      // Get intent parameters
      Integer amount = null;

      // Load saved state
      if (savedInstanceState != null) {
         amount = (Integer) savedInstanceState.getSerializable("amount");
      }

      String numberString;
      if (amount != null) {
         numberString = amount.toString();
         ((TextView) findViewById(R.id.tvAmount)).setText(numberString);
      } else {
         numberString = "";
      }

      _numberEntry = new NumberEntry(0, this, this, numberString);

      _tvAmount = (TextView) findViewById(R.id.tvAmount);

      String hint = String.format(new Locale(_mbwManager.getLanguage()), "%d-%d", _adSearchItem.minimumFiat,
            _adSearchItem.maximumFiat);
      _tvAmount.setHint(hint);

      _btStartTrading = (Button) findViewById(R.id.btStartTrading);
      _btStartTrading.setOnClickListener(startTradingClickListener);
      ((TextView) findViewById(R.id.tvCurrency)).setText(_adSearchItem.currency);
   }

   OnClickListener startTradingClickListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         CreateTrade request = new CreateTrade(_adSearchItem.id, getNumber());
         SendRequestActivity.callMe(CreateTradeActivity.this, request,
               getString(R.string.lt_place_instant_buy_order_title));
         finish();
      }
   };

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("amount", getNumber());
   }

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   @Override
   public void onEntryChanged(String entry, boolean wasSet) {
      updateUi();
   }

   private void updateUi() {
      Integer number = getNumber();
      if (number == null) {
         // Nothing entered
         _tvAmount.setText("");
         _btStartTrading.setEnabled(false);
      } else if (number < _adSearchItem.minimumFiat || number > _adSearchItem.maximumFiat) {
         // Number too small or too large
         _tvAmount.setText(number.toString());
         _tvAmount.setTextColor(getResources().getColor(R.color.red));
         _btStartTrading.setEnabled(false);
      } else {
         // Everything ok
         _tvAmount.setText(number.toString());
         _tvAmount.setTextColor(getResources().getColor(R.color.white));
         _btStartTrading.setEnabled(true);
      }
   }

   private Integer getNumber() {
      try {
         return Integer.parseInt(_numberEntry.getEntry());
      } catch (NumberFormatException e) {
         return null;
      }
   }

}

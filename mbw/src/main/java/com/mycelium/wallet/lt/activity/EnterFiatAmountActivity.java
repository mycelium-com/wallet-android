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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;

public class EnterFiatAmountActivity extends Activity implements NumberEntryListener {

   public static void callMe(Activity currentActivity, String currency, Integer amount, int requestCode) {
      Intent intent = new Intent(currentActivity, EnterFiatAmountActivity.class);
      intent.putExtra("currency", currency);
      intent.putExtra("amount", amount);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private NumberEntry _numberEntry;
   protected MbwManager _mbwManager;
   private TextView _tvAmount;
   private Button _btUse;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_enter_fiat_amount_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      Integer amount = (Integer) getIntent().getSerializableExtra("amount");
      String currency = getIntent().getStringExtra("currency");

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
      _tvAmount.setHint(Integer.toString(0));

      _btUse = (Button) findViewById(R.id.btUse);
      _btUse.setOnClickListener(useClickListener);
      ((TextView) findViewById(R.id.tvCurrency)).setText(currency);
   }

   OnClickListener useClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Intent result = new Intent();
         result.putExtra("amount", getNumber());
         setResult(RESULT_OK, result);
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
         _btUse.setEnabled(false);
      } else {
         // Everything ok
         _tvAmount.setText(number.toString());
         _tvAmount.setTextColor(getResources().getColor(R.color.white));
         _btUse.setEnabled(true);
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

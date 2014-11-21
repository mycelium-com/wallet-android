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

package com.mycelium.wallet.activity.settings;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.mycelium.wallet.R;
import com.mycelium.wapi.api.lib.CurrencyCode;

public class SetLocalCurrencyActivity extends Activity {

   public static void callMeForResult(Activity currentActivity, String currency, int requestCode) {
      Intent intent = new Intent(currentActivity, SetLocalCurrencyActivity.class);
      intent.putExtra("currency", currency);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static final String CURRENCY_RESULT_NAME = "currency";
   private Map<String, String> _currencySelectionToCurrencyMap;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.settings_local_currency_activity);

      ((AutoCompleteTextView) findViewById(R.id.tvCurrency)).setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
      String enteredString = getIntent().getStringExtra("currency");
      // Load saved state
      if (savedInstanceState != null) {
         enteredString = savedInstanceState.getString("entered");
      }
      enteredString = enteredString == null ? "" : enteredString;
      
      // Build data structures for holding currencies
      _currencySelectionToCurrencyMap = new HashMap<String, String>();
      CurrencyCode[] codes = CurrencyCode.sortedArray();
      String[] strings = new String[codes.length];
      for (int i = 0; i < codes.length; i++) {
         strings[i] = codes[i].getShortString() + " - " + codes[i].getName();
         _currencySelectionToCurrencyMap.put(strings[i], codes[i].getShortString());
      }

      // Populate adapter
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line,
            strings);

      // Configure text view in an arcane manner.
      AutoCompleteTextView acTextView = (AutoCompleteTextView) findViewById(R.id.tvCurrency);
      acTextView.setHint(enteredString);
      acTextView.setThreshold(0);
      acTextView.setAdapter(adapter);
      acTextView.setOnItemClickListener(itemClicked);
   }

   OnItemClickListener itemClicked = new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
         String selection = (String) parent.getItemAtPosition(position);
         String currency = _currencySelectionToCurrencyMap.get(selection);
         if (currency == null) {
            return;
         }
         Intent result = new Intent();
         result.putExtra(CURRENCY_RESULT_NAME, currency);
         setResult(RESULT_OK, result);
         finish();

      }
   };

   @Override
   protected void onResume() {

      // This is pretty crude but does the trick
      // We simulate a click motion event at the end of the text view to make
      // the keyboard appear and the cursor appear at the end.
      // In turn this makes the drop down appear as we have a touch listener
      // opening it up
      Handler handle = new Handler();
      handle.postDelayed(new Runnable() {

         @SuppressLint("Recycle")
         @Override
         public void run() {
            AutoCompleteTextView acTextView = (AutoCompleteTextView) findViewById(R.id.tvCurrency);
            acTextView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                  MotionEvent.ACTION_DOWN, acTextView.getWidth(), 0, 0));
            acTextView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                  MotionEvent.ACTION_UP, acTextView.getWidth(), 0, 0));
         }
      }, 100);
      super.onResume();
   }

   @Override
   protected void onPause() {
      AutoCompleteTextView acTextView = (AutoCompleteTextView) findViewById(R.id.tvCurrency);
      if (acTextView.isPopupShowing()) {
         acTextView.dismissDropDown();
      }
      super.onPause();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putString("entered", ((AutoCompleteTextView) findViewById(R.id.tvCurrency)).getText()
            .toString());
   }

}

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

import java.util.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.common.collect.Sets;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.api.lib.CurrencyCode;

public class SetLocalCurrencyActivity extends Activity {

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, SetLocalCurrencyActivity.class);
      currentActivity.startActivity(intent);
   }

   private Map<String, String> _currencySelectionToCurrencyMap;
   private Set<String> _currencies;
   private ArrayAdapter<String> _adapter;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.settings_local_currency_activity);

      
      // Build data structures for holding currencies
      _currencySelectionToCurrencyMap = new HashMap<String, String>();
      CurrencyCode[] codes = CurrencyCode.sortedArray();
      String[] strings = new String[codes.length];
      for (int i = 0; i < codes.length; i++) {
         strings[i] = codes[i].getShortString() + " - " + codes[i].getName();
         _currencySelectionToCurrencyMap.put(strings[i], codes[i].getShortString());
      }

      // Populate adapter - and overwrite getView to correctly set checkbox status
      _adapter = new ArrayAdapter<String>(this, R.layout.listview_item_with_checkbox, R.id.tv_currency_name, strings){
         @Override
         public View getView(int pos, View convertView, ViewGroup parent){
            if(convertView == null) {
               LayoutInflater inflater = (LayoutInflater)getSystemService(SetLocalCurrencyActivity.LAYOUT_INFLATER_SERVICE);
               convertView = inflater.inflate(R.layout.listview_item_with_checkbox, null);
            }

            String fullname = getItem(pos);
            String currency = _currencySelectionToCurrencyMap.get(fullname);

            TextView tv = (TextView) convertView.findViewById(R.id.tv_currency_name);
            tv.setText(fullname);
            CheckBox box = (CheckBox) convertView.findViewById(R.id.checkbox_currency);
            box.setChecked(_currencies.contains(currency));

            convertView.setOnClickListener(itemClicked);

            return convertView;
         }
      };

      //configure edittext for filtering
      EditText search = (EditText) findViewById(R.id.etFilterCurrency);
      search.addTextChangedListener(filterWatcher);

      // Configure list view
      ListView listview = (ListView) findViewById(R.id.lvCurrencies);
      listview.setAdapter(_adapter);

      _currencies = Sets.newHashSet(MbwManager.getInstance(this).getCurrencyList());
   }

   View.OnClickListener itemClicked = new View.OnClickListener() {

      @Override
      public void onClick(View v) {
         String selection = ((TextView) v.findViewById(R.id.tv_currency_name)).getText().toString();
         String currency = _currencySelectionToCurrencyMap.get(selection);
         if (currency == null) {
            return;
         }
         CheckBox box = (CheckBox) v.findViewById(R.id.checkbox_currency);
         box.setChecked(!box.isChecked());
         setCurrency(currency, box.isChecked());
      }
   };

   TextWatcher filterWatcher = new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
         //empty
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
         SetLocalCurrencyActivity.this._adapter.getFilter().filter(s);
      }

      @Override
      public void afterTextChanged(Editable s) {
         //empty
      }
   };

   private void setCurrency(String currency, boolean isSelected) {
      if (isSelected) {
         _currencies.add(currency);
      } else {
         _currencies.remove(currency);
      }
      MbwManager.getInstance(this).setCurrencyList(_currencies);
   }
}

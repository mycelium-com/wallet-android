/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mycelium.wallet.activity;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;

/***
 * PreferenceActivity is a built-in Activity for preferences management
 * 
 * To retrieve the values stored by this activity in other activities use the
 * following snippet:
 * 
 * SharedPreferences sharedPreferences =
 * PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 * <Preference Type> preferenceValue = sharedPreferences.get<Preference
 * Type>("<Preference Key>",<default value>);
 */
public class SettingsActivity extends PreferenceActivity {

   private Preference _clearPin;
   private Preference _setPin;
   private ListPreference _bitcoinDenomination;
   private ListPreference _localCurrency;
   private CheckBoxPreference _showHints;
   private MbwManager _mbwManager;
   private Dialog _dialog;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
      _mbwManager = MbwManager.getInstance(SettingsActivity.this.getApplication());
      // Bitcoin Denomination
      _bitcoinDenomination = (ListPreference) findPreference("bitcoin_denomination");
      _bitcoinDenomination.setDefaultValue(_mbwManager.getBitcoinDenomination().toString());
      _bitcoinDenomination.setValue(_mbwManager.getBitcoinDenomination().toString());
      CharSequence[] denominations = new CharSequence[] { Denomination.BTC.toString(), Denomination.mBTC.toString(),
            Denomination.uBTC.toString() };
      _bitcoinDenomination.setEntries(denominations);
      _bitcoinDenomination.setEntryValues(denominations);
      _bitcoinDenomination.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setBitcoinDenomination(Denomination.fromString(newValue.toString()));
            return true;
         }
      });

      // Local Currency
      _localCurrency = (ListPreference) findPreference("local_currency");
      _localCurrency.setDefaultValue(_mbwManager.getFiatCurrency());
      _localCurrency.setValue(_mbwManager.getFiatCurrency());
      _localCurrency.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setFiatCurrency(newValue.toString());
            return true;
         }
      });

      // Set PIN
      _setPin = (Preference) findPreference("setPin");
      _setPin.setOnPreferenceClickListener(setPinClickListener);

      // Clear PIN
      updateClearPin();

      // Show Hints
      _showHints = (CheckBoxPreference) findPreference("showHints");
      _showHints.setChecked(_mbwManager.getShowHints());
      _showHints.setOnPreferenceClickListener(showHintsClickListener);

   }

   private void updateClearPin() {
      _clearPin = (Preference) findPreference("clearPin");
      _clearPin.setEnabled(_mbwManager.isPinProtected());
      _clearPin.setOnPreferenceClickListener(clearPinClickListener);
   }

   @Override
   protected void onDestroy() {
      if (_dialog != null && _dialog.isShowing()) {
         _dialog.dismiss();
      }
      super.onDestroy();
   }

   private final OnPreferenceClickListener setPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.runPinProtectedFunction(SettingsActivity.this, new Runnable() {
            @Override
            public void run() {
               setPin();
            }
         });
         return true;
      }
   };

   private void setPin() {
      final Context context = SettingsActivity.this;
      _dialog = new PinDialog(context, false, new PinDialog.OnPinEntered() {
         private String newPin = null;

         @Override
         public void pinEntered(PinDialog dialog, String pin) {
            if (newPin == null) {
               newPin = pin;
               dialog.setTitle(R.string.pin_confirm_pin);
            } else if (newPin.equals(pin)) {
               _mbwManager.setPin(pin);
               Toast.makeText(context, R.string.pin_set, Toast.LENGTH_LONG).show();
               updateClearPin();
               dialog.dismiss();
            } else {
               Toast.makeText(context, R.string.pin_codes_dont_match, Toast.LENGTH_LONG).show();
               _mbwManager.vibrate(500);
               dialog.dismiss();
            }
         }
      });
      _dialog.setTitle(R.string.pin_enter_new_pin);
      _dialog.show();
   }

   private final OnPreferenceClickListener clearPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.runPinProtectedFunction(SettingsActivity.this, new Runnable() {
            @Override
            public void run() {
               _mbwManager.setPin("");
               updateClearPin();
               Toast.makeText(SettingsActivity.this, R.string.pin_cleared, Toast.LENGTH_LONG).show();
            }
         });
         return true;
      }
   };

   private final OnPreferenceClickListener showHintsClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.setShowHints(p.isChecked());
         return true;
      }
   };

}
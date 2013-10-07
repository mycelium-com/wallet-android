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

package com.mycelium.wallet.activity.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mycelium.wallet.CurrencyCode;
import com.mycelium.wallet.ExchangeRateCalculationMode;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.activity.TextNormalizer;

/**
 * PreferenceActivity is a built-in Activity for preferences management
 * <p/>
 * To retrieve the values stored by this activity in other activities use the
 * following snippet:
 * <p/>
 * SharedPreferences sharedPreferences =
 * PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 * <Preference Type> preferenceValue = sharedPreferences.get<Preference
 * Type>("<Preference Key>",<default value>);
 */
public class SettingsActivity extends PreferenceActivity {

   private static final int GET_CURRENCY_RESULT_CODE = 0;

   public static final CharMatcher AMOUNT = CharMatcher.JAVA_DIGIT.or(CharMatcher.anyOf(".,"));
   private Preference _clearPin;
   private Preference _setPin;
   private ListPreference _bitcoinDenomination;
   private Preference _localCurrency;
   private ListPreference _exchangeSource;
   private CheckBoxPreference _showHints;
   private CheckBoxPreference _expertMode;
   private CheckBoxPreference _continuousAutoFocus;
   private CheckBoxPreference _aggregatedView;
   private CheckBoxPreference _pulsingQrCodes;
   private MbwManager _mbwManager;
   private Dialog _dialog;
   private EditTextPreference _proxy;
   private EditTextPreference _autoPay;
   public static final Function<String, String> AUTOPAY_EXTRACT = new Function<String, String>() {
      @Override
      public String apply(String input) {
         return extractAmount(input);
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
      _mbwManager = MbwManager.getInstance(SettingsActivity.this.getApplication());
      // Bitcoin Denomination
      _bitcoinDenomination = (ListPreference) findPreference("bitcoin_denomination");
      _bitcoinDenomination.setTitle(bitcoinDenominationTitle());
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
            _bitcoinDenomination.setTitle(bitcoinDenominationTitle());
            return true;
         }
      });

      _localCurrency = findPreference("local_currency");
      _localCurrency.setOnPreferenceClickListener(localCurrencyClickListener);
      _localCurrency.setTitle(localCurrencyTitle());

      // Exchange Source
      _exchangeSource = (ListPreference) findPreference("exchange_source");
      _exchangeSource.setTitle(exchangeSourceTitle());
      _exchangeSource.setDefaultValue(_mbwManager.getExchangeRateCalculationMode().toString());
      _exchangeSource.setValue(_mbwManager.getExchangeRateCalculationMode().toString());
      _exchangeSource.setEntries(ExchangeRateCalculationMode.orderedNames());
      _exchangeSource.setEntryValues(ExchangeRateCalculationMode.orderedNames());
      _exchangeSource.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setExchangeRateCalculationMode(ExchangeRateCalculationMode.fromName(newValue.toString()));
            _exchangeSource.setTitle(exchangeSourceTitle());
            return true;
         }
      });

      // Set PIN
      _setPin = Preconditions.checkNotNull(findPreference("setPin"));
      _setPin.setOnPreferenceClickListener(setPinClickListener);

      // Clear PIN
      updateClearPin();

      // Show Hints
      _showHints = (CheckBoxPreference) findPreference("showHints");
      _showHints.setChecked(_mbwManager.getShowHints());
      _showHints.setOnPreferenceClickListener(showHintsClickListener);

      // Expert Mode
      _expertMode = (CheckBoxPreference) findPreference("expertMode");
      _expertMode.setChecked(_mbwManager.getExpertMode());
      _expertMode.setOnPreferenceClickListener(expertModeClickListener);

      // Show Swipe Animation
      _continuousAutoFocus = (CheckBoxPreference) findPreference("continuousFocus");
      _continuousAutoFocus.setChecked(_mbwManager.getContinuousFocus());
      _continuousAutoFocus.setOnPreferenceClickListener(continuousAutoFocusClickListener);

      // Pulsing QR codes
      _pulsingQrCodes = (CheckBoxPreference) findPreference("pulsingQrCodes");
      _pulsingQrCodes.setChecked(_mbwManager.getPulsingQrCodes());
      _pulsingQrCodes.setOnPreferenceClickListener(pulsingQrCodeClickListener);

      // Aggregated View
      _aggregatedView = (CheckBoxPreference) findPreference("aggregatedView");
      _aggregatedView.setChecked(_mbwManager.getWalletMode() == WalletMode.Aggregated);
      _aggregatedView.setOnPreferenceClickListener(aggregatedViewClickListener);

      // Socks Proxy
      _proxy = Preconditions.checkNotNull((EditTextPreference) findPreference("proxy"));
      _proxy.setTitle(getSocksProxyTitle());

      _proxy.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            _mbwManager.setProxy(value);
            getSocksProxyTitle();
            return true;
         }
      });

      _autoPay = Preconditions.checkNotNull((EditTextPreference) findPreference("instantPayAmount"));
      _autoPay.setDefaultValue("0.00");
      _autoPay.setTitle(autoPayTitle());
      _autoPay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object o) {
            final String text = (String) o;
            long amount = isNumber(text) ? (long) (Double.parseDouble(text) * 100) : 0;
            _mbwManager.setAutoPay(amount);
            _autoPay.setTitle(autoPayTitle());
            return true;
         }
      });

      final EditText autpayEdit = _autoPay.getEditText();
      autpayEdit.addTextChangedListener(new TextNormalizer(AUTOPAY_EXTRACT, autpayEdit));

      applyExpertMode();
   }

   private void applyExpertMode() {
      boolean expert = _mbwManager.getExpertMode();
      _aggregatedView.setEnabled(expert);
      _autoPay.setEnabled(expert);
      _proxy.setEnabled(expert);
   }

   private String getSocksProxyTitle() {
      String host = System.getProperty(MbwManager.PROXY_HOST);
      String port = System.getProperty(MbwManager.PROXY_PORT);
      if (Strings.isNullOrEmpty(host) || Strings.isNullOrEmpty(port)) {
         return getResources().getString(R.string.pref_socks_proxy_not_enabled);
      } else {
         return getResources().getString(R.string.pref_socks_proxy_enabled);
      }
   }

   @VisibleForTesting
   static boolean isNumber(String text) {
      try {
         Double.parseDouble(text);
      } catch (NumberFormatException ignore) {
         return false;
      }
      return true;
   }

   @VisibleForTesting
   static String extractAmount(CharSequence s) {
      String amt = AMOUNT.retainFrom(s).replace(",", ".");
      int commaIdx = amt.indexOf(".");
      if (commaIdx > -1) {
         String cents = amt.substring(commaIdx + 1, Math.min(amt.length(), commaIdx + 3));
         String euros = amt.substring(0, commaIdx);
         return euros + "." + cents;
      }
      return amt;
   }

   private String autoPayTitle() {
      return getResources().getString(R.string.pref_autopay_with_value,
            CurrencyCode.fromShortString(_mbwManager.getFiatCurrency()).getShortString(),
            (double) _mbwManager.getAutoPay() / 100);
   }

   private String localCurrencyTitle() {
      return getResources().getString(R.string.pref_local_currency_with_currency,
            CurrencyCode.fromShortString(_mbwManager.getFiatCurrency()).getShortString());
   }

   private String exchangeSourceTitle() {
      return getResources().getString(R.string.pref_exchange_source_with_value,
            _mbwManager.getExchangeRateCalculationMode().getShortName());
   }

   private String bitcoinDenominationTitle() {
      return getResources().getString(R.string.pref_bitcoin_denomination_with_denomination,
            _mbwManager.getBitcoinDenomination().getAsciiName());
   }

   private void updateClearPin() {
      _clearPin = findPreference("clearPin");
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

   private final OnPreferenceClickListener localCurrencyClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         Intent intent = new Intent(SettingsActivity.this, SetLocalCurrencyActivity.class);
         SettingsActivity.this.startActivityForResult(intent, GET_CURRENCY_RESULT_CODE);
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

   private final OnPreferenceClickListener expertModeClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.setExpertMode(p.isChecked());
         applyExpertMode();
         return true;
      }
   };

   private final OnPreferenceClickListener continuousAutoFocusClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.setContinousFocus(p.isChecked());
         return true;
      }
   };

   private final OnPreferenceClickListener pulsingQrCodeClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.setPulsingQrCodes(p.isChecked());
         return true;
      }
   };

   private final OnPreferenceClickListener aggregatedViewClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         WalletMode mode = p.isChecked() ? WalletMode.Aggregated : WalletMode.Segregated;
         _mbwManager.setWalletMode(mode);
         return true;
      }
   };

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == GET_CURRENCY_RESULT_CODE && resultCode == RESULT_OK) {
         String currency = Preconditions.checkNotNull(intent
               .getStringExtra(SetLocalCurrencyActivity.CURRENCY_RESULT_NAME));
         _mbwManager.setFiatCurrency(currency);
         _localCurrency.setTitle(localCurrencyTitle());
      }
   }

}
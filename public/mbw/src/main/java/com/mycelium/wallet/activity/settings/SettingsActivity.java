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

import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.widget.Toast;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mrd.mbwapi.api.CurrencyCode;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.PinDialog;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;

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
   private static final boolean EMAIL_ENABLED = false;
   private ListPreference _bitcoinDenomination;
   private Preference _localCurrency;
   private ListPreference _exchangeSource;
   private ListPreference _language;
   private CheckBoxPreference _aggregatedView;
   private CheckBoxPreference _ltDisable;
   private CheckBoxPreference _ltNotificationSound;
   private CheckBoxPreference _ltMilesKilometers;
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Dialog _dialog;
   private EditTextPreference _proxy;
   public static final Function<String, String> AUTOPAY_EXTRACT = new Function<String, String>() {
      @Override
      public String apply(String input) {
         return extractAmount(input);
      }
   };
   private ImmutableMap<String, String> _languageLookup;
   private LocalTraderEventSubscriber ltListener;
   private EditTextPreference ltNotificationEmail;

   @SuppressWarnings("deprecation")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.preferences);
      _mbwManager = MbwManager.getInstance(SettingsActivity.this.getApplication());
      _ltManager = _mbwManager.getLocalTraderManager();
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
      ExchangeRateManager exchangeManager = _mbwManager.getExchamgeRateManager();
      CharSequence[] exchangeNames = exchangeManager.getExchangeRateNames().toArray(new String[] {});
      _exchangeSource.setEntries(exchangeNames);
      if (exchangeNames.length == 0) {
         _exchangeSource.setEnabled(false);
      } else {
         String currentName = exchangeManager.getCurrentRateName();
         if (currentName == null) {
            currentName = "";
         }
         _exchangeSource.setEntries(exchangeNames);
         _exchangeSource.setEntryValues(exchangeNames);
         _exchangeSource.setDefaultValue(currentName);
         _exchangeSource.setValue(currentName);
      }
      _exchangeSource.setTitle(exchangeSourceTitle());
      _exchangeSource.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.getExchamgeRateManager().setCurrentRateName(newValue.toString());
            _exchangeSource.setTitle(exchangeSourceTitle());
            return true;
         }
      });

      _language = (ListPreference) findPreference("user_language");
      _language.setTitle(getLanguageSettingTitle());
      _language.setDefaultValue(Locale.getDefault().getLanguage());
      _language.setSummary(_mbwManager.getLanguage());
      _language.setValue(_mbwManager.getLanguage());

      _languageLookup = loadLanguageLookups();
      _language.setSummary(_languageLookup.get(_mbwManager.getLanguage()));

      _language.setEntries(R.array.languages_desc);
      _language.setEntryValues(R.array.languages);
      _language.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            String lang = newValue.toString();
            _mbwManager.setLanguage(lang);
            WalletApplication app = (WalletApplication) getApplication();
            app.applyLanguageChange(lang);

            restart();

            return true;
         }
      });

      // Set PIN
      Preference setPin = Preconditions.checkNotNull(findPreference("setPin"));
      setPin.setOnPreferenceClickListener(setPinClickListener);

      // Clear PIN
      updateClearPin();

      // Local Trader
      _ltDisable = (CheckBoxPreference) findPreference("ltDisable");
      _ltDisable.setChecked(_ltManager.isLocalTraderDisabled());
      _ltDisable.setOnPreferenceClickListener(ltDisableLocalTraderClickListener);


      _ltNotificationSound = (CheckBoxPreference) findPreference("ltNotificationSound");
      _ltNotificationSound.setChecked(_ltManager.getPlaySoundOnTradeNotification());
      _ltNotificationSound.setOnPreferenceClickListener(ltNotificationSoundClickListener);

      _ltMilesKilometers = (CheckBoxPreference) findPreference("ltMilesKilometers");
      _ltMilesKilometers.setChecked(_ltManager.useMiles());
      _ltMilesKilometers.setOnPreferenceClickListener(ltMilesKilometersClickListener);

      // Expert Mode
      CheckBoxPreference expertMode = (CheckBoxPreference) findPreference("expertMode");
      expertMode.setChecked(_mbwManager.getExpertMode());
      expertMode.setOnPreferenceClickListener(expertModeClickListener);

      // Show Swipe Animation
      CheckBoxPreference continuousFocus = (CheckBoxPreference) findPreference("continuousFocus");
      continuousFocus.setChecked(_mbwManager.getContinuousFocus());
      continuousFocus.setOnPreferenceClickListener(continuousAutoFocusClickListener);

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

      applyExpertMode();
      applyLocalTraderEnablement();
   }

   @Override
   protected void onResume() {
      ltListener = new LocalTraderEventSubscriber(new Handler()) {
         @Override
         public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
            ltNotificationEmail.setSummary(info.notificationEmail);
         }

         @Override
         public void onLtError(int errorCode) {
            //
         }
      };
      _ltManager.subscribe(ltListener);

      if (EMAIL_ENABLED) {
         setupEmailNotificationSetting();
      } else {
         PreferenceCategory ltprefs = (PreferenceCategory) findPreference("localtraderPrefs");
         ltprefs.removePreference(findPreference("ltNotificationEmail"));
      }

      super.onResume();
   }

   private void setupEmailNotificationSetting() {
      ltNotificationEmail = (EditTextPreference) findPreference("ltNotificationEmail");

      if (!_ltManager.hasLocalTraderAccount()) {
         ltNotificationEmail.setEnabled(false);
         return;
      }


      ltNotificationEmail.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _ltManager.makeRequest(new SetNotificationMail(newValue.toString()));
            ltNotificationEmail.setSummary(newValue.toString());
            return true;
         }
      });

      final TraderInfo cachedTraderInfo = _ltManager.getCachedTraderInfo();
      if (cachedTraderInfo == null) {
         new Thread() {
            @Override
            public void run() {
               _ltManager.makeRequest(new GetTraderInfo());
            }
         }.start();
      } else {
         ltNotificationEmail.setSummary(cachedTraderInfo.notificationEmail);
      }

   }
   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltListener);
      super.onPause();
   }

   private String getLanguageSettingTitle() {
      String displayed = getResources().getString(R.string.pref_change_language);
      String english = Utils.loadEnglish(R.string.pref_change_language);
      return english.equals(displayed) ? displayed : displayed + " / " + english;
   }

   private ImmutableMap<String, String> loadLanguageLookups() {
      String[] langDesc = getResources().getStringArray(R.array.languages_desc);
      String[] langs = getResources().getStringArray(R.array.languages);

      ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
      for (int i = 0; i < langs.length; i++) {
         String lang = langs[i];
         String desc = langDesc[i];
         b.put(lang, desc);
      }
      return b.build();
   }

   private void restart() {
      Intent running = getIntent();
      finish();
      startActivity(running);
   }

   private void applyExpertMode() {
      boolean expert = _mbwManager.getExpertMode();
      _aggregatedView.setEnabled(expert);
      _proxy.setEnabled(expert);
   }

   private void applyLocalTraderEnablement() {
      boolean ltDisabled = _ltManager.isLocalTraderDisabled();
      _ltNotificationSound.setEnabled(!ltDisabled);
      _ltMilesKilometers.setEnabled(!ltDisabled);
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

   private String localCurrencyTitle() {
      return getResources().getString(R.string.pref_local_currency_with_currency,
            CurrencyCode.fromShortString(_mbwManager.getFiatCurrency()).getShortString());
   }

   private String exchangeSourceTitle() {
      String name = _mbwManager.getExchamgeRateManager().getCurrentRateName();
      if (name == null) {
         name = "";
      }
      return getResources().getString(R.string.pref_exchange_source_with_value, name);
   }

   private String bitcoinDenominationTitle() {
      return getResources().getString(R.string.pref_bitcoin_denomination_with_denomination,
            _mbwManager.getBitcoinDenomination().getAsciiName());
   }

   @SuppressWarnings("deprecation")
   private void updateClearPin() {
      Preference clearPin = findPreference("clearPin");
      clearPin.setEnabled(_mbwManager.isPinProtected());
      clearPin.setOnPreferenceClickListener(clearPinClickListener);
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
         String currency = _mbwManager.getFiatCurrency();
         SetLocalCurrencyActivity.callMeForResult(SettingsActivity.this, currency, GET_CURRENCY_RESULT_CODE);
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

   private final OnPreferenceClickListener ltDisableLocalTraderClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _ltManager.setLocalTraderDisabled(p.isChecked());
         applyLocalTraderEnablement();
         return true;
      }
   };

   private final OnPreferenceClickListener ltNotificationSoundClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _ltManager.setPlaySoundOnTradeNotification(p.isChecked());
         return true;
      }
   };
   
   private final OnPreferenceClickListener ltMilesKilometersClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _ltManager.setUseMiles(p.isChecked());
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
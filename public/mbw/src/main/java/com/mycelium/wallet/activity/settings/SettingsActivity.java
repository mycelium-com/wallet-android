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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;
import com.mycelium.wapi.api.lib.CurrencyCode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.List;
import java.util.Locale;

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

   public static final CharMatcher AMOUNT = CharMatcher.JAVA_DIGIT.or(CharMatcher.anyOf(".,"));
   public static final Function<String, String> AUTOPAY_EXTRACT = new Function<String, String>() {
      @Override
      public String apply(String input) {
         return extractAmount(input);
      }
   };
   private static final int GET_CURRENCY_RESULT_CODE = 0;
   private final OnPreferenceClickListener localCurrencyClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         String currency = _mbwManager.getFiatCurrency();
         SetLocalCurrencyActivity.callMeForResult(SettingsActivity.this, currency, GET_CURRENCY_RESULT_CODE);
         return true;
      }
   };
   private final OnPreferenceClickListener setPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.showSetPinDialog(SettingsActivity.this, Optional.<Runnable>of(new Runnable() {
                  @Override
                  public void run() {
                     updateClearPin();
                  }
               })
         );
         return true;
      }
   };
   private final OnPreferenceClickListener clearPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.showClearPinDialog(SettingsActivity.this, Optional.<Runnable>of(new Runnable() {
            @Override
            public void run() {
               updateClearPin();
            }
         }));
         return true;
      }
   };
   private final OnPreferenceClickListener legacyBackupClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         Utils.pinProtectedBackup(SettingsActivity.this);
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

   private ListPreference _bitcoinDenomination;
   private Preference _localCurrency;
   private ListPreference _exchangeSource;
   private CheckBoxPreference _ltNotificationSound;
   private CheckBoxPreference _ltMilesKilometers;
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Dialog _dialog;
   private ListPreference _minerFee;

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
      CharSequence[] denominations = new CharSequence[]{Denomination.BTC.toString(), Denomination.mBTC.toString(),
            Denomination.uBTC.toString(), Denomination.BITS.toString()};
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

      // Miner Fee
      _minerFee = (ListPreference) findPreference("miner_fee");
      _minerFee.setTitle(getMinerFeeTitle());
      _minerFee.setSummary(getMinerFeeSummary());
      _minerFee.setDefaultValue(_mbwManager.getMinerFee().toString());
      _minerFee.setValue(_mbwManager.getMinerFee().toString());
      CharSequence[] minerFees = new CharSequence[]{MinerFee.ECONOMIC.toString(), MinerFee.NORMAL.toString(), MinerFee.PRIORITY.toString()};
      CharSequence[] minerFeeNames = new CharSequence[]{getString(R.string.miner_fee_economic_name),
            getString(R.string.miner_fee_normal_name), getString(R.string.miner_fee_priority_name)};
      _minerFee.setEntries(minerFeeNames);
      _minerFee.setEntryValues(minerFees);
      _minerFee.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setMinerFee(MinerFee.fromString(newValue.toString()));
            _minerFee.setTitle(getMinerFeeTitle());
            _minerFee.setSummary(getMinerFeeSummary());
            String description = MinerFee.getMinerFeeDescription(_mbwManager.getMinerFee(), SettingsActivity.this);
            Utils.showSimpleMessageDialog(SettingsActivity.this, description);
            return true;
         }
      });

      _localCurrency = findPreference("local_currency");
      _localCurrency.setOnPreferenceClickListener(localCurrencyClickListener);
      _localCurrency.setTitle(localCurrencyTitle());

      // Exchange Source
      _exchangeSource = (ListPreference) findPreference("exchange_source");
      ExchangeRateManager exchangeManager = _mbwManager.getExchangeRateManager();
      CharSequence[] exchangeNames = exchangeManager.getExchangeRateNames().toArray(new String[]{});
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
            _mbwManager.getExchangeRateManager().setCurrentRateName(newValue.toString());
            _exchangeSource.setTitle(exchangeSourceTitle());
            return true;
         }
      });

      ListPreference language = (ListPreference) findPreference("user_language");
      language.setTitle(getLanguageSettingTitle());
      language.setDefaultValue(Locale.getDefault().getLanguage());
      language.setSummary(_mbwManager.getLanguage());
      language.setValue(_mbwManager.getLanguage());

      ImmutableMap<String, String> languageLookup = loadLanguageLookups();
      language.setSummary(languageLookup.get(_mbwManager.getLanguage()));

      language.setEntries(R.array.languages_desc);
      language.setEntryValues(R.array.languages);
      language.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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

      // Legacy backup function
      Preference legacyBackup = Preconditions.checkNotNull(findPreference("legacyBackup"));
      legacyBackup.setOnPreferenceClickListener(legacyBackupClickListener);

      // Local Trader
      CheckBoxPreference ltDisable = (CheckBoxPreference) findPreference("ltDisable");
      ltDisable.setChecked(_ltManager.isLocalTraderDisabled());
      ltDisable.setOnPreferenceClickListener(ltDisableLocalTraderClickListener);


      _ltNotificationSound = (CheckBoxPreference) findPreference("ltNotificationSound");
      _ltNotificationSound.setChecked(_ltManager.getPlaySoundOnTradeNotification());
      _ltNotificationSound.setOnPreferenceClickListener(ltNotificationSoundClickListener);

      _ltMilesKilometers = (CheckBoxPreference) findPreference("ltMilesKilometers");
      _ltMilesKilometers.setChecked(_ltManager.useMiles());
      _ltMilesKilometers.setOnPreferenceClickListener(ltMilesKilometersClickListener);

      // Socks Proxy
      EditTextPreference proxy = Preconditions.checkNotNull((EditTextPreference) findPreference("proxy"));
      proxy.setTitle(getSocksProxyTitle());

      proxy.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            _mbwManager.setProxy(value);
            getSocksProxyTitle();
            return true;
         }
      });

      applyLocalTraderEnablement();
   }

   @Override
   protected void onResume() {
      setupLocalTraderSettings();
      showOrHideLegacyBackup();
      super.onResume();
   }

   private ProgressDialog pleaseWait;


   @SuppressWarnings("deprecation")
   private void setupLocalTraderSettings() {
      if (!_ltManager.hasLocalTraderAccount()) {
         PreferenceScreen myceliumPreferences = (PreferenceScreen) findPreference("myceliumPreferences");
         PreferenceCategory localTraderPrefs = (PreferenceCategory) findPreference("localtraderPrefs");
         CheckBoxPreference disableLt = (CheckBoxPreference) findPreference("ltDisable");
         if (localTraderPrefs != null) {
            localTraderPrefs.removeAll();
            //its important we keep this prefs, so users can still enable / disable lt without having an account
            localTraderPrefs.addPreference(disableLt);
         }
         return;
      }
      setupEmailNotificationSetting();
   }

   @SuppressWarnings("deprecation")
   private void showOrHideLegacyBackup() {
      List<WalletAccount> accounts = _mbwManager.getWalletManager(false).getSpendingAccounts();
      Preference legacyPref = findPreference("legacyBackup");
      if (legacyPref == null) return; // it was already removed, don't remove it again.

      PreferenceCategory legacyCat = (PreferenceCategory) findPreference("legacy");
      for (WalletAccount account : accounts) {
         if (account instanceof SingleAddressAccount) {
            return; //we have a single address account with priv key, so its fine to show the setting
         }
      }
      //no matching account, hide setting
      legacyCat.removePreference(legacyPref);
   }

   @SuppressWarnings("deprecation")
   private void setupEmailNotificationSetting() {
      Preference ltNotificationEmail = findPreference("ltNotificationEmail2");
      ltNotificationEmail.setOnPreferenceClickListener(new OnPreferenceClickListener() {
         @Override
         public boolean onPreferenceClick(final Preference preference) {
            LocalTraderEventSubscriber listener = new SubscribeToServerResponse();
            _ltManager.subscribe(listener);
            new Thread() {
               @Override
               public void run() {
                  _ltManager.makeRequest(new GetTraderInfo());
               }
            }.start();
            pleaseWait = ProgressDialog.show(SettingsActivity.this, getString(R.string.fetching_info),
                  getString(R.string.please_wait), true);
            return true;
         }
      });
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

   private String localCurrencyTitle() {
      return getResources().getString(R.string.pref_local_currency_with_currency,
            CurrencyCode.fromShortString(_mbwManager.getFiatCurrency()).getShortString());
   }

   private String exchangeSourceTitle() {
      String name = _mbwManager.getExchangeRateManager().getCurrentRateName();
      if (name == null) {
         name = "";
      }
      return getResources().getString(R.string.pref_exchange_source_with_value, name);
   }

   private String bitcoinDenominationTitle() {
      return getResources().getString(R.string.pref_bitcoin_denomination_with_denomination,
            _mbwManager.getBitcoinDenomination().getAsciiName());
   }

   private String getMinerFeeTitle() {
      return getResources().getString(R.string.pref_miner_fee_title,
            MinerFee.getMinerFeeName(_mbwManager.getMinerFee(), this));
   }

   private String getMinerFeeSummary() {
      return getResources().getString(R.string.pref_miner_fee_summary,
            _mbwManager.getBtcValueString(_mbwManager.getMinerFee().kbMinerFee));
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


   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == GET_CURRENCY_RESULT_CODE && resultCode == RESULT_OK) {
         String currency = Preconditions.checkNotNull(intent
               .getStringExtra(SetLocalCurrencyActivity.CURRENCY_RESULT_NAME));
         _mbwManager.setFiatCurrency(currency);
         _localCurrency.setTitle(localCurrencyTitle());
      }
   }

   private class SubscribeToServerResponse extends LocalTraderEventSubscriber {

      private Button okButton;
      private EditText emailEdit;

      public SubscribeToServerResponse() {
         super(new Handler());
      }

      private boolean checkValidMail(CharSequence value) {
         return value.length() == 0 || //allow empty email, this removes email notifications
               android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         pleaseWait.dismiss();
         AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this);
         b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               String email = emailEdit.getText().toString();
               _ltManager.makeRequest(new SetNotificationMail(email));
            }
         });
         b.setNegativeButton(R.string.cancel, null);

         emailEdit = new EditText(SettingsActivity.this) {
            @Override
            protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {

               super.onTextChanged(text, start, lengthBefore, lengthAfter);
               if (okButton != null) { //setText is also set before the alert is finished constructing
                  okButton.setEnabled(checkValidMail(text));
               }
            }
         };
         emailEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

         emailEdit.setText(info.notificationEmail);
         b.setView(emailEdit);
         AlertDialog dialog = b.show();
         okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
         _ltManager.unsubscribe(this);
      }

      @Override
      public void onLtError(int errorCode) {
         pleaseWait.dismiss();
         new Toaster(SettingsActivity.this).toast("Unable to retrieve Trader Info from the server", false);
         _ltManager.unsubscribe(this);

      }
   }
}
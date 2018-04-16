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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.view.ButtonPreference;
import com.mycelium.wallet.event.SpvSyncChanged;
import com.mycelium.wallet.external.BuySellServiceDescriptor;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wallet.modularisation.GooglePlayModuleCollection;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import info.guardianproject.onionkit.ui.OrbotHelper;

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
   private static final int REQUEST_CODE_UNINSTALL = 1;
   private final OnPreferenceClickListener localCurrencyClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         SetLocalCurrencyActivity.callMe(SettingsActivity.this);
         return true;
      }
   };
   private final OnPreferenceClickListener setPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.showSetPinDialog(SettingsActivity.this, Optional.<Runnable>of(new Runnable() {
                    @Override
                    public void run() {
                       updateClearPin();
                       updatePinAtStartup();
                    }
                 })
         );
         return true;
      }
   };

   private final OnPreferenceChangeListener setPinOnStartupClickListener = new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(final Preference preference, Object o) {
         _mbwManager.runPinProtectedFunction(SettingsActivity.this, new Runnable() {
                    @Override
                    public void run() {
                       // toggle it here
                       boolean checked = !((CheckBoxPreference) preference).isChecked();
                       _mbwManager.setPinRequiredOnStartup(checked);
                       ((CheckBoxPreference) preference).setChecked(_mbwManager.getPinRequiredOnStartup());
                    }
                 }
         );

         // dont automatically take the new value, lets to it in our the pin protected runnable
         return false;
      }
   };
   private final OnPreferenceClickListener clearPinClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         _mbwManager.showClearPinDialog(SettingsActivity.this, Optional.<Runnable>of(new Runnable() {
            @Override
            public void run() {
               updateClearPin();
               updatePinAtStartup();
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
   private final OnPreferenceClickListener legacyBackupVerifyClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         VerifyBackupActivity.callMe(SettingsActivity.this);
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

   private final OnPreferenceClickListener showBip44PathClickListener = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.getMetadataStorage().setShowBip44Path(p.isChecked());
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

   private final OnPreferenceClickListener onClickLedgerNotificationDisableTee = new OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference preference) {
         CheckBoxPreference p = (CheckBoxPreference) preference;
         _mbwManager.getLedgerManager().setDisableTEE(p.isChecked());
         return true;
      }
   };

   private final OnPreferenceClickListener onClickLedgerSetUnpluggedAID = new OnPreferenceClickListener() {
      private EditText aidEdit;

      public boolean onPreferenceClick(Preference preference) {
         AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this);
         b.setTitle(getString(R.string.ledger_set_unplugged_aid_title));
         b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               byte[] aidBinary;
               String aid = aidEdit.getText().toString();
               try {
                  aidBinary = HexUtils.toBytes(aid);
               } catch (Exception e) {
                  aidBinary = null;
               }
               if (aidBinary == null) {
                  Utils.showSimpleMessageDialog(SettingsActivity.this, getString(R.string.ledger_check_unplugged_aid));
               } else {
                  _mbwManager.getLedgerManager().setUnpluggedAID(aid);
               }
            }
         });
         b.setNegativeButton(R.string.cancel, null);

         aidEdit = new EditText(SettingsActivity.this);
         aidEdit.setInputType(InputType.TYPE_CLASS_TEXT);
         aidEdit.setText(_mbwManager.getLedgerManager().getUnpluggedAID());
         LinearLayout llDialog = new LinearLayout(SettingsActivity.this);
         llDialog.setOrientation(LinearLayout.VERTICAL);
         llDialog.setPadding(10, 10, 10, 10);
         TextView tvInfo = new TextView(SettingsActivity.this);
         tvInfo.setText(getString(R.string.ledger_unplugged_aid));
         llDialog.addView(tvInfo);
         llDialog.addView(aidEdit);
         b.setView(llDialog);
         b.show();
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
   private ListPreference _minerFee;
   private ListPreference _blockExplorer;

   @SuppressWarnings("ResultOfMethodCallIgnored")
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
      _minerFee.setValue(_mbwManager.getMinerFee().toString());
      CharSequence[] minerFees = new CharSequence[]{
              MinerFee.LOWPRIO.toString(),
              MinerFee.ECONOMIC.toString(),
              MinerFee.NORMAL.toString(),
              MinerFee.PRIORITY.toString()};
      CharSequence[] minerFeeNames = new CharSequence[]{
              getString(R.string.miner_fee_lowprio_name),
              getString(R.string.miner_fee_economic_name),
              getString(R.string.miner_fee_normal_name),
              getString(R.string.miner_fee_priority_name)};
      _minerFee.setEntries(minerFeeNames);
      _minerFee.setEntryValues(minerFees);
      _minerFee.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setMinerFee(MinerFee.fromString(newValue.toString()));
            _minerFee.setTitle(getMinerFeeTitle());
            _minerFee.setSummary(getMinerFeeSummary());
            String description = _mbwManager.getMinerFee().getMinerFeeDescription(SettingsActivity.this);
            Utils.showSimpleMessageDialog(SettingsActivity.this, description);
            return true;
         }
      });


      //Block Explorer
      _blockExplorer = (ListPreference) findPreference("block_explorer");
      _blockExplorer.setTitle(getBlockExplorerTitle());
      _blockExplorer.setSummary(getBlockExplorerSummary());
      _blockExplorer.setValue(_mbwManager._blockExplorerManager.getBlockExplorer().getIdentifier());
      CharSequence[] blockExplorerNames = _mbwManager._blockExplorerManager.getBlockExplorerNames(_mbwManager._blockExplorerManager.getAllBlockExplorer());
      CharSequence[] blockExplorerValues = _mbwManager._blockExplorerManager.getBlockExplorerIds();
      _blockExplorer.setEntries(blockExplorerNames);
      _blockExplorer.setEntryValues(blockExplorerValues);
      _blockExplorer.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

         public boolean onPreferenceChange(Preference preference, Object newValue) {
            _mbwManager.setBlockExplorer(_mbwManager._blockExplorerManager.getBlockExplorerById(newValue.toString()));
            _blockExplorer.setTitle(getBlockExplorerTitle());
            _blockExplorer.setSummary(getBlockExplorerSummary());
            return true;
         }
      });

      //localcurrency
      _localCurrency = findPreference("local_currency");
      _localCurrency.setOnPreferenceClickListener(localCurrencyClickListener);
      _localCurrency.setTitle(localCurrencyTitle());

      // Exchange Source
      _exchangeSource = (ListPreference) findPreference("exchange_source");
      ExchangeRateManager exchangeManager = _mbwManager.getExchangeRateManager();
      List<String> exchangeSourceNamesList = exchangeManager.getExchangeSourceNames();
      CharSequence[] exchangeNames = exchangeSourceNamesList.toArray(new String[exchangeSourceNamesList.size()]);
      _exchangeSource.setEntries(exchangeNames);
      if (exchangeNames.length == 0) {
         _exchangeSource.setEnabled(false);
      } else {
         String currentName = exchangeManager.getCurrentExchangeSourceName();
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
            _mbwManager.getExchangeRateManager().setCurrentExchangeSourceName(newValue.toString());
            _exchangeSource.setTitle(exchangeSourceTitle());
            return true;
         }
      });

      ListPreference language = (ListPreference) findPreference(Constants.LANGUAGE_SETTING);
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
            WalletApplication.applyLanguageChange(getBaseContext(), lang);

            restart();

            return true;
         }
      });

      // Set PIN
      Preference setPin = Preconditions.checkNotNull(findPreference("setPin"));
      setPin.setOnPreferenceClickListener(setPinClickListener);

      // Clear PIN
      updateClearPin();

      // PIN required on startup
      updatePinAtStartup();

      // Legacy backup function
      Preference legacyBackup = Preconditions.checkNotNull(findPreference("legacyBackup"));
      legacyBackup.setOnPreferenceClickListener(legacyBackupClickListener);

      // Legacy backup function
      Preference legacyBackupVerify = Preconditions.checkNotNull(findPreference("legacyBackupVerify"));
      legacyBackupVerify.setOnPreferenceClickListener(legacyBackupVerifyClickListener);

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

      // show bip44 path
      CheckBoxPreference showBip44Path = (CheckBoxPreference) findPreference("showBip44Path");
      showBip44Path.setChecked(_mbwManager.getMetadataStorage().getShowBip44Path());
      showBip44Path.setOnPreferenceClickListener(showBip44PathClickListener);


      // Socks Proxy
      final ListPreference useTor = Preconditions.checkNotNull((ListPreference) findPreference("useTor"));
      useTor.setTitle(getUseTorTitle());

      useTor.setEntries(new String[]{
              getString(R.string.use_https),
              getString(R.string.use_external_tor),
//            getString(R.string.both),
      });

      useTor.setEntryValues(new String[]{
              ServerEndpointType.Types.ONLY_HTTPS.toString(),
              ServerEndpointType.Types.ONLY_TOR.toString(),
              //      ServerEndpointType.Types.HTTPS_AND_TOR.toString(),
      });

      useTor.setValue(_mbwManager.getTorMode().toString());

      useTor.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
         @Override
         public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (newValue.equals(ServerEndpointType.Types.ONLY_TOR.toString())) {
               OrbotHelper obh = new OrbotHelper(SettingsActivity.this);
               if (!obh.isOrbotInstalled()) {
                  obh.promptToInstall(SettingsActivity.this);
               }
            }
            _mbwManager.setTorMode(ServerEndpointType.Types.valueOf((String) newValue));
            useTor.setTitle(getUseTorTitle());
            return true;
         }
      });

      CheckBoxPreference ledgerDisableTee = (CheckBoxPreference) findPreference("ledgerDisableTee");
      Preference ledgerSetUnpluggedAID = findPreference("ledgerUnpluggedAID");

      boolean isTeeAvailable = LedgerTransportTEEProxyFactory.isServiceAvailable(this);
      if (isTeeAvailable) {
         ledgerDisableTee.setChecked(_mbwManager.getLedgerManager().getDisableTEE());
         ledgerDisableTee.setOnPreferenceClickListener(onClickLedgerNotificationDisableTee);
      } else {
         PreferenceCategory ledger = (PreferenceCategory) findPreference("ledger");
         ledger.removePreference(ledgerDisableTee);
      }

      ledgerSetUnpluggedAID.setOnPreferenceClickListener(onClickLedgerSetUnpluggedAID);

      applyLocalTraderEnablement();


      initExternalSettings();

      // external Services

      final PreferenceCategory modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
      if (!CommunicationManager.getInstance(this).getPairedModules().isEmpty()) {
         for (final Module module : CommunicationManager.getInstance(this).getPairedModules()) {
            final ButtonPreference preference = new ButtonPreference(this);
            preference.setLayoutResource(R.layout.preference_layout);
            preference.setTitle(Html.fromHtml(module.getName()));
            preference.setKey("Module_" + module.getModulePackage());
            updateModulePreference(preference, module, BCHHelper.getBCHSyncProgress(this));
            preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
               @Override
               public boolean onPreferenceClick(Preference preference) {
                  Intent intent = new Intent(com.mycelium.modularizationtools.Constants.getSETTINGS());
                  intent.setPackage(module.getModulePackage());
                  intent.putExtra("callingPackage", getPackageName());
                  try {
                     startActivity(intent);
                  } catch (ActivityNotFoundException e) {
                     Log.e("SettingsActivity", "Something wrong with module", e);
                  }
                  return true;
               }
            });
            preference.setButtonText(getString(R.string.uninstall));
            preference.setButtonClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                  Uri packageUri = Uri.parse("package:" + module.getModulePackage());
                  preference.setEnabled(false);
                  startActivityForResult(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                          .putExtra(Intent.EXTRA_RETURN_RESULT, true), REQUEST_CODE_UNINSTALL);
               }
            });
            modulesPrefs.addPreference(preference);
         }
      } else {
         Preference preference = new Preference(this);
         preference.setTitle(R.string.no_connected_modules);
         modulesPrefs.addPreference(preference);
      }

      for (final Module module : GooglePlayModuleCollection.getModules(this).values()) {
         if (!CommunicationManager.getInstance(this).getPairedModules().contains(module)) {
            ButtonPreference installPreference = new ButtonPreference(this);
            installPreference.setButtonText(getString(R.string.install));
            installPreference.setButtonClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                  Intent installIntent = new Intent(Intent.ACTION_VIEW);
                  installIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" +
                          module.getModulePackage()));
                  startActivity(installIntent);
               }
            });
            installPreference.setTitle(Html.fromHtml(module.getName()));
            installPreference.setSummary(module.getDescription());
            modulesPrefs.addPreference(installPreference);
         }
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_CODE_UNINSTALL) {
         pleaseWait = new ProgressDialog(SettingsActivity.this);
         pleaseWait.setMessage(getString(R.string.module_uninstall_progress));
         pleaseWait.show();
         PreferenceCategory modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
         if (resultCode == RESULT_CANCELED) {
            pleaseWait.dismiss();
            for (int index = 0; index < modulesPrefs.getPreferenceCount(); index++) {
               ButtonPreference preferenceButton = (ButtonPreference) modulesPrefs.getPreference(index);
               preferenceButton.setEnabled(true);
            }
         }
      }
   }

   private void updateModulePreference(Preference preference, Module module, float progress) {
      if (preference != null) {
         DecimalFormat format = new DecimalFormat(progress < 0.1f ? "#.###" : "#");
         preference.setSummary(Html.fromHtml(module.getDescription()
                 + "<br/>"
                 + addColorHtmlTag(getString(R.string.sync_progress, format.format(progress)), "#00CC00")));

      }
   }

   @Subscribe
   public void onSyncStateChanged(SpvSyncChanged syncChanged) {
      PreferenceCategory modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
      String bchPackage = "Module_" + WalletApplication.getSpvModuleName(WalletAccount.Type.BCHBIP44);
      Preference preference = modulesPrefs.findPreference(bchPackage);
      updateModulePreference(preference, syncChanged.module, syncChanged.chainDownloadPercentDone);
   }

   private String addColorHtmlTag(String input, String color) {
      return "<font color=\"" + color + "\">" + input + "</font>";
   }

   void initExternalSettings() {
      final PreferenceCategory external = (PreferenceCategory) findPreference("external");
      final List<BuySellServiceDescriptor> buySellServices = _mbwManager.getEnvironmentSettings().getBuySellServices();

      for (final BuySellServiceDescriptor buySellService : buySellServices) {
         if (!buySellService.showEnableInSettings()) {
            continue;
         }

         final CheckBoxPreference cbService = new CheckBoxPreference(this);
         final String enableTitle = getResources().getString(R.string.settings_service_enabled,
                 getResources().getString(buySellService.title)
         );
         cbService.setTitle(enableTitle);
         cbService.setSummary(buySellService.settingDescription);
         cbService.setChecked(buySellService.isEnabled(_mbwManager));
         cbService.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
               CheckBoxPreference p = (CheckBoxPreference) preference;
               buySellService.setEnabled(_mbwManager, p.isChecked());
               return true;
            }
         });
         external.addPreference(cbService);
      }
   }

   @Override
   protected void onResume() {
      setupLocalTraderSettings();
      showOrHideLegacyBackup();
      _localCurrency.setTitle(localCurrencyTitle());
      _mbwManager.getEventBus().register(this);
      super.onResume();
   }

   private ProgressDialog pleaseWait;


   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @SuppressWarnings("deprecation")
   private void setupLocalTraderSettings() {
      if (!_ltManager.hasLocalTraderAccount()) {
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
      if (legacyPref == null) {
         return; // it was already removed, don't remove it again.
      }

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

   private String getUseTorTitle() {
      if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_HTTPS) {
         return getResources().getString(R.string.useTorOnlyHttps);
      } else if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR) {
         return getResources().getString(R.string.useTorOnlyExternalTor);
      } else {
         return getResources().getString(R.string.useTorBoth);
      }
   }

   private String localCurrencyTitle() {
      if (_mbwManager.hasFiatCurrency()) {
         String currency = _mbwManager.getFiatCurrency();
         if (_mbwManager.getCurrencyList().size() > 1) {
            //multiple selected, add ...
            currency = currency + "...";
         }
         return getResources().getString(R.string.pref_local_currency_with_currency, currency);
      } else {
         //nothing selected
         return getResources().getString(R.string.pref_no_fiat_selected);
      }
   }

   private String exchangeSourceTitle() {
      String name = _mbwManager.getExchangeRateManager().getCurrentExchangeSourceName();
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
              _mbwManager.getMinerFee().getMinerFeeName(this));
   }

   private String getMinerFeeSummary() {
      return getResources().getString(R.string.pref_miner_fee_block_summary,
              Integer.toString(_mbwManager.getMinerFee().getNBlocks()));
   }

   private String getBlockExplorerTitle() {
      return getResources().getString(R.string.block_explorer_title,
              _mbwManager._blockExplorerManager.getBlockExplorer().getTitle());
   }

   private String getBlockExplorerSummary() {
      return getResources().getString(R.string.block_explorer_summary,
              _mbwManager._blockExplorerManager.getBlockExplorer().getTitle());
   }

   @SuppressWarnings("deprecation")
   private void updateClearPin() {
      Preference clearPin = findPreference("clearPin");
      clearPin.setEnabled(_mbwManager.isPinProtected());
      clearPin.setOnPreferenceClickListener(clearPinClickListener);
   }

   private void updatePinAtStartup() {
      CheckBoxPreference setPinRequiredStartup = (CheckBoxPreference) Preconditions.checkNotNull(findPreference("requirePinOnStartup"));
      setPinRequiredStartup.setOnPreferenceChangeListener(setPinOnStartupClickListener);
      setPinRequiredStartup.setEnabled(_mbwManager.isPinProtected());
      setPinRequiredStartup.setChecked(_mbwManager.isPinProtected() && _mbwManager.getPinRequiredOnStartup());
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
   }

   private class SubscribeToServerResponse extends LocalTraderEventSubscriber {

      private Button okButton;
      private EditText emailEdit;

      public SubscribeToServerResponse() {
         super(new Handler());
      }

      @Override
      //TODO: upgrade to android support v7 >>19.1.0
      @SuppressLint("AppCompatCustomView")
      public void onLtTraderInfoFetched(final TraderInfo info, GetTraderInfo request) {
         pleaseWait.dismiss();
         AlertDialog.Builder b = new AlertDialog.Builder(SettingsActivity.this);
         b.setTitle(getString(R.string.lt_set_email_title));
         b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               String email = emailEdit.getText().toString();
               _ltManager.makeRequest(new SetNotificationMail(email));

               if ((info.notificationEmail == null || !info.notificationEmail.equals(email)) && !Strings.isNullOrEmpty(email)) {
                  Utils.showSimpleMessageDialog(SettingsActivity.this, getString(R.string.lt_email_please_verify_message));
               }
            }
         });
         b.setNegativeButton(R.string.cancel, null);

         emailEdit = new EditText(SettingsActivity.this) {
            @Override
            protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {

               super.onTextChanged(text, start, lengthBefore, lengthAfter);
               if (okButton != null) { //setText is also set before the alert is finished constructing
                  boolean validMail = Strings.isNullOrEmpty(text.toString()) || //allow empty email, this removes email notifications
                          Utils.isValidEmailAddress(text.toString());
                  okButton.setEnabled(validMail);
               }
            }
         };
         emailEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
         emailEdit.setText(info.notificationEmail);
         LinearLayout llDialog = new LinearLayout(SettingsActivity.this);
         llDialog.setOrientation(LinearLayout.VERTICAL);
         llDialog.setPadding(10, 10, 10, 10);
         TextView tvInfo = new TextView(SettingsActivity.this);
         tvInfo.setText(getString(R.string.lt_set_email_info));
         llDialog.addView(tvInfo);
         llDialog.addView(emailEdit);
         b.setView(llDialog);
         AlertDialog dialog = b.show();
         okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
         _ltManager.unsubscribe(this);
      }

      @Override
      public void onLtError(int errorCode) {
         pleaseWait.dismiss();
         new Toaster(SettingsActivity.this).toast(getString(R.string.lt_set_email_error), false);
         _ltManager.unsubscribe(this);

      }
   }
}

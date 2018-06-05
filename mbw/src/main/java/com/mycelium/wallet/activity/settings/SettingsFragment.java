package com.mycelium.wallet.activity.settings;

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
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.mrd.bitlib.util.CoinUtil;
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
import com.mycelium.wallet.activity.view.TwoButtonsPreference;
import com.mycelium.wallet.event.SpvSyncChanged;
import com.mycelium.wallet.external.BuySellServiceDescriptor;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wallet.modularisation.GooglePlayModuleCollection;
import com.mycelium.wallet.modularisation.ModularisationVersionHelper;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.guardianproject.onionkit.ui.OrbotHelper;

import static android.app.Activity.RESULT_CANCELED;

public class SettingsFragment extends PreferenceFragment {

    public static final CharMatcher AMOUNT = CharMatcher.JAVA_DIGIT.or(CharMatcher.anyOf(".,"));
    private static final int REQUEST_CODE_UNINSTALL = 1;


    private ListPreference _bitcoinDenomination;
    private Preference _localCurrency;
    private ListPreference _exchangeSource;
    private CheckBoxPreference _ltNotificationSound;
    private CheckBoxPreference _ltMilesKilometers;
    private MbwManager _mbwManager;
    private LocalTraderManager _ltManager;
    private ListPreference _minerFee;
    private ListPreference _blockExplorer;

    private final Preference.OnPreferenceClickListener localCurrencyClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SetLocalCurrencyActivity.callMe(getActivity());
            return true;
        }
    };
    private final Preference.OnPreferenceClickListener setPinClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            _mbwManager.showSetPinDialog(getActivity(), Optional.<Runnable>of(new Runnable() {
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

    private final Preference.OnPreferenceChangeListener setPinOnStartupClickListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object o) {
            _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
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
    private final Preference.OnPreferenceClickListener clearPinClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            _mbwManager.showClearPinDialog(getActivity(), Optional.<Runnable>of(new Runnable() {
                @Override
                public void run() {
                    updateClearPin();
                    updatePinAtStartup();
                }
            }));
            return true;
        }
    };
    private final Preference.OnPreferenceClickListener ltDisableLocalTraderClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SwitchPreference p = (SwitchPreference) preference;
            _ltManager.setLocalTraderDisabled(p.isChecked());
            applyLocalTraderEnablement();
            return true;
        }
    };
    private final Preference.OnPreferenceClickListener ltNotificationSoundClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            _ltManager.setPlaySoundOnTradeNotification(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener showBip44PathClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            _mbwManager.getMetadataStorage().setShowBip44Path(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener ltMilesKilometersClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            _ltManager.setUseMiles(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener onClickLedgerNotificationDisableTee = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            _mbwManager.getLedgerManager().setDisableTEE(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener onClickLedgerSetUnpluggedAID = new Preference.OnPreferenceClickListener() {
        private EditText aidEdit;

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity(), R.style.MyceliumModern_Dialog);
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
                        Utils.showSimpleMessageDialog(getActivity(), getString(R.string.ledger_check_unplugged_aid));
                    } else {
                        _mbwManager.getLedgerManager().setUnpluggedAID(aid);
                    }
                }
            });
            b.setNegativeButton(R.string.cancel, null);

            aidEdit = new EditText(getActivity());
            aidEdit.setInputType(InputType.TYPE_CLASS_TEXT);
            aidEdit.setText(_mbwManager.getLedgerManager().getUnpluggedAID());
            LinearLayout llDialog = new LinearLayout(getActivity());
            llDialog.setOrientation(LinearLayout.VERTICAL);
            llDialog.setPadding(10, 10, 10, 10);
            TextView tvInfo = new TextView(getActivity());
            tvInfo.setText(getString(R.string.ledger_unplugged_aid));
            llDialog.addView(tvInfo);
            llDialog.addView(aidEdit);
            b.setView(llDialog);
            b.show();
            return true;
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);

        _mbwManager = MbwManager.getInstance(getActivity().getApplication());
        _ltManager = _mbwManager.getLocalTraderManager();
        // Bitcoin Denomination
        _bitcoinDenomination = (ListPreference) findPreference("bitcoin_denomination");
        _bitcoinDenomination.setTitle(bitcoinDenominationTitle());
        _bitcoinDenomination.setDefaultValue(_mbwManager.getBitcoinDenomination().toString());
        _bitcoinDenomination.setValue(_mbwManager.getBitcoinDenomination().toString());
        CharSequence[] denominations = new CharSequence[]{CoinUtil.Denomination.BTC.toString(), CoinUtil.Denomination.mBTC.toString(),
                CoinUtil.Denomination.uBTC.toString(), CoinUtil.Denomination.BITS.toString()};
        _bitcoinDenomination.setEntries(denominations);
        _bitcoinDenomination.setEntryValues(denominations);
        _bitcoinDenomination.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _mbwManager.setBitcoinDenomination(CoinUtil.Denomination.fromString(newValue.toString()));
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
        _minerFee.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _mbwManager.setMinerFee(MinerFee.fromString(newValue.toString()));
                _minerFee.setTitle(getMinerFeeTitle());
                _minerFee.setSummary(getMinerFeeSummary());
                String description = _mbwManager.getMinerFee().getMinerFeeDescription(getActivity());
                Utils.showSimpleMessageDialog(getActivity(), description);
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
        _blockExplorer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

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
        _exchangeSource.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

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
        language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String lang = newValue.toString();
                _mbwManager.setLanguage(lang);
                WalletApplication.applyLanguageChange(getActivity().getBaseContext(), lang);

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


        // Local Trader
        SwitchPreference ltDisable = (SwitchPreference) findPreference("ltDisable");
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

        useTor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals(ServerEndpointType.Types.ONLY_TOR.toString())) {
                    OrbotHelper obh = new OrbotHelper(getActivity());
                    if (!obh.isOrbotInstalled()) {
                        obh.promptToInstall(getActivity());
                    }
                }
                _mbwManager.setTorMode(ServerEndpointType.Types.valueOf((String) newValue));
                useTor.setTitle(getUseTorTitle());
                return true;
            }
        });

        CheckBoxPreference ledgerDisableTee = (CheckBoxPreference) findPreference("ledgerDisableTee");
        Preference ledgerSetUnpluggedAID = findPreference("ledgerUnpluggedAID");

        boolean isTeeAvailable = LedgerTransportTEEProxyFactory.isServiceAvailable(getActivity());
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
        if (!CommunicationManager.getInstance().getPairedModules().isEmpty()) {
            processPairedModules(modulesPrefs);
        } else {
            Preference preference = new Preference(getActivity());
            preference.setTitle(R.string.no_connected_modules);
            modulesPrefs.addPreference(preference);
        }
        processUnpairedModules(modulesPrefs);

    }

    List<Preference> preferenceList = new ArrayList<>();

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);


        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
                    preferenceList.add(getPreferenceScreen().getPreference(i));
                }
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                getPreferenceScreen().removeAll();
                for (Preference preference : preferenceList) {
                    getPreferenceScreen().addPreference(preference);
                }
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                getPreferenceScreen().removeAll();
                for (Preference preferenceCat : preferenceList) {
                    if (preferenceCat instanceof PreferenceCategory) {
                        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceCat;
                        for (int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
                            Preference preference = preferenceCategory.getPreference(i);
                            if (preference.getTitle() != null
                                    && preference.getTitle().toString().toLowerCase().contains(s.toLowerCase())
                                    || preference.getSummary() != null
                                    && preference.getSummary().toString().toLowerCase().contains(s.toLowerCase())) {
                                getPreferenceScreen().addPreference(preference);
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

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

    private void processPairedModules(PreferenceCategory modulesPrefs) {
        for (final Module module : CommunicationManager.getInstance().getPairedModules()) {
            final ButtonPreference preference = createUninstallableModulePreference(module);
            modulesPrefs.addPreference(preference);
        }
    }

    private void processUnpairedModules(PreferenceCategory modulesPrefs) {
        for (final Module module : GooglePlayModuleCollection.getModules(getActivity()).values()) {
            if (!CommunicationManager.getInstance().getPairedModules().contains(module)) {
                if (Utils.isAppInstalled(getActivity(), module.getModulePackage()) && ModularisationVersionHelper.isUpdateRequired(getActivity(), module.getModulePackage())) {
                    TwoButtonsPreference preference = createUpdateRequiredPreference(module);
                    modulesPrefs.addPreference(preference);
                    preference.setEnabled(false, true, true);
                } else if (Utils.isAppInstalled(getActivity(), module.getModulePackage())) {
                    final ButtonPreference preference = createUninstallableModulePreference(module);
                    preference.setEnabled(false);
                    preference.setButtonEnabled(true);
                    modulesPrefs.addPreference(preference);
                } else {
                    ButtonPreference installPreference = new ButtonPreference(getActivity());
                    installPreference.setButtonText(getString(R.string.install));
                    installPreference.setButtonClickListener(getInstallClickListener(module));
                    installPreference.setTitle(Html.fromHtml(module.getName()));
                    installPreference.setSummary(module.getDescription());
                    modulesPrefs.addPreference(installPreference);
                }
            }
        }
    }

    @NonNull
    private ButtonPreference createUninstallableModulePreference(final Module module) {
        final ButtonPreference preference = new ButtonPreference(getActivity());
        preference.setLayoutResource(R.layout.preference_module_layout);
        preference.setTitle(Html.fromHtml(module.getName()));
        preference.setKey("Module_" + module.getModulePackage());
        preference.setIcon(R.drawable.image_bch_module);
        updateModulePreference(preference, module, BCHHelper.getBCHSyncProgress(getActivity()));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(com.mycelium.modularizationtools.Constants.getSETTINGS());
                intent.setPackage(module.getModulePackage());
                intent.putExtra("callingPackage", getActivity().getPackageName());
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
        preference.setUnderIconText(module.getShortName());
        return preference;
    }

    private TwoButtonsPreference createUpdateRequiredPreference(final Module module) {
        final TwoButtonsPreference preference = new TwoButtonsPreference(getActivity());
        preference.setLayoutResource(R.layout.preference_layout);
        preference.setTitle(Html.fromHtml(module.getName()));
        preference.setKey("Module_" + module.getModulePackage());
//        updateModulePreference(preference, module, BCHHelper.getBCHSyncProgress(getActivity()));
        preference.setButtonsText(getString(R.string.uninstall), getString(R.string.update));
        preference.setTopButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri packageUri = Uri.parse("package:" + module.getModulePackage());
                preference.setEnabled(false, false, false);
                startActivityForResult(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
                        .putExtra(Intent.EXTRA_RETURN_RESULT, true), REQUEST_CODE_UNINSTALL);
            }
        });
        preference.setBottomButtonClickListener(getInstallClickListener(module));
        return preference;
    }

    @NonNull
    private View.OnClickListener getInstallClickListener(final Module module) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" +
                        module.getModulePackage()));
                startActivity(installIntent);
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_UNINSTALL) {
            pleaseWait = new ProgressDialog(getActivity());
            pleaseWait.setMessage(getString(R.string.module_uninstall_progress));
            pleaseWait.show();
            PreferenceCategory modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
            if (resultCode == RESULT_CANCELED) {
                pleaseWait.dismiss();
                for (int index = 0; index < modulesPrefs.getPreferenceCount(); index++) {
                    Preference preferenceButton = modulesPrefs.getPreference(index);
                    if (preferenceButton instanceof ButtonPreference) {
                        if (ModularisationVersionHelper.isUpdateRequired(getActivity(), preferenceButton.getKey().replace("Module_", "")))
                            preferenceButton.setEnabled(true);
                    } else if (preferenceButton instanceof TwoButtonsPreference) {
                        ((TwoButtonsPreference) preferenceButton).setEnabled(false, true, true);
                    }
                }
            }
        }
    }

    private void updateModulePreference(ButtonPreference preference, Module module, float progress) {
        if (preference != null) {
            DecimalFormat format = new DecimalFormat(progress < 0.1f ? "#.###" : "#.##");

            String syncStatus = progress == 100F ? getString(R.string.fully_synced)
                    : getString(R.string.sync_progress, format.format(progress));
            preference.setSummary(Html.fromHtml(module.getDescription()
                    /*+ "<br/><br/>"
                    + addColorHtmlTag(syncStatus, "#00CC00")*/));

            preference.setSyncStateText(syncStatus);
        }
    }

    @Subscribe
    public void onSyncStateChanged(SpvSyncChanged syncChanged) {
        PreferenceCategory modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
        String bchPackage = "Module_" + WalletApplication.getSpvModuleName(WalletAccount.Type.BCHBIP44);
        Preference preference = modulesPrefs.findPreference(bchPackage);
        updateModulePreference((ButtonPreference) preference, syncChanged.module, syncChanged.chainDownloadPercentDone);
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

            final CheckBoxPreference cbService = new CheckBoxPreference(getActivity());
            final String enableTitle = getResources().getString(R.string.settings_service_enabled,
                    getResources().getString(buySellService.title)
            );
            cbService.setTitle(enableTitle);
            cbService.setSummary(buySellService.settingDescription);
            cbService.setChecked(buySellService.isEnabled(_mbwManager));
            cbService.setWidgetLayoutResource(R.layout.preference_checkbox);
            cbService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    buySellService.setEnabled(_mbwManager, p.isChecked());
                    return true;
                }
            });
            external.addPreference(cbService);
        }
        if (!SettingsPreference.getInstance().isEndedMyDFS()) {
            final CheckBoxPreference cbService = new CheckBoxPreference(getActivity());
            cbService.setTitle(R.string.settings_mydfs_title);
            cbService.setSummary(R.string.settings_mydfs_summary);
            cbService.setChecked(SettingsPreference.getInstance().isMyDFSEnabled());
            cbService.setWidgetLayoutResource(R.layout.preference_checkbox);
            cbService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    SettingsPreference.getInstance().setEnableMyDFS(p.isChecked());
                    return true;
                }
            });
            external.addPreference(cbService);
        }

        if (!SettingsPreference.getInstance().isEndedApex()) {
            final CheckBoxPreference cbServiceApex = new CheckBoxPreference(getActivity());
            cbServiceApex.setTitle(R.string.settings_apex_title);
            cbServiceApex.setSummary(R.string.settings_apex_summary);
            cbServiceApex.setChecked(SettingsPreference.getInstance().isApexEnabled());
            cbServiceApex.setWidgetLayoutResource(R.layout.preference_checkbox);
            cbServiceApex.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    SettingsPreference.getInstance().setEnableApex(p.isChecked());
                    return true;
                }
            });
            external.addPreference(cbServiceApex);
        }
    }

    @Override
    public void onResume() {
        setupLocalTraderSettings();
        _localCurrency.setTitle(localCurrencyTitle());
        _mbwManager.getEventBus().register(this);
        super.onResume();
    }

    private ProgressDialog pleaseWait;


    @Override
    public void onPause() {
        _mbwManager.getEventBus().unregister(this);
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    private void setupLocalTraderSettings() {
        if (!_ltManager.hasLocalTraderAccount()) {
            PreferenceCategory localTraderPrefs = (PreferenceCategory) findPreference("localtraderPrefs");
            SwitchPreference disableLt = (SwitchPreference) findPreference("ltDisable");
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
    private void setupEmailNotificationSetting() {
        Preference ltNotificationEmail = findPreference("ltNotificationEmail2");
        ltNotificationEmail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
                pleaseWait = ProgressDialog.show(getActivity(), getString(R.string.fetching_info),
                        getString(R.string.please_wait), true);
                return true;
            }
        });
    }

    private String getLanguageSettingTitle() {
        String displayed = getResources().getString(R.string.pref_language);
        String english = Utils.getResourcesByLocale(getResources(), "en")
                .getString(R.string.pref_language);
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
        Intent running = getActivity().getIntent();
        getActivity().finish();
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
                _mbwManager.getMinerFee().getMinerFeeName(getActivity()));
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

    private class SubscribeToServerResponse extends LocalTraderEventSubscriber {

        private Button okButton;
        private EditText emailEdit;

        public SubscribeToServerResponse() {
            super(new Handler());
        }

        @Override
        public void onLtTraderInfoFetched(final TraderInfo info, GetTraderInfo request) {
            pleaseWait.dismiss();
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setTitle(getString(R.string.lt_set_email_title));
            b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String email = emailEdit.getText().toString();
                    _ltManager.makeRequest(new SetNotificationMail(email));

                    if ((info.notificationEmail == null || !info.notificationEmail.equals(email)) && !Strings.isNullOrEmpty(email)) {
                        Utils.showSimpleMessageDialog(getActivity(), getString(R.string.lt_email_please_verify_message));
                    }
                }
            });
            b.setNegativeButton(R.string.cancel, null);

            emailEdit = new AppCompatEditText(getActivity()) {
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
            LinearLayout llDialog = new LinearLayout(getActivity());
            llDialog.setOrientation(LinearLayout.VERTICAL);
            llDialog.setPadding(10, 10, 10, 10);
            TextView tvInfo = new TextView(getActivity());
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
            new Toaster(getActivity()).toast(getString(R.string.lt_set_email_error), false);
            _ltManager.unsubscribe(this);
        }
    }

}

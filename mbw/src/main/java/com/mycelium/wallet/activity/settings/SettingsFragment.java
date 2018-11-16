package com.mycelium.wallet.activity.settings;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.model.AddressType;
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
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler;
import com.mycelium.wallet.activity.view.ButtonPreference;
import com.mycelium.wallet.activity.view.TwoButtonsPreference;
import com.mycelium.wallet.event.SpvSyncChanged;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wallet.modularisation.GooglePlayModuleCollection;
import com.mycelium.wallet.modularisation.ModularisationVersionHelper;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Subscribe;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import info.guardianproject.onionkit.ui.OrbotHelper;

import static android.app.Activity.RESULT_CANCELED;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final CharMatcher AMOUNT = CharMatcher.javaDigit().or(CharMatcher.anyOf(".,"));
    private static final int REQUEST_CODE_UNINSTALL = 1;
    // adding extra info to preferences (for search)
    private static final String TAG = "settingsfragmenttag";

    private SearchView searchView;

    private ListPreference language;
    private ListPreference _bitcoinDenomination;
    private Preference _localCurrency;
    private ListPreference _exchangeSource;
    private CheckBoxPreference _ltNotificationSound;
    private CheckBoxPreference _ltMilesKilometers;
    private MbwManager _mbwManager;
    private LocalTraderManager _ltManager;
    private ListPreference _minerFee;
    private ListPreference _blockExplorer;
    private Preference changeAddressType;
    private Preference notificationPreference;
    private CheckBoxPreference useTor;
    private PreferenceCategory modulesPrefs;
    private Preference externalPreference;
    private Preference versionPreference;
    private CheckBoxPreference localTraderDisable;
    private CheckBoxPreference ledgerDisableTee;
    private Preference ledgerSetUnpluggedAID;
    private CheckBoxPreference showBip44Path;
    private PreferenceCategory ledger;
    private Preference ltNotificationEmail;
    private PreferenceCategory localTraderPrefs;

    private DisplayPreferenceDialogHandler displayPreferenceDialogHandler;

    // sub screens and their preferences
    private PreferenceScreen backupPreferenceScreen;
    private List<Preference> backupPrefs = new ArrayList<>();
    private PreferenceScreen pinPreferenceScreen;
    private List<Preference> pinPrefs = new ArrayList<>();

    // lists to manipulate with search
    private List<Preference> rootPreferenceList = new ArrayList<>();
    private List<Preference> foundPrefList = new ArrayList<>();

    private final Preference.OnPreferenceClickListener localCurrencyClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SetLocalCurrencyActivity.callMe(getActivity());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener ltDisableLocalTraderClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            _ltManager.setLocalTraderEnabled(p.isChecked());
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

    private final Preference.OnPreferenceClickListener segwitChangeAddressClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SetSegwitChangeActivity.callMe(getActivity());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener onClickLedgerSetUnpluggedAID = new Preference.OnPreferenceClickListener() {
        private EditText aidEdit;

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity(), R.style.MyceliumSettings_Dialog_Small);
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
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_ledger_aid, null);
            aidEdit = view.findViewById(R.id.edit_text);
            aidEdit.setInputType(InputType.TYPE_CLASS_TEXT);
            aidEdit.setText(_mbwManager.getLedgerManager().getUnpluggedAID());
            b.setView(view);
            b.show();
            return true;
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);

        _mbwManager = MbwManager.getInstance(getActivity().getApplication());
        _ltManager = _mbwManager.getLocalTraderManager();
        // Bitcoin Denomination
        _bitcoinDenomination = (ListPreference) findPreference(Constants.SETTING_DENOMINATION);
        // Miner Fee
        _minerFee = (ListPreference) findPreference(Constants.SETTING_MINER_FEE);
        //Block Explorer
        _blockExplorer = (ListPreference) findPreference("block_explorer");
        // Transaction change address type
        changeAddressType = findPreference("change_type");
        //localcurrency
        _localCurrency = findPreference("local_currency");
        // Exchange Source
        _exchangeSource = (ListPreference) findPreference("exchange_source");
        language = (ListPreference) findPreference(Constants.LANGUAGE_SETTING);
        notificationPreference = findPreference("notifications");
        // Socks Proxy
        useTor = (CheckBoxPreference) Preconditions.checkNotNull(findPreference(Constants.SETTING_TOR));
        showBip44Path = (CheckBoxPreference) findPreference("showBip44Path");
        ledgerDisableTee = (CheckBoxPreference) findPreference("ledgerDisableTee");
        ledgerSetUnpluggedAID = findPreference("ledgerUnpluggedAID");
        // external Services
        modulesPrefs = (PreferenceCategory) findPreference("modulesPrefs");
        localTraderDisable = (CheckBoxPreference) findPreference("ltDisable");
        externalPreference = findPreference("external_services");
        versionPreference = findPreference("updates");
        _ltNotificationSound = (CheckBoxPreference) findPreference("ltNotificationSound");
        _ltMilesKilometers = (CheckBoxPreference) findPreference("ltMilesKilometers");
        ledger = (PreferenceCategory) findPreference("ledger");
        ltNotificationEmail = findPreference("ltNotificationEmail2");
        localTraderPrefs = (PreferenceCategory) findPreference("localtraderPrefs");

        displayPreferenceDialogHandler = new DisplayPreferenceDialogHandler(getActivity());

        // sub screens and sub prefs
        backupPreferenceScreen = (PreferenceScreen) findPreference("backup");
        createSubPreferences(backupPreferenceScreen, backupPrefs);
        pinPreferenceScreen = (PreferenceScreen) findPreference("pincode");
        createSubPreferences(pinPreferenceScreen, pinPrefs);


        // adding prefs for search
        rootPreferenceList.clear();
        foundPrefList.clear();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            rootPreferenceList.add(getPreferenceScreen().getPreference(i));
            foundPrefList.add(getPreferenceScreen().getPreference(i));
        }
        // adding hidden prefs
        List<PreferenceScreen> hiddenPreferencesScreenList = Arrays.asList(backupPreferenceScreen, pinPreferenceScreen);
        for (PreferenceScreen preferenceScreen: hiddenPreferencesScreenList) {
            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                foundPrefList.add(preferenceScreen.getPreference(i));
            }
        }
    }

    /**
     * Initializes sub preferences that are in preferences.xml
     * @param preferenceScreen The PreferenceScreen that hosts sub-settings
     * @param subPrefs Array list is needed to bind the preferences in bindSubPrefs()
     */
    private void createSubPreferences(PreferenceScreen preferenceScreen, List<Preference> subPrefs) {
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            if (preferenceScreen.getPreference(i) instanceof PreferenceCategory) {
                PreferenceCategory prefCat = (PreferenceCategory) preferenceScreen.getPreference(i);
                for (int j = 0; j < prefCat.getPreferenceCount(); j++) {
                    Preference pref = findPreference(prefCat.getPreference(j).getKey());
                    subPrefs.add(pref);
                }
            }
            else {
                Preference pref = findPreference(preferenceScreen.getPreference(i).getKey());
                subPrefs.add(pref);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        getListView().removeItemDecorationAt(0);
        DividerDecoration dividerDecoration = new DividerDecoration();
        dividerDecoration.setDivider(getResources().getDrawable(R.drawable.pref_list_divider));
        dividerDecoration.setAllowDividerAfterLastItem(false);
        getListView().addItemDecoration(dividerDecoration);
        return view;
    }

    @Override
    protected void onBindPreferences() {
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
        if (notificationPreference != null) {
            notificationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new NotificationsFragment())
                            .addToBackStack("pincode")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }

        pinPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, PinCodeFragment.newInstance(pinPreferenceScreen.getKey()))
                        .addToBackStack("pincode")
                        .commitAllowingStateLoss();
                return true;
            }
        });
        pinPreferenceScreen.setSummary(_mbwManager.isPinProtected() ? "On" : "Off");

        useTor.setTitle(getUseTorTitle());
        useTor.setChecked(_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR);
        useTor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (useTor.isChecked()) {
                    OrbotHelper obh = new OrbotHelper(getActivity());
                    if (!obh.isOrbotInstalled()) {
                        obh.promptToInstall(getActivity());
                        useTor.setChecked(false);
                    }
                }
                _mbwManager.setTorMode(useTor.isChecked()
                        ? ServerEndpointType.Types.ONLY_TOR : ServerEndpointType.Types.ONLY_HTTPS);
                return true;
            }
        });

        backupPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, BackupFragment.newInstance(backupPreferenceScreen.getKey()))
                        .addToBackStack("backup")
                        .commitAllowingStateLoss();
                return true;
            }
        });
        modulesPrefs.removeAll();
        if (!CommunicationManager.getInstance().getPairedModules().isEmpty()) {
            processPairedModules(modulesPrefs);
        }
        processUnpairedModules(modulesPrefs);

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
                return true;
            }
        });
        _localCurrency.setOnPreferenceClickListener(localCurrencyClickListener);
        _localCurrency.setTitle(localCurrencyTitle());

        ExchangeRateManager exchangeManager = _mbwManager.getExchangeRateManager();
        List<String> exchangeSourceNamesList = exchangeManager.getExchangeSourceNames();
        Collections.sort(exchangeSourceNamesList, new Comparator<String>() {
            @Override
            public int compare(String rate1, String rate2) {
                return rate1.compareToIgnoreCase(rate2);
            }
        });

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
        _exchangeSource.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _mbwManager.getExchangeRateManager().setCurrentExchangeSourceName(newValue.toString());
                return true;
            }
        });
        _blockExplorer.setValue(_mbwManager._blockExplorerManager.getBlockExplorer().getIdentifier());
        CharSequence[] blockExplorerNames = _mbwManager._blockExplorerManager.getBlockExplorerNames(_mbwManager._blockExplorerManager.getAllBlockExplorer());
        CharSequence[] blockExplorerValues = _mbwManager._blockExplorerManager.getBlockExplorerIds();
        _blockExplorer.setEntries(blockExplorerNames);
        _blockExplorer.setEntryValues(blockExplorerValues);
        _blockExplorer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                _mbwManager.setBlockExplorer(_mbwManager._blockExplorerManager.getBlockExplorerById(newValue.toString()));
                return true;
            }
        });

        changeAddressType.setOnPreferenceClickListener(segwitChangeAddressClickListener);

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
                _minerFee.setSummary(getMinerFeeSummary());
                String description = _mbwManager.getMinerFee().getMinerFeeDescription(getActivity());
                Utils.showSimpleMessageDialog(getActivity(), description);
                return true;
            }
        });

        // Local Trader
        localTraderDisable.setChecked(_ltManager.isLocalTraderEnabled());
        localTraderDisable.setOnPreferenceClickListener(ltDisableLocalTraderClickListener);


        _ltNotificationSound.setChecked(_ltManager.getPlaySoundOnTradeNotification());
        _ltNotificationSound.setOnPreferenceClickListener(ltNotificationSoundClickListener);


        _ltMilesKilometers.setChecked(_ltManager.useMiles());
        _ltMilesKilometers.setOnPreferenceClickListener(ltMilesKilometersClickListener);
        // show bip44 path
        showBip44Path.setChecked(_mbwManager.getMetadataStorage().getShowBip44Path());
        showBip44Path.setOnPreferenceClickListener(showBip44PathClickListener);
        boolean isTeeAvailable = LedgerTransportTEEProxyFactory.isServiceAvailable(getActivity());
        if (isTeeAvailable) {
            ledgerDisableTee.setChecked(_mbwManager.getLedgerManager().getDisableTEE());
            ledgerDisableTee.setOnPreferenceClickListener(onClickLedgerNotificationDisableTee);
        } else {
            ledger.removePreference(ledgerDisableTee);
        }

        ledgerSetUnpluggedAID.setOnPreferenceClickListener(onClickLedgerSetUnpluggedAID);
        applyLocalTraderEnablement();

        externalPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, new ExternalServiceFragment())
                        .addToBackStack("external_services")
                        .commitAllowingStateLoss();
                return true;
            }
        });
        versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, new VersionFragment())
                        .addToBackStack("version")
                        .commitAllowingStateLoss();
                return true;
            }
        });

        bindSubPrefs();
    }

    private void bindSubPrefs() {
        // each i corresponds to openType in simulateClick(i) in sub-setting's fragment

        for (int i = 0; i < backupPrefs.size(); i++) {
            Preference pref = backupPrefs.get(i);

            final int finalI = i;
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    BackupFragment backupFragment = BackupFragment.newInstance(backupPreferenceScreen.getKey());
                    backupFragment.getArguments().putInt(BackupFragment.ARG_FRAGMENT_OPEN_TYPE, finalI);

                    getFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, backupFragment)
                            .addToBackStack("backup")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }

        for (int i = 0; i < pinPrefs.size(); i++) {
            Preference pref = pinPrefs.get(i);

            final int finalI = i;
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PinCodeFragment pinFragment = PinCodeFragment.newInstance(pinPreferenceScreen.getKey());
                    pinFragment.getArguments().putInt(PinCodeFragment.ARG_FRAGMENT_OPEN_TYPE, finalI);

                    getFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, pinFragment)
                            .addToBackStack("pincode")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                refreshPreferences();
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                findSearchResult(s);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.length() == 0) {
                    refreshPreferences();
                }
                else {
                    findSearchResult(s);
                }
                return true;
            }

            private void findSearchResult(String s) {
                getPreferenceScreen().removeAll();
                List<Preference> prefs = new ArrayList<>();
                for (Preference preferenceCat : foundPrefList) {
                    prefs.clear();

                    if (preferenceCat instanceof PreferenceCategory) {
                        PreferenceCategory preferenceCategory = (PreferenceCategory) preferenceCat;

                        for (int i = 0; i < preferenceCategory.getPreferenceCount(); i++) {
                            Preference preference = preferenceCategory.getPreference(i);

                            if ((isTitleValid(preference, s) || isSummaryValid(preference, s))
                                    && isIndependent(preference)) {
                                prefs.add(preference);
                            }
                        }
                        if(!prefs.isEmpty()) {
                            PreferenceCategory preferenceCategory1 = new PreferenceCategory(getContext());
                            preferenceCategory1.setLayoutResource(preferenceCategory.getLayoutResource());
                            preferenceCategory1.setTitle(preferenceCategory.getTitle());
                            getPreferenceScreen().addPreference(preferenceCategory1);
                            for (Preference pref : prefs) {
                                preferenceCategory1.addPreference(pref);
                            }
                        }
                    } else {
                        if ((isTitleValid(preferenceCat, s) || isSummaryValid(preferenceCat, s))
                                && isIndependent(preferenceCat)) {
                            getPreferenceScreen().addPreference(preferenceCat);
                        }
                    }
                }
            }

            private boolean isTitleValid(Preference preference, String search) {
                return preference.getTitle() != null
                        && preference.getTitle().toString().toLowerCase().contains(search.toLowerCase());
            }

            private boolean isSummaryValid(Preference preference, String search) {
                return preference.getSummary() != null
                        && preference.getSummary().toString().toLowerCase().contains(search.toLowerCase());
            }

            private boolean isIndependent(Preference preference) {
                return preference.getDependency() == null;
            }

        });
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setTitle(R.string.settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if(searchView.isIconified())
                getActivity().finish();
            else
                searchView.setIconified(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        preference.setWidgetLayoutResource(R.layout.preference_button_uninstall);
        preference.setTitle(Html.fromHtml(module.getName()));
        preference.setKey("Module_" + module.getModulePackage());
        updateModulePreference(preference, module);
        updateModuleSyncAsync(preference);
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
        return preference;
    }

    private TwoButtonsPreference createUpdateRequiredPreference(final Module module) {
        final TwoButtonsPreference preference = new TwoButtonsPreference(getActivity());
        preference.setTitle(Html.fromHtml(module.getName()));
        preference.setKey("Module_" + module.getModulePackage());
        updateModulePreference(preference, module);
        updateModuleSyncAsync(preference);
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

    private void updateModuleSyncAsync(final ModulePreference preference) {
        new UpdateModuleSync(preference).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static class UpdateModuleSync extends AsyncTask<Void, Void, Float> {
        private ModulePreference preference;

        public UpdateModuleSync(ModulePreference preference) {
            this.preference = preference;
        }

        @Override
        protected Float doInBackground(Void... voids) {
            return BCHHelper.getBCHSyncProgress(preference.getContext());
        }

        @Override
        protected void onPostExecute(Float aFloat) {
            super.onPostExecute(aFloat);
            updateModulePreferenceSync(preference, aFloat);
        }
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
            if (resultCode == RESULT_CANCELED) {
                pleaseWait.dismiss();
                for (int index = 0; index < modulesPrefs.getPreferenceCount(); index++) {
                    Preference preferenceButton = modulesPrefs.getPreference(index);
                    if (preferenceButton instanceof ButtonPreference) {
                        if (!ModularisationVersionHelper.isUpdateRequired(getActivity(), preferenceButton.getKey().replace("Module_", ""))) {
                            preferenceButton.setEnabled(true);
                        }
                    } else if (preferenceButton instanceof TwoButtonsPreference) {
                        ((TwoButtonsPreference) preferenceButton).setEnabled(false, true, true);
                    }
                }
            }
        }
    }

    private void updateModulePreference(Preference preference, Module module) {
        if (preference != null) {
            preference.setSummary(Html.fromHtml(module.getDescription()));
        }
    }

    private static void updateModulePreferenceSync(ModulePreference preference, float progress) {
        DecimalFormat format = new DecimalFormat(progress < 0.1f ? "#.###" : "#.##");
        String syncStatus = progress == 100F ? preference.getContext().getString(R.string.fully_synced)
                : preference.getContext().getString(R.string.sync_progress, format.format(progress));
        preference.setSyncStateText(syncStatus);
    }

    @Subscribe
    public void onSyncStateChanged(SpvSyncChanged syncChanged) {
        String bchPackage = "Module_" + WalletApplication.getSpvModuleName(WalletAccount.Type.BCHBIP44);
        Preference preference = modulesPrefs.findPreference(bchPackage);
        updateModulePreferenceSync((ButtonPreference) preference, syncChanged.chainDownloadPercentDone);
    }

    @Override
    public void onResume() {
        setupLocalTraderSettings();
        _localCurrency.setTitle(localCurrencyTitle());
        _localCurrency.setSummary(localCurrencySummary());
        _mbwManager.getEventBus().register(this);
        modulesPrefs.removeAll();
        if (!CommunicationManager.getInstance().getPairedModules().isEmpty()) {
            processPairedModules(modulesPrefs);
        }
        processUnpairedModules(modulesPrefs);
        super.onResume();
    }

    private ProgressDialog pleaseWait;


    @Override
    public void onPause() {
        _mbwManager.getEventBus().unregister(this);
        refreshPreferences();
        if (pleaseWait != null) pleaseWait.dismiss();
        super.onPause();
    }

    private void refreshPreferences()
    {
        getPreferenceScreen().removeAll();
        for (Preference preference : rootPreferenceList)
            getPreferenceScreen().addPreference(preference);
    }

    @SuppressWarnings("deprecation")
    private void setupLocalTraderSettings() {
        if (!_ltManager.hasLocalTraderAccount()) {
            if (localTraderPrefs != null) {
                localTraderPrefs.removeAll();
                //its important we keep this prefs, so users can still enable / disable lt without having an account
                localTraderPrefs.addPreference(localTraderDisable);
            }
            return;
        }
        setupEmailNotificationSetting();
    }

    @SuppressWarnings("deprecation")
    private void setupEmailNotificationSetting() {
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
        String english = Utils.getResourcesByLocale(getActivity(), "en")
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
        boolean ltEnabled = _ltManager.isLocalTraderEnabled();
        _ltNotificationSound.setEnabled(ltEnabled);
        _ltMilesKilometers.setEnabled(ltEnabled);
    }

    private String getUseTorTitle() {
        return getResources().getString(R.string.useTor);
    }

    private String localCurrencyTitle() {
        return getResources().getString(R.string.pref_local_currency);
    }

    private String localCurrencySummary() {
        if (_mbwManager.hasFiatCurrency()) {
            String currency = _mbwManager.getFiatCurrency();
            List<String> currencyList = _mbwManager.getCurrencyList();
            currencyList.remove(currency);
            for (int i = 0; i < Math.min(currencyList.size(), 2); i++) {
                //noinspection StringConcatenationInLoop
                currency += ", " + currencyList.get(i);
            }
            if (_mbwManager.getCurrencyList().size() > 3) {
                //multiple selected, add ...
                currency += "...";
            }
            return currency;
        } else {
            //nothing selected
            return getResources().getString(R.string.pref_no_fiat_selected);
        }
    }

    private String getMinerFeeSummary() {
        return getResources().getString(R.string.pref_miner_fee_block_summary,
                Integer.toString(_mbwManager.getMinerFee().getNBlocks()));
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
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity(), R.style.MyceliumSettings_Dialog);
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

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        displayPreferenceDialogHandler.onDisplayPreferenceDialog(preference);
    }

    private class DividerDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDivider;
        private int mDividerHeight;
        private boolean mAllowDividerAfterLastItem = true;

        private int iconDividerLeft;
        private Paint paint = new Paint() {{
            setColor(Color.parseColor("#2c2c2c"));
        }};

        DividerDecoration() {
            iconDividerLeft = getResources().getDimensionPixelSize(R.dimen.pref_divider_margin);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (mDivider == null) {
                return;
            }
            final int childCount = parent.getChildCount();
            final int width = parent.getWidth();
            for (int childViewIndex = 0; childViewIndex < childCount; childViewIndex++) {
                final View view = parent.getChildAt(childViewIndex);
                if (shouldDrawDividerBelow(view, parent)) {
                    int left = 0;
                    if (view.findViewById(android.R.id.icon) != null) {
                        left = iconDividerLeft;
                    }
                    int top = (int) view.getY() + view.getHeight();
                    c.drawRect(0, top, width, top + mDividerHeight, paint);
                    mDivider.setBounds(left, top, width, top + mDividerHeight);
                    mDivider.draw(c);
                }
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            if (shouldDrawDividerBelow(view, parent)) {
                outRect.bottom = mDividerHeight;
            }
        }

        private boolean shouldDrawDividerBelow(View view, RecyclerView parent) {
            final RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
            final boolean dividerAllowedBelow = holder instanceof PreferenceViewHolder
                    && ((PreferenceViewHolder) holder).isDividerAllowedBelow();
            if (!dividerAllowedBelow) {
                return false;
            }
            boolean nextAllowed = mAllowDividerAfterLastItem;
            int index = parent.indexOfChild(view);
            if (index < parent.getChildCount() - 1) {
                final View nextView = parent.getChildAt(index + 1);
                final RecyclerView.ViewHolder nextHolder = parent.getChildViewHolder(nextView);
                nextAllowed = nextHolder instanceof PreferenceViewHolder
                        && ((PreferenceViewHolder) nextHolder).isDividerAllowedAbove();
            }
            return nextAllowed;
        }

        public void setDivider(Drawable divider) {
            if (divider != null) {
                mDividerHeight = divider.getIntrinsicHeight();
            } else {
                mDividerHeight = 0;
            }
            mDivider = divider;
            getListView().invalidateItemDecorations();
        }

        public void setDividerHeight(int dividerHeight) {
            mDividerHeight = dividerHeight;
            getListView().invalidateItemDecorations();
        }

        public void setAllowDividerAfterLastItem(boolean allowDividerAfterLastItem) {
            mAllowDividerAfterLastItem = allowDividerAfterLastItem;
        }
    }
}

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
import android.os.Bundle;
import android.os.Handler;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.SearchView;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.AboutActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler;
import com.mycelium.wallet.activity.view.ButtonPreference;
import com.mycelium.wallet.activity.view.TwoButtonsPreference;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.GetTraderInfo;
import com.mycelium.wallet.lt.api.SetNotificationMail;
import com.mycelium.wapi.wallet.coins.AssetInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import info.guardianproject.onionkit.ui.OrbotHelper;

import static android.app.Activity.RESULT_CANCELED;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_CODE_UNINSTALL = 1;
    // adding extra info to preferences (for search)
    private SearchView searchView;

    private ListPreference language;
    private PreferenceScreen denominationScreen;
    private Preference localCurrency;
    private PreferenceScreen exchangeSourceScreen;
    private CheckBoxPreference ltNotificationSound;
    private CheckBoxPreference ltMilesKilometers;
    private MbwManager mbwManager;
    private LocalTraderManager ltManager;
    private PreferenceScreen minerFeeScreen;
    private PreferenceScreen blockExplorerScreen;
    private Preference changeAddressType;
    private Preference notificationPreference;
    private CheckBoxPreference useTor;
    private Preference externalPreference;
    private Preference versionPreference;
    private CheckBoxPreference localTraderDisable;
    private CheckBoxPreference ledgerDisableTee;
    private Preference ledgerSetUnpluggedAID;
    private CheckBoxPreference showBip44Path;
    private PreferenceCategory ledger;
    private Preference ltNotificationEmail;
    private PreferenceCategory localTraderPrefs;
    private Preference aboutPrefs;
    private Preference helpPrefs;

    private DisplayPreferenceDialogHandler displayPreferenceDialogHandler;

    // sub screens and their preferences
    private PreferenceScreen backupPreferenceScreen;
    private List<Preference> backupPrefs = new ArrayList<>();
    private PreferenceScreen pinPreferenceScreen;
    private List<Preference> pinPrefs = new ArrayList<>();

    private ProgressDialog pleaseWait;

    private boolean isSearchViewOpen = false;

    private final Preference.OnPreferenceClickListener localCurrencyClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SetLocalCurrencyActivity.callMe(getActivity());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener ltDisableLocalTraderClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            ltManager.setLocalTraderEnabled(p.isChecked());
            applyLocalTraderEnablement();
            return true;
        }
    };
    private final Preference.OnPreferenceClickListener ltNotificationSoundClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            ltManager.setPlaySoundOnTradeNotification(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener showBip44PathClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            mbwManager.getMetadataStorage().setShowBip44Path(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener ltMilesKilometersClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            ltManager.setUseMiles(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener onClickLedgerNotificationDisableTee = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            CheckBoxPreference p = (CheckBoxPreference) preference;
            mbwManager.getLedgerManager().setDisableTEE(p.isChecked());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener segwitChangeAddressClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SetSegwitChangeActivity.callMe(requireActivity());
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener onClickLedgerSetUnpluggedAID = new Preference.OnPreferenceClickListener() {
        private EditText aidEdit;

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder b = new AlertDialog.Builder(requireActivity(), R.style.MyceliumSettings_Dialog_Small);
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
                        mbwManager.getLedgerManager().setUnpluggedAID(aid);
                    }
                }
            });
            b.setNegativeButton(R.string.cancel, null);
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_ledger_aid, null);
            aidEdit = view.findViewById(R.id.edit_text);
            aidEdit.setInputType(InputType.TYPE_CLASS_TEXT);
            aidEdit.setText(mbwManager.getLedgerManager().getUnpluggedAID());
            b.setView(view);
            b.show();
            return true;
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);

        mbwManager = MbwManager.getInstance(requireActivity().getApplication());
        ltManager = mbwManager.getLocalTraderManager();
        displayPreferenceDialogHandler = new DisplayPreferenceDialogHandler(getActivity());
        assignPreferences();
    }

    private void assignPreferences() {
        // Denomination
        denominationScreen = findPreference(Constants.SETTING_DENOMINATION);
        // Miner Fee
        minerFeeScreen = findPreference(Constants.SETTING_MINER_FEE);
        //Block Explorer
        blockExplorerScreen = findPreference("block_explorer");
        // Transaction change address type
        changeAddressType = findPreference("change_type");
        //localcurrency
        localCurrency = findPreference("local_currency");
        // Exchange Source
        exchangeSourceScreen = findPreference("exchange_source");
        language = findPreference(Constants.LANGUAGE_SETTING);
        notificationPreference = findPreference("notifications");
        // Socks Proxy
        useTor = findPreference(Constants.SETTING_TOR);
        showBip44Path = findPreference("showBip44Path");
        ledgerDisableTee = findPreference("ledgerDisableTee");
        ledgerSetUnpluggedAID = findPreference("ledgerUnpluggedAID");
        // external Services
        localTraderDisable = findPreference("ltDisable");
        externalPreference = findPreference("external_services");
        versionPreference = findPreference("updates");
        ltNotificationSound = findPreference("ltNotificationSound");
        ltMilesKilometers = findPreference("ltMilesKilometers");
        ledger = findPreference("ledger");
        ltNotificationEmail = findPreference("ltNotificationEmail2");
        localTraderPrefs = findPreference("localtraderPrefs");
        aboutPrefs = findPreference("about");
        helpPrefs = findPreference("help");

        // sub screens and sub prefs
        backupPreferenceScreen = findPreference("backup");
        if (backupPreferenceScreen != null) {
            backupPrefs.clear();
            createSubPreferences(backupPreferenceScreen, backupPrefs);
        }
        pinPreferenceScreen = findPreference("pincode");
        if (pinPreferenceScreen != null) {
            pinPrefs.clear();
            createSubPreferences(pinPreferenceScreen, pinPrefs);
        }
    }

    /**
     * Initializes sub preferences that are in preferences.xml
     *
     * @param preferenceScreen The PreferenceScreen that hosts sub-settings
     * @param subPrefs         Array list is needed to bind the preferences in bindSubPrefs()
     */
    private void createSubPreferences(PreferenceScreen preferenceScreen, List<Preference> subPrefs) {
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            if (preferenceScreen.getPreference(i) instanceof PreferenceCategory) {
                PreferenceCategory prefCat = (PreferenceCategory) preferenceScreen.getPreference(i);
                for (int j = 0; j < prefCat.getPreferenceCount(); j++) {
                    Preference pref = findPreference(prefCat.getPreference(j).getKey());
                    subPrefs.add(pref);
                }
            } else {
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
        if (language != null) {
            language.setTitle(getLanguageSettingTitle());
            language.setDefaultValue(Locale.getDefault().getLanguage());
            language.setSummary(mbwManager.getLanguage());
            language.setValue(mbwManager.getLanguage());

            ImmutableMap<String, String> languageLookup = loadLanguageLookups();
            language.setSummary(languageLookup.get(mbwManager.getLanguage()));

            language.setEntries(R.array.languages_desc);
            language.setEntryValues(R.array.languages);
            language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String lang = newValue.toString();
                    mbwManager.setLanguage(lang);
                    WalletApplication.applyLanguageChange(requireActivity().getBaseContext(), lang);

                    restart();

                    return true;
                }
            });
        }
        if (notificationPreference != null) {
            notificationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new NotificationsFragment())
                            .addToBackStack("notification")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }
        if (pinPreferenceScreen != null) {
            pinPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, PinCodeFragment.newInstance(pinPreferenceScreen.getKey()))
                            .addToBackStack("pincode")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
            pinPreferenceScreen.setSummary(mbwManager.isPinProtected() ? "On" : "Off");
        }
        if (useTor != null) {
            useTor.setTitle(getUseTorTitle());
            useTor.setChecked(mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR);
            useTor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (useTor.isChecked()) {
                        OrbotHelper obh = new OrbotHelper(getActivity());
                        if (!obh.isOrbotInstalled()) {
                            obh.promptToInstall(requireActivity());
                            useTor.setChecked(false);
                        }
                    }
                    mbwManager.setTorMode(useTor.isChecked()
                            ? ServerEndpointType.Types.ONLY_TOR : ServerEndpointType.Types.ONLY_HTTPS);
                    return true;
                }
            });
        }
        if (backupPreferenceScreen != null) {
            backupPreferenceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, BackupFragment.newInstance(backupPreferenceScreen.getKey()))
                            .addToBackStack("backup")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }
        if (denominationScreen != null) {
            denominationScreen.setOnPreferenceClickListener(preference -> {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, DenominationFragment.create(denominationScreen.getKey()))
                        .addToBackStack("bitcoin_denomination")
                        .commitAllowingStateLoss();
                return true;
            });
        }
        if (localCurrency != null) {
            localCurrency.setOnPreferenceClickListener(localCurrencyClickListener);
            localCurrency.setTitle(localCurrencyTitle());
        }

        if (exchangeSourceScreen != null) {
            exchangeSourceScreen.setOnPreferenceClickListener(preference -> {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, ExchangeSourcesFragment.create(exchangeSourceScreen.getKey()))
                        .addToBackStack("exchange_source")
                        .commitAllowingStateLoss();
                return true;
            });
        }
        if (blockExplorerScreen != null) {
            blockExplorerScreen.setOnPreferenceClickListener(preference -> {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, BlockExplorersFragment.create(blockExplorerScreen.getKey()))
                        .addToBackStack("block_explorer")
                        .commitAllowingStateLoss();
                return true;
            });
        }

        if (changeAddressType != null) {
            changeAddressType.setOnPreferenceClickListener(segwitChangeAddressClickListener);
        }
        if (minerFeeScreen != null) {
            minerFeeScreen.setOnPreferenceClickListener(preference -> {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                R.anim.slide_left_in, R.anim.slide_right_out)
                        .replace(R.id.fragment_container, MinerFeeFragment.create(minerFeeScreen.getKey()))
                        .addToBackStack("miner_fee")
                        .commitAllowingStateLoss();
                return true;
            });
        }

        // Local Trader
        if (localTraderDisable != null) {
            localTraderDisable.setChecked(ltManager.isLocalTraderEnabled());
            localTraderDisable.setOnPreferenceClickListener(ltDisableLocalTraderClickListener);
        }

        if (ltNotificationSound != null) {
            ltNotificationSound.setChecked(ltManager.getPlaySoundOnTradeNotification());
            ltNotificationSound.setOnPreferenceClickListener(ltNotificationSoundClickListener);
        }


        if (ltMilesKilometers != null) {
            ltMilesKilometers.setChecked(ltManager.useMiles());
            ltMilesKilometers.setOnPreferenceClickListener(ltMilesKilometersClickListener);
        }
        // show bip44 path
        if (showBip44Path != null) {
            showBip44Path.setChecked(mbwManager.getMetadataStorage().getShowBip44Path());
            showBip44Path.setOnPreferenceClickListener(showBip44PathClickListener);
        }
        if (ledgerDisableTee != null) {
            boolean isTeeAvailable = LedgerTransportTEEProxyFactory.isServiceAvailable(getActivity());
            if (isTeeAvailable) {
                ledgerDisableTee.setChecked(mbwManager.getLedgerManager().getDisableTEE());
                ledgerDisableTee.setOnPreferenceClickListener(onClickLedgerNotificationDisableTee);
            } else {
                ledger.removePreference(ledgerDisableTee);
            }
        }
        if (ledgerSetUnpluggedAID != null) {
            ledgerSetUnpluggedAID.setOnPreferenceClickListener(onClickLedgerSetUnpluggedAID);
        }
        applyLocalTraderEnablement();

        if (externalPreference != null) {
            externalPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, new ExternalServiceFragment())
                            .addToBackStack("external_services")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }
        if (versionPreference != null) {
            versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, new VersionFragment())
                            .addToBackStack("version")
                            .commitAllowingStateLoss();
                    return true;
                }
            });
        }
        if (aboutPrefs != null) {
            aboutPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(requireContext(), AboutActivity.class));
                    return false;
                }
            });
        }
        if (helpPrefs != null) {
            helpPrefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    requireFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_right_in, R.anim.slide_left_out,
                                    R.anim.slide_left_in, R.anim.slide_right_out)
                            .replace(R.id.fragment_container, new HelpFragment())
                            .addToBackStack("help")
                            .commitAllowingStateLoss();
                    return false;
                }
            });
        }

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

                    requireFragmentManager()
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

                    requireFragmentManager()
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

        isSearchViewOpen = false;
        ((SettingsActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                isSearchViewOpen = false;
                refreshPreferences();
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSearchViewOpen = true;
                ((SettingsActivity) requireActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
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
                } else {
                    findSearchResult(s);
                }
                return true;
            }

            private boolean isValid(Preference preference, String text) {
                return (isTitleValid(preference, text) || isSummaryValid(preference, text)) && isIndependent(preference);
            }

            private void findSearchResult(String s) {
                refreshPreferences();

                // adding hidden prefs
                List<PreferenceScreen> hiddenPreferencesScreenList = Arrays.asList(backupPreferenceScreen, pinPreferenceScreen);
                for (PreferenceScreen preferenceScreen : hiddenPreferencesScreenList) {
                    for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                        Preference preference = preferenceScreen.getPreference(i);
                        if (preference.getParent() != null) {
                            preference.getParent().removePreference(preference);
                        }
                        preference.setOrder(getPreferenceScreen().getPreferenceCount());
                        getPreferenceScreen().addPreference(preference);
                    }
                }


                for (int j = getPreferenceScreen().getPreferenceCount() - 1; j >= 0; j--) {
                    Preference preference = getPreferenceScreen().getPreference(j);
                    preference.setOrder(j);
                    if (preference instanceof PreferenceCategory) {
                        PreferenceCategory preferenceCategory = (PreferenceCategory) preference;
                        for (int i = preferenceCategory.getPreferenceCount() - 1; i >= 0; i--) {
                            Preference preferenceInner = preferenceCategory.getPreference(i);

                            if (!isValid(preferenceInner, s) && !checkInDeep(preferenceInner, s)) {
                                preferenceCategory.removePreference(preferenceInner);
                            }
                        }
                        if (preferenceCategory.getPreferenceCount() == 0) {
                            getPreferenceScreen().removePreference(preferenceCategory);
                        }
                    } else {
                        if (!isValid(preference, s) && !checkInDeep(preference, s)) {
                            getPreferenceScreen().removePreference(preference);
                        }
                    }
                }
            }

            private boolean checkInDeep(Preference preference, String text) {
                if (preference instanceof PreferenceGroup) {
                    PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                    for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                        Preference pref = preferenceGroup.getPreference(i);
                        if (isValid(pref, text) || checkInDeep(pref, text)) {
                            return true;
                        }
                    }
                }
                return false;
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
        ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setTitle(R.string.settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (searchView.isIconified()) {
                requireActivity().finish();
            } else {
                searchView.setIconified(true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    private ButtonPreference createUninstallableModulePreference(final Module module) {
        final ButtonPreference preference = new ButtonPreference(getActivity());
        preference.setWidgetLayoutResource(R.layout.preference_button_uninstall);
        preference.setTitle(Html.fromHtml(module.getName()));
        preference.setKey("Module_" + module.getModulePackage());
        updateModulePreference(preference, module);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(com.mycelium.modularizationtools.Constants.getSETTINGS());
                intent.setPackage(module.getModulePackage());
                intent.putExtra("callingPackage", requireActivity().getPackageName());
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
            if (resultCode == RESULT_CANCELED) {
                pleaseWait.dismiss();
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

    @Override
    public void onResume() {
        setupLocalTraderSettings();
        localCurrency.setTitle(localCurrencyTitle());
        localCurrency.setSummary(localCurrencySummary());
        MbwManager.getEventBus().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        MbwManager.getEventBus().unregister(this);
        refreshPreferences();
        if (pleaseWait != null) {
            pleaseWait.dismiss();
        }
        super.onPause();
    }

    private void refreshPreferences() {
        ((SettingsActivity) requireActivity()).getSupportActionBar()
                .setDisplayShowTitleEnabled(!isSearchViewOpen);
        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);
        assignPreferences();
        onBindPreferences();
        setupLocalTraderSettings();
    }

    private void setupLocalTraderSettings() {
        if (!ltManager.hasLocalTraderAccount()) {
            if (localTraderPrefs != null) {
                localTraderPrefs.removeAll();
                //its important we keep this prefs, so users can still enable / disable lt without having an account
                localTraderPrefs.addPreference(localTraderDisable);
            }
            return;
        }
        setupEmailNotificationSetting();
    }

    private void setupEmailNotificationSetting() {
        ltNotificationEmail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                LocalTraderEventSubscriber listener = new SubscribeToServerResponse();
                ltManager.subscribe(listener);
                new Thread() {
                    @Override
                    public void run() {
                        ltManager.makeRequest(new GetTraderInfo());
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
        String english = Utils.getResourcesByLocale(requireActivity(), "en")
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
        Intent running = requireActivity().getIntent();
        requireActivity().finish();
        startActivity(running);
    }

    private void applyLocalTraderEnablement() {
        boolean ltEnabled = ltManager.isLocalTraderEnabled();
        if (ltNotificationSound != null) {
            ltNotificationSound.setEnabled(ltEnabled);
        }
        if (ltMilesKilometers != null) {
            ltMilesKilometers.setEnabled(ltEnabled);
        }
    }

    private String getUseTorTitle() {
        return getResources().getString(R.string.useTor);
    }

    private String localCurrencyTitle() {
        return getResources().getString(R.string.pref_local_currency);
    }

    private String localCurrencySummary() {
        if (mbwManager.hasFiatCurrency()) {
            AssetInfo currentCurrency = mbwManager.getCurrencySwitcher().getCurrentTotalCurrency();
            String currencies = currentCurrency.getSymbol();
            List<AssetInfo> currencyList = mbwManager.getCurrencyList();
            currencyList.remove(currentCurrency);
            for (int i = 0; i < Math.min(currencyList.size(), 2); i++) {
                //noinspection StringConcatenationInLoop
                currencies += ", " + currencyList.get(i).getSymbol();
            }
            if (mbwManager.getCurrencyList().size() > 3) {
                //multiple selected, add ...
                currencies += "...";
            }
            return currencies;
        } else {
            //nothing selected
            return getResources().getString(R.string.pref_no_fiat_selected);
        }
    }

    private class SubscribeToServerResponse extends LocalTraderEventSubscriber {
        private Button okButton;
        private EditText emailEdit;

        SubscribeToServerResponse() {
            super(new Handler());
        }

        @Override
        public void onLtTraderInfoFetched(final TraderInfo info, GetTraderInfo request) {
            pleaseWait.dismiss();
            AlertDialog.Builder b = new AlertDialog.Builder(requireActivity(), R.style.MyceliumSettings_Dialog);
            b.setTitle(getString(R.string.lt_set_email_title));
            b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String email = emailEdit.getText().toString();
                    ltManager.makeRequest(new SetNotificationMail(email));

                    if ((info.notificationEmail == null || !info.notificationEmail.equals(email)) && !Strings.isNullOrEmpty(email)) {
                        Utils.showSimpleMessageDialog(getActivity(), getString(R.string.lt_email_please_verify_message));
                    }
                }
            });
            b.setNegativeButton(R.string.cancel, null);

            emailEdit = new AppCompatEditText(requireActivity()) {
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
            ltManager.unsubscribe(this);
        }

        @Override
        public void onLtError(int errorCode) {
            pleaseWait.dismiss();
            new Toaster(requireActivity()).toast(getString(R.string.lt_set_email_error), false);
            ltManager.unsubscribe(this);
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
        private Paint paint;

        DividerDecoration() {
            paint = new Paint();
            paint.setColor(Color.parseColor("#2c2c2c"));
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

        void setDivider(Drawable divider) {
            if (divider != null) {
                mDividerHeight = divider.getIntrinsicHeight();
            } else {
                mDividerHeight = 0;
            }
            mDivider = divider;
            getListView().invalidateItemDecorations();
        }

        void setAllowDividerAfterLastItem(boolean allowDividerAfterLastItem) {
            mAllowDividerAfterLastItem = allowDividerAfterLastItem;
        }
    }
}
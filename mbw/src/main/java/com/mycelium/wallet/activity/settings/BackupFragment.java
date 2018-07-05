package com.mycelium.wallet.activity.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.List;

public class BackupFragment extends PreferenceFragmentCompat {

    private static final String ARG_PREFS_ROOT = "preference_root_key";
    private String mRootKey;

    private static final String ARG_FRAGMENT_OPEN_TYPE = "fragment_open_type";
    private int mOpenType;
    public static final int OPEN_NONE = 0;
    public static final int OPEN_LEGACY_BACKUP = 1;
    public static final int OPEN_LEGACY_BACKUP_VERIFY = 2;

    private Preference legacyBackup;
    private Preference legacyBackupVerify;

    private MbwManager _mbwManager;


    public static BackupFragment newInstance(int code, String pageId){
        BackupFragment fragment = new BackupFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FRAGMENT_OPEN_TYPE, code);
        args.putString(ARG_PREFS_ROOT, pageId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (getArguments() != null) {
            mOpenType = getArguments().getInt(ARG_FRAGMENT_OPEN_TYPE);
            mRootKey = getArguments().getString(ARG_PREFS_ROOT);
        }

        setPreferencesFromResource(R.xml.preferences, mRootKey);

        _mbwManager = MbwManager.getInstance(getActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.backup_lower);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Legacy backup function
        legacyBackup = Preconditions.checkNotNull(findPreference("legacyBackup"));
        legacyBackup.setOnPreferenceClickListener(legacyBackupClickListener);

        // Legacy backup function
        legacyBackupVerify = Preconditions.checkNotNull(findPreference("legacyBackupVerify"));
        legacyBackupVerify.setOnPreferenceClickListener(legacyBackupVerifyClickListener);

        simulateClick(mOpenType);
    }

    @SuppressLint("RestrictedApi")
    public void simulateClick(int openType){
        switch (openType){
            case OPEN_NONE:
                break;
            case OPEN_LEGACY_BACKUP:
                legacyBackup.performClick();
                break;
            case OPEN_LEGACY_BACKUP_VERIFY:
                legacyBackupVerify.performClick();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        showOrHideLegacyBackup();
        super.onResume();
    }

    private final Preference.OnPreferenceClickListener legacyBackupClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            Utils.pinProtectedBackup(getActivity());
            return true;
        }
    };
    private final Preference.OnPreferenceClickListener legacyBackupVerifyClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            VerifyBackupActivity.callMe(getActivity());
            return true;
        }
    };

    @SuppressWarnings("deprecation")
    private void showOrHideLegacyBackup() {
        List<WalletAccount> accounts = _mbwManager.getWalletManager(false).getSpendingAccounts();
        Preference legacyPref = findPreference("legacyBackup");
        if (legacyPref == null) {
            return; // it was already removed, don't remove it again.
        }

        for (WalletAccount account : accounts) {
            if (account instanceof SingleAddressAccount) {
                return; //we have a single address account with priv key, so its fine to show the setting
            }
        }
        //no matching account, hide setting
        getPreferenceScreen().removePreference(legacyPref);
    }


}

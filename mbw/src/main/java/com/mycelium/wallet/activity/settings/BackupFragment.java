package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.List;

public class BackupFragment extends PreferenceFragment {

    private MbwManager _mbwManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_backup);
        _mbwManager = MbwManager.getInstance(getActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.backup);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Legacy backup function
        Preference legacyBackup = Preconditions.checkNotNull(findPreference("legacyBackup"));
        legacyBackup.setOnPreferenceClickListener(legacyBackupClickListener);

        // Legacy backup function
        Preference legacyBackupVerify = Preconditions.checkNotNull(findPreference("legacyBackupVerify"));
        legacyBackupVerify.setOnPreferenceClickListener(legacyBackupVerifyClickListener);

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

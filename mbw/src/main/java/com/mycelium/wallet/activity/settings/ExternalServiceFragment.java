package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.external.BuySellServiceDescriptor;

import java.util.List;

public class ExternalServiceFragment extends PreferenceFragmentCompat {
    private MbwManager _mbwManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_external_service);
        _mbwManager = MbwManager.getInstance(getActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.external_service);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("container");
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
            cbService.setLayoutResource(R.layout.preference_layout);
            cbService.setSummary(buySellService.settingDescription);
            cbService.setChecked(buySellService.isEnabled(_mbwManager));
            cbService.setWidgetLayoutResource(R.layout.preference_switch);
            cbService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    buySellService.setEnabled(_mbwManager, p.isChecked());
                    return true;
                }
            });
            preferenceCategory.addPreference(cbService);
        }

        if (!SettingsPreference.getInstance().isEndedMyDFS()) {
            final CheckBoxPreference cbService = new CheckBoxPreference(getActivity());
            cbService.setTitle(R.string.settings_mydfs_title);
            cbService.setSummary(R.string.settings_mydfs_summary);
            cbService.setChecked(SettingsPreference.getInstance().isMyDFSEnabled());
            cbService.setLayoutResource(R.layout.preference_layout);
            cbService.setWidgetLayoutResource(R.layout.preference_switch);
            cbService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    SettingsPreference.getInstance().setEnableMyDFS(p.isChecked());
                    return true;
                }
            });
            preferenceCategory.addPreference(cbService);
        }

        if (!SettingsPreference.getInstance().isEndedApex()) {
            final CheckBoxPreference cbServiceApex = new CheckBoxPreference(getActivity());
            cbServiceApex.setTitle(R.string.settings_apex_title);
            cbServiceApex.setSummary(R.string.settings_apex_summary);
            cbServiceApex.setChecked(SettingsPreference.getInstance().isApexEnabled());
            cbServiceApex.setLayoutResource(R.layout.preference_layout);
            cbServiceApex.setWidgetLayoutResource(R.layout.preference_switch);
            cbServiceApex.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    SettingsPreference.getInstance().setEnableApex(p.isChecked());
                    return true;
                }
            });
            preferenceCategory.addPreference(cbServiceApex);
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
}

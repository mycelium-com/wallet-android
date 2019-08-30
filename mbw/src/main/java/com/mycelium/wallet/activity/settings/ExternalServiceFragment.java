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
    private MbwManager mbwManager;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_external_service);
        mbwManager = MbwManager.getInstance(requireActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.external_service);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("container");
        final List<BuySellServiceDescriptor> buySellServices = mbwManager.getEnvironmentSettings().getBuySellServices();

        for (final BuySellServiceDescriptor buySellService : buySellServices) {
            if (!buySellService.showEnableInSettings()) {
                continue;
            }

            final CheckBoxPreference cbService = new CheckBoxPreference(requireActivity());
            final String enableTitle = getResources().getString(R.string.settings_service_enabled,
                    getResources().getString(buySellService.title)
            );
            cbService.setTitle(enableTitle);
            cbService.setLayoutResource(R.layout.preference_layout);
            cbService.setSummary(buySellService.settingDescription);
            cbService.setChecked(buySellService.isEnabled(mbwManager));
            cbService.setWidgetLayoutResource(R.layout.preference_switch);
            cbService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    buySellService.setEnabled(mbwManager, p.isChecked());
                    return true;
                }
            });
            preferenceCategory.addPreference(cbService);
        }

        if (SettingsPreference.INSTANCE.getFioActive()) {
            final CheckBoxPreference cbServiceFio = new CheckBoxPreference(requireActivity());
            cbServiceFio.setTitle(R.string.settings_fio_title);
            cbServiceFio.setSummary(R.string.settings_fio_summary);
            cbServiceFio.setChecked(SettingsPreference.INSTANCE.getFioEnabled());
            cbServiceFio.setLayoutResource(R.layout.preference_layout);
            cbServiceFio.setWidgetLayoutResource(R.layout.preference_switch);
            cbServiceFio.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    CheckBoxPreference p = (CheckBoxPreference) preference;
                    SettingsPreference.INSTANCE.setFioEnabled(p.isChecked());
                    return true;
                }
            });
            preferenceCategory.addPreference(cbServiceFio);
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

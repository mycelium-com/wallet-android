package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
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

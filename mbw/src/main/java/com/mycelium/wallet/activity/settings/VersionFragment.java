package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.MenuItem;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.R;

public class VersionFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_versions);
        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.updates);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("modules");
        for (Module module : CommunicationManager.getInstance().getPairedModules()) {
            Preference preference = new Preference(getActivity());
            preference.setTitle(Html.fromHtml(module.getName()));
            preference.setSummary(getString(R.string.version) + " " + module.getVersion());
            preferenceCategory.addPreference(preference);
        }
        Preference appPreference = findPreference("application");
        appPreference.setSummary(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
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

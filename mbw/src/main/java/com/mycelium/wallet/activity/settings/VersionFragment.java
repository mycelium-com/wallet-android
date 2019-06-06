package com.mycelium.wallet.activity.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.view.ButtonPreference;
import com.mycelium.wallet.modularisation.ModularisationVersionHelper;

public class VersionFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_versions);
        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.updates);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Preference appPreference = findPreference("application");
        appPreference.setSummary(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
        appPreference.setTitle(R.string.app_name);
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

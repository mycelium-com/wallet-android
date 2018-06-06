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
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("modules");
        for (final Module module : CommunicationManager.getInstance().getPairedModules()) {
            ButtonPreference preference = new ButtonPreference(getActivity());
            preference.setTitle(Html.fromHtml(module.getName()));
            preference.setSummary(getString(R.string.version) + " " + module.getVersion());
            preference.setLayoutResource(R.layout.preference_layout_no_icon);
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
            if (ModularisationVersionHelper.isUpdateRequired(getActivity(), module.getModulePackage())) {
                preference.setButtonText(getString(R.string.update));
                preference.setButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                        installIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" +
                                module.getModulePackage()));
                        startActivity(installIntent);
                    }
                });
            }
            preferenceCategory.addPreference(preference);
        }
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

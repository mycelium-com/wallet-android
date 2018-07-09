package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.mycelium.wallet.R;

public class NotificationsFragment extends PreferenceFragmentCompat {
    private CheckBoxPreference newsAllPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_notifications);

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.notifications);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        newsAllPreference = (CheckBoxPreference) findPreference("news_all_notification");
    }

    @Override
    protected void onBindPreferences() {
        newsAllPreference.setChecked(SettingsPreference.getInstance().isNewsNotificationEnabled());
        newsAllPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                CheckBoxPreference p = (CheckBoxPreference) preference;
                SettingsPreference.getInstance().setNewsNotificationEnabled(p.isChecked());
                return true;
            }
        });
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

package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

public class PinCodeFragment extends PreferenceFragmentCompat {
    private MbwManager _mbwManager;

    private CheckBoxPreference setPin;
    private CheckBoxPreference setPinRequiredStartup;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_pincode);
        _mbwManager = MbwManager.getInstance(getActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.pin_code);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set PIN
        setPin = (CheckBoxPreference) Preconditions.checkNotNull(findPreference("setPin"));
        setPin.setOnPreferenceClickListener(setPinClickListener);

        setPinRequiredStartup = (CheckBoxPreference) Preconditions.checkNotNull(findPreference("requirePinOnStartup"));
        setPinRequiredStartup.setOnPreferenceChangeListener(setPinOnStartupClickListener);
        update();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final Preference.OnPreferenceClickListener setPinClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            Optional<Runnable> afterDialogClosed = Optional.<Runnable>of(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            });
            if(setPin.isChecked()) {
                _mbwManager.showSetPinDialog(getActivity(), afterDialogClosed);
            } else {
                _mbwManager.showClearPinDialog(getActivity(), afterDialogClosed);
            }
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener setPinOnStartupClickListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object o) {
            _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
                        @Override
                        public void run() {
                            // toggle it here
                            boolean checked = !((CheckBoxPreference) preference).isChecked();
                            _mbwManager.setPinRequiredOnStartup(checked);
                            update();
                        }
                    }
            );

            // don't automatically take the new value, lets do it in the pin protected runnable
            return false;
        }
    };

    void update() {
        setPin.setChecked(_mbwManager.isPinProtected());
        setPinRequiredStartup.setChecked(_mbwManager.isPinProtected() && _mbwManager.getPinRequiredOnStartup());
    }
}

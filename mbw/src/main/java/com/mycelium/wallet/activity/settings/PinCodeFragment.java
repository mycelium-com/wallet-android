package com.mycelium.wallet.activity.settings;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

public class PinCodeFragment extends PreferenceFragment {
    private MbwManager _mbwManager;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_pincode);
        _mbwManager = MbwManager.getInstance(getActivity().getApplication());

        setHasOptionsMenu(true);
        ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.pin_code);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set PIN
        Preference setPin = Preconditions.checkNotNull(findPreference("setPin"));
        setPin.setOnPreferenceClickListener(setPinClickListener);

        // Clear PIN
        updateClearPin();

        // PIN required on startup
        updatePinAtStartup();

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
            _mbwManager.showSetPinDialog(getActivity(), Optional.<Runnable>of(new Runnable() {
                        @Override
                        public void run() {
                            updateClearPin();
                            updatePinAtStartup();
                        }
                    })
            );
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
                            ((CheckBoxPreference) preference).setChecked(_mbwManager.getPinRequiredOnStartup());
                        }
                    }
            );

            // dont automatically take the new value, lets to it in our the pin protected runnable
            return false;
        }
    };
    private final Preference.OnPreferenceClickListener clearPinClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            _mbwManager.showClearPinDialog(getActivity(), Optional.<Runnable>of(new Runnable() {
                @Override
                public void run() {
                    updateClearPin();
                    updatePinAtStartup();
                }
            }));
            return true;
        }
    };

    @SuppressWarnings("deprecation")
    private void updateClearPin() {
        Preference clearPin = findPreference("clearPin");
        clearPin.setEnabled(_mbwManager.isPinProtected());
        clearPin.setOnPreferenceClickListener(clearPinClickListener);
    }

    private void updatePinAtStartup() {
        CheckBoxPreference setPinRequiredStartup = (CheckBoxPreference) Preconditions.checkNotNull(findPreference("requirePinOnStartup"));
        setPinRequiredStartup.setOnPreferenceChangeListener(setPinOnStartupClickListener);
        setPinRequiredStartup.setEnabled(_mbwManager.isPinProtected());
        setPinRequiredStartup.setChecked(_mbwManager.isPinProtected() && _mbwManager.getPinRequiredOnStartup());
    }

}

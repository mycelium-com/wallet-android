package com.mycelium.wallet.activity.settings;

import android.annotation.SuppressLint;
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

    private static final String ARG_PREFS_ROOT = "preference_root_key";
    public static final String ARG_FRAGMENT_OPEN_TYPE = "fragment_open_type";
    private String mRootKey;
    private int mOpenType;

    private MbwManager _mbwManager;

    // preferences
    private CheckBoxPreference setPin;
    private CheckBoxPreference setPinRequiredStartup;
    private CheckBoxPreference randomizePin;

    public static PinCodeFragment newInstance(String pageId) {
        PinCodeFragment fragment = new PinCodeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PREFS_ROOT, pageId);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (getArguments() != null) {
            mOpenType = getArguments().getInt(ARG_FRAGMENT_OPEN_TYPE, -1);
            mRootKey = getArguments().getString(ARG_PREFS_ROOT);
        }

        setPreferencesFromResource(R.xml.preferences, mRootKey);

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

        randomizePin = (CheckBoxPreference) Preconditions.checkNotNull(findPreference("pinPadIsRandomized"));
        randomizePin.setOnPreferenceChangeListener(randomizePinListener);
        update();

        simulateClick(mOpenType);
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

            // This is an ugly hack to not to develop error handling for PinCode class.
            // Correct value would be automatically set on success and should not change on error.
            setPin.setChecked(!setPin.isChecked());
            if(!setPin.isChecked()) {
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

    private final Preference.OnPreferenceChangeListener randomizePinListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object o) {
            _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
                @Override
                public void run() {
                    boolean checked = !((CheckBoxPreference) preference).isChecked();
                    if(_mbwManager.isPinProtected()) {
                        _mbwManager.setPinPadRandomized(checked);
                    } else {
                        _mbwManager.setPinPadRandomized(false);
                    }
                    update();
                }
            });

            // don't automatically take the new value, lets do it in the pin protected runnable
            return false;
        }
    };

    void update() {
        setPin.setChecked(_mbwManager.isPinProtected());
        setPinRequiredStartup.setChecked(_mbwManager.isPinProtected() && _mbwManager.getPinRequiredOnStartup());
        randomizePin.setChecked(_mbwManager.isPinProtected() && _mbwManager.isPinPadRandomized());
    }

    @SuppressLint("RestrictedApi")
    public void simulateClick(int openType) {
        switch (openType){
            case 0:
                setPin.performClick();
                break;
            case 1:
                setPinRequiredStartup.performClick();
                break;
            case 2:
                randomizePin.performClick();
                break;
        }
    }
}

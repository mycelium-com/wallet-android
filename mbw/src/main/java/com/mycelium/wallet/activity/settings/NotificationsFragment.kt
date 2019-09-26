package com.mycelium.wallet.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.wallet.R

class NotificationsFragment : PreferenceFragmentCompat() {
    private var newsAllPreference: CheckBoxPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        addPreferencesFromResource(R.xml.preferences_notifications)

        setHasOptionsMenu(true)
        (activity as SettingsActivity).supportActionBar?.run {
            setTitle(R.string.notifications)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        newsAllPreference = findPreference("news_all_notification")
    }

    override fun onBindPreferences() {
        newsAllPreference?.isChecked = SettingsPreference.mediaFLowNotificationEnabled
        newsAllPreference?.setOnPreferenceClickListener { preference ->
            val p = preference as CheckBoxPreference
            SettingsPreference.mediaFLowNotificationEnabled = p.isChecked
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            fragmentManager?.popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

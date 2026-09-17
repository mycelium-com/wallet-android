package com.mycelium.wallet.activity.settings

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.wallet.R

class NotificationsFragment : PreferenceFragmentCompat() {
    private var newsAllPreference: CheckBoxPreference? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                SettingsPreference.mediaFLowNotificationEnabled = true
            }
            updateNewsAllPreference()
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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
        updateNewsAllPreference()
        newsAllPreference?.setOnPreferenceClickListener { preference ->
            val p = preference as CheckBoxPreference
            if (areSystemNotificationsEnabled()) {
                SettingsPreference.mediaFLowNotificationEnabled = p.isChecked
            } else {
                // notifications are blocked by the system: the app setting alone would have no effect
                p.isChecked = false
                requestNotificationsPermission()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // the permission may have been changed in the system settings meanwhile
        updateNewsAllPreference()
    }

    private fun areSystemNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()

    private fun updateNewsAllPreference() {
        val systemEnabled = areSystemNotificationsEnabled()
        newsAllPreference?.isChecked = systemEnabled &&
                SettingsPreference.mediaFLowNotificationEnabled && SettingsPreference.mediaFlowEnabled
        newsAllPreference?.summary =
            if (systemEnabled) null else getString(R.string.settings_notifications_blocked)
        newsAllPreference?.isEnabled = SettingsPreference.mediaFlowEnabled
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
            notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
        } else {
            // permanently denied (the system dialog is not shown anymore) or blocked in the app
            // notification settings: let the user allow notifications there
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        val context = requireContext()
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)))
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

package com.mycelium.wallet.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.BequantPreference
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.SettingsPreference.fioActive
import com.mycelium.wallet.activity.settings.SettingsPreference.fioEnabled
import com.mycelium.wallet.activity.settings.SettingsPreference.mediaFlowEnabled
import com.mycelium.wallet.external.changelly.ChangellyConstants

class ExternalServiceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_external_service)
        val mbwManager = MbwManager.getInstance(requireActivity())
        setHasOptionsMenu(true)
        (requireActivity() as SettingsActivity).supportActionBar!!.apply {
            setTitle(R.string.external_service)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        val preferenceCategory = findPreference<PreferenceCategory>("container")
        val buySellServices = mbwManager.environmentSettings.buySellServices
        for (buySellService in buySellServices) {
            if (!buySellService.showEnableInSettings()) {
                continue
            }
            preferenceCategory?.addPreference(CheckBoxPreference(requireActivity()).apply {
                title = resources.getString(R.string.settings_service_enabled,
                        resources.getString(buySellService.title))
                layoutResource = R.layout.preference_layout
                setSummary(buySellService.settingDescription)
                isChecked = buySellService.isEnabled(mbwManager)
                widgetLayoutResource = R.layout.preference_switch
                onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                    val p = preference as CheckBoxPreference
                    buySellService.setEnabled(mbwManager, p.isChecked)
                    true
                }
            })
        }
        SettingsPreference.getPartnerInfos().filter { it.name != null }.forEach { partnerInfo ->
            preferenceCategory?.addPreference(CheckBoxPreference(requireActivity()).apply {
                title = resources.getString(R.string.settings_service_enabled, partnerInfo.name)
                layoutResource = R.layout.preference_layout
                isChecked = SettingsPreference.isEnabled(partnerInfo.id ?: "")
                widgetLayoutResource = R.layout.preference_switch
                onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                    val p = preference as CheckBoxPreference
                    SettingsPreference.setEnabled(partnerInfo.id ?: "", p.isChecked)
                    true
                }
                if(partnerInfo.id == BequantConstants.PARTNER_ID && BequantPreference.isLogged()) {
                    isEnabled = false
                    summary = getString(R.string.cant_disable_active_account)
                }
            })
            if(partnerInfo.id == ChangellyConstants.PARTNER_ID_CHANGELLY) {
                preferenceCategory?.addPreference(CheckBoxPreference(requireActivity()).apply {
                    title = getString(R.string.settings_exchange_quick_exchange_title)
                    summary = getString(R.string.settings_exchange_quick_exchange_summary)
                    layoutResource = R.layout.preference_layout
                    isChecked = SettingsPreference.exchangeConfirmationEnabled
                    widgetLayoutResource = R.layout.preference_switch
                    onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                        val p = preference as CheckBoxPreference
                        SettingsPreference.exchangeConfirmationEnabled = p.isChecked
                        true
                    }
                })
            }
        }
        if (fioActive) {
            preferenceCategory?.addPreference(CheckBoxPreference(requireActivity()).apply {
                setTitle(R.string.settings_fiopresale_title)
                setSummary(R.string.settings_fiopresale_summary)
                isChecked = fioEnabled
                layoutResource = R.layout.preference_layout
                widgetLayoutResource = R.layout.preference_switch
                onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                    val p = preference as CheckBoxPreference
                    fioEnabled = p.isChecked
                    true
                }
            })
        }
        findPreference<CheckBoxPreference>("media_flow")?.apply {
            isChecked = mediaFlowEnabled
            onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                val p = preference as CheckBoxPreference
                mediaFlowEnabled = p.isChecked
                true
            }
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
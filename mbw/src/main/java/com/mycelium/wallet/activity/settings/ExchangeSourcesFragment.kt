package com.mycelium.wallet.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler
import com.mycelium.wallet.Utils


class ExchangeSourcesFragment : PreferenceFragmentCompat() {

    private var displayPreferenceDialogHandler: DisplayPreferenceDialogHandler? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, arguments?.getString(ARG_PREFS_ROOT))

        val mbwManager = MbwManager.getInstance(activity!!)
        displayPreferenceDialogHandler = DisplayPreferenceDialogHandler(preferenceScreen.context)
        setHasOptionsMenu(true)
        (activity as SettingsActivity).supportActionBar!!.apply {
            setTitle(R.string.pref_exchange_source)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        val prefCat = PreferenceCategory(preferenceScreen.context)
        preferenceScreen.addPreference(prefCat)
        val cryptocurrencies = mbwManager.cryptocurrenciesSorted
        for (name in cryptocurrencies) {
            val listPreference = ListPreference(preferenceScreen.context).apply {
                title = name
                val symbol = Utils.getTypeByName(name)!!.symbol
                val exchangeManager = mbwManager.exchangeRateManager
                val exchangeSourceNamesList = exchangeManager.getExchangeSourceNames(symbol)
                val exchangeNames = exchangeSourceNamesList.toTypedArray()
                if (exchangeNames.isEmpty()) {
                    isEnabled = false
                } else {
                    var currentName: String? = exchangeManager.getCurrentExchangeSourceName(symbol)
                    if (currentName == null) {
                        currentName = ""
                    }
                    entries = exchangeNames
                    entryValues = exchangeNames
                    setDefaultValue(currentName)
                    value = currentName
                }
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    mbwManager.exchangeRateManager.setCurrentExchangeSourceName(symbol, newValue.toString())
                    true
                }
                layoutResource = R.layout.preference_layout_no_icon
                widgetLayoutResource = R.layout.preference_arrow
                dialogTitle = "$name exchange source"
            }
            prefCat.addPreference(listPreference)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            fragmentManager!!.popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        displayPreferenceDialogHandler!!.onDisplayPreferenceDialog(preference)
    }

    companion object {
        private const val ARG_PREFS_ROOT = "preference_root_key"

        @JvmStatic
        fun create(pageId: String) = ExchangeSourcesFragment().apply {
            arguments = Bundle().apply { putString(ARG_PREFS_ROOT, pageId) }
        }
    }
}
package com.mycelium.wallet.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler
import java.util.*

class DenominationFragment : PreferenceFragmentCompat() {

    private var displayPreferenceDialogHandler: DisplayPreferenceDialogHandler? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, arguments?.getString(ARG_PREFS_ROOT))

        val mbwManager: MbwManager = MbwManager.getInstance(activity!!)
        displayPreferenceDialogHandler = DisplayPreferenceDialogHandler(preferenceScreen.context)
        setHasOptionsMenu(true)
        (activity as SettingsActivity).supportActionBar!!.apply {
            setTitle(R.string.pref_bitcoin_denomination)
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
                val denominationMap = LinkedHashMap<String, Denomination>()
                var defaultValue = ""
                val coinType = Utils.getTypeByName(name)!!
                var symbol = coinType.symbol
                symbol = if (symbol.startsWith("t")) symbol.substring(1) else symbol
                for (value in Denomination.values()) {
                    if (value.supportedBy(symbol)) {
                        val key = value.toString().toLowerCase(Locale.ROOT) + "(" + value.getUnicodeString(symbol) + ")"
                        denominationMap[key] = value
                        if (value === mbwManager.getDenomination(coinType)) {
                            defaultValue = key
                        }
                    }
                }
                setDefaultValue(defaultValue)
                value = defaultValue
                entries = denominationMap.keys.toTypedArray()
                entryValues = denominationMap.keys.toTypedArray()
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    mbwManager.setDenomination(coinType, denominationMap[newValue.toString()])
                    true
                }
                layoutResource = R.layout.preference_layout_no_icon
                widgetLayoutResource = R.layout.preference_arrow
                dialogTitle = "$name denomination"
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
        fun create(pageId: String) = DenominationFragment().apply {
            arguments = Bundle().apply { putString(ARG_PREFS_ROOT, pageId) }
        }
    }
}
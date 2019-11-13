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
    private var mRootKey: String? = null
    private var mOpenType: Int = 0

    private var _mbwManager: MbwManager? = null
    private var displayPreferenceDialogHandler: DisplayPreferenceDialogHandler? = null


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (arguments != null) {
            mOpenType = arguments!!.getInt(ARG_FRAGMENT_OPEN_TYPE, -1)
            mRootKey = arguments!!.getString(ARG_PREFS_ROOT)
        }

        setPreferencesFromResource(R.xml.preferences, mRootKey)

        _mbwManager = MbwManager.getInstance(activity!!.application)
        displayPreferenceDialogHandler = DisplayPreferenceDialogHandler(preferenceScreen.context)
        setHasOptionsMenu(true)
        val actionBar = (activity as SettingsActivity).supportActionBar
        actionBar!!.setTitle(R.string.pref_bitcoin_denomination)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val prefCat = PreferenceCategory(preferenceScreen.context)
        preferenceScreen.addPreference(prefCat)
        val cryptocurrencies = _mbwManager!!.getWalletManager(false).getCryptocurrenciesNames().toMutableList()
        cryptocurrencies.sortWith(Comparator { c1, c2 -> c1.compareTo(c2, ignoreCase = true) })
        for (name in cryptocurrencies) {
            val listPreference = ListPreference(preferenceScreen.context)
            listPreference.title = name
            val denominationMap = LinkedHashMap<String, Denomination>()
            var defaultValue = ""
            val coinType = Utils.getTypeByName(name)!!
            var symbol = coinType.symbol
            symbol = if (symbol.startsWith("t")) symbol.substring(1) else symbol
            for (value in Denomination.values()) {
                if (value.supportedBy(symbol)) {
                    val key = value.toString().toLowerCase() + "(" + value.getUnicodeString(symbol) + ")"
                    denominationMap[key] = value
                    if (value === _mbwManager!!.getDenomination(coinType)) {
                        defaultValue = key
                    }
                }
            }
            listPreference.setDefaultValue(defaultValue)
            listPreference.value = defaultValue
            listPreference.entries = denominationMap.keys.toTypedArray()
            listPreference.entryValues = denominationMap.keys.toTypedArray()
            listPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                _mbwManager!!.setBitcoinDenomination(coinType, denominationMap[newValue.toString()])
                true
            }
            listPreference.layoutResource = R.layout.preference_layout_no_icon
            listPreference.widgetLayoutResource = R.layout.preference_arrow
            listPreference.dialogTitle = "$name denomination"
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
        const val ARG_FRAGMENT_OPEN_TYPE = "fragment_open_type"

        @JvmStatic
        fun newInstance(pageId: String): DenominationFragment {
            val fragment = DenominationFragment()
            val args = Bundle()
            args.putString(ARG_PREFS_ROOT, pageId)
            fragment.arguments = args
            return fragment
        }
    }
}
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

class BlockExplorersFragment : PreferenceFragmentCompat() {

    private var displayPreferenceDialogHandler: DisplayPreferenceDialogHandler? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, arguments?.getString(ARG_PREFS_ROOT))

        val _mbwManager: MbwManager = MbwManager.getInstance(activity!!)
        displayPreferenceDialogHandler = DisplayPreferenceDialogHandler(preferenceScreen.context)
        setHasOptionsMenu(true)
        (activity as SettingsActivity).supportActionBar!!.run {
            setTitle(R.string.block_explorer_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        val prefCat = PreferenceCategory(preferenceScreen.context)
        preferenceScreen.addPreference(prefCat)
        val cryptocurrencies = _mbwManager.cryptocurrenciesSorted
        for (name in cryptocurrencies) {
            val listPreference = ListPreference(preferenceScreen.context).apply {
                title = name

                val blockExplorerManager = _mbwManager._blockExplorerManager.getBEMByCurrency(name)!!
                value = blockExplorerManager.blockExplorer.identifier
                val blockExplorerNames = blockExplorerManager.getBlockExplorerNames(blockExplorerManager.allBlockExplorer)
                val blockExplorerValues = blockExplorerManager.blockExplorerIds
                entries = blockExplorerNames
                entryValues = blockExplorerValues
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    _mbwManager.setBlockExplorer(name, blockExplorerManager.getBlockExplorerById(newValue.toString()))
                    true
                }
                layoutResource = R.layout.preference_layout_no_icon
                widgetLayoutResource = R.layout.preference_arrow
                dialogTitle = "$name block explorer"
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
        fun create(pageId: String) = BlockExplorersFragment().apply {
            arguments = Bundle().apply { putString(ARG_PREFS_ROOT, pageId) }
        }
    }
}
package com.mycelium.wallet.activity.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.MinerFee.fromString
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler
import com.mycelium.wallet.MinerFee.ECONOMIC
import com.mycelium.wallet.MinerFee.LOWPRIO
import com.mycelium.wallet.MinerFee.NORMAL
import com.mycelium.wallet.MinerFee.PRIORITY

class MinerFeeFragment : PreferenceFragmentCompat() {
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
        actionBar!!.setTitle(R.string.pref_miner_fee_title)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        actionBar.setDisplayShowHomeEnabled(false)
        actionBar.setDisplayHomeAsUpEnabled(true)

        val prefCat = PreferenceCategory(preferenceScreen.context)
        preferenceScreen.addPreference(prefCat)
        val minerFees = arrayOf<CharSequence>(LOWPRIO.toString(), ECONOMIC.toString(), NORMAL.toString(), PRIORITY.toString())
        val minerFeeNames = arrayOf<CharSequence>(getString(R.string.miner_fee_lowprio_name), getString(R.string.miner_fee_economic_name), getString(R.string.miner_fee_normal_name), getString(R.string.miner_fee_priority_name))
        val cryptocurrencies = _mbwManager!!.getWalletManager(false).getCryptocurrenciesNames().toMutableList()
        cryptocurrencies.sortWith(Comparator { c1, c2 -> c1.compareTo(c2, ignoreCase = true) })
        for (name in cryptocurrencies) {
            val listPreference = ListPreference(preferenceScreen.context)
            listPreference.title = name
            setSummary(name, listPreference)
            listPreference.value = _mbwManager!!.getMinerFee(name).toString()
            listPreference.entries = minerFeeNames
            listPreference.entryValues = minerFees
            listPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                _mbwManager!!.setMinerFee(name, fromString(newValue.toString()))
                setSummary(name, listPreference)
                val description = _mbwManager!!.getMinerFee(name).getMinerFeeDescription(requireActivity())
                Utils.showSimpleMessageDialog(requireContext(), description)
                true
            }
            listPreference.layoutResource = R.layout.preference_layout_no_icon
            listPreference.widgetLayoutResource = R.layout.preference_arrow
            listPreference.dialogTitle = "$name miner fee"
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

    private fun setSummary(name: String, pref: Preference) {
        // TODO fix usage of hardcoded values as soon as the summary for ethereum will be provided
        if (name == Utils.getBtcCoinType().name) {
            pref.summary = getMinerFeeSummary()
        }
    }

    private fun getMinerFeeSummary(): String {
        val blocks = when (_mbwManager!!.getMinerFee(Utils.getBtcCoinType().name)) {
            LOWPRIO -> 20
            ECONOMIC -> 10
            NORMAL -> 3
            PRIORITY -> 1
            null -> 3
        }
        return resources.getString(R.string.pref_miner_fee_block_summary,
                blocks.toString())
    }

    companion object {
        private const val ARG_PREFS_ROOT = "preference_root_key"
        const val ARG_FRAGMENT_OPEN_TYPE = "fragment_open_type"

        @JvmStatic
        fun newInstance(pageId: String): MinerFeeFragment {
            val fragment = MinerFeeFragment()
            val args = Bundle()
            args.putString(ARG_PREFS_ROOT, pageId)
            fragment.arguments = args
            return fragment
        }
    }
}
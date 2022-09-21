package com.mycelium.wallet.activity.settings

import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.MinerFee.*
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.model.SendFioModel
import com.mycelium.wallet.activity.send.model.SendFioModel.Companion.DEFAULT_FEE
import com.mycelium.wallet.activity.settings.helper.DisplayPreferenceDialogHandler
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.fio.getFioAccounts

class MinerFeeFragment : PreferenceFragmentCompat() {

    private var displayPreferenceDialogHandler: DisplayPreferenceDialogHandler? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, arguments?.getString(ARG_PREFS_ROOT))

        val mbwManager = MbwManager.getInstance(requireContext().applicationContext)
        displayPreferenceDialogHandler = DisplayPreferenceDialogHandler(preferenceScreen.context)
        setHasOptionsMenu(true)
        (activity as SettingsActivity).supportActionBar!!.apply {
            setTitle(R.string.pref_miner_fee_title)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }

        val prefCat = PreferenceCategory(preferenceScreen.context)
        preferenceScreen.addPreference(prefCat)
        val minerFees = arrayOf<CharSequence>(LOWPRIO.toString(), ECONOMIC.toString(), NORMAL.toString(), PRIORITY.toString())
        val minerFeeNames = arrayOf<CharSequence>(getString(R.string.miner_fee_lowprio_name), getString(R.string.miner_fee_economic_name), getString(R.string.miner_fee_normal_name), getString(R.string.miner_fee_priority_name))
        val cryptocurrencies = mbwManager.cryptocurrenciesSorted
        for (name in cryptocurrencies) {
            if (name.startsWith("FIO")) {
                prefCat.addPreference(Preference(preferenceScreen.context).apply {
                    title = name
                    summary = getString(R.string.settings_summary_fio_fee, "", "")
                    layoutResource = R.layout.preference_layout_no_icon
                    val account = mbwManager.getWalletManager(false).getFioAccounts().first()
                    SendFioModel.UpdateFeeTask(account) { feeInSUF ->
                        val coinType = account.coinType
                        val feeValue = Value.valueOf(coinType, feeInSUF ?: DEFAULT_FEE)
                        val rate = mbwManager.exchangeRateManager
                            .get(feeValue, mbwManager.getFiatCurrency(coinType))
                            ?.let { "~${it.toStringWithUnit()}" }
                            .orEmpty()
                        summary = this@MinerFeeFragment.context?.getString(
                            R.string.settings_summary_fio_fee,
                            feeValue.toStringWithUnit(),
                            rate
                        )
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                })
            } else {
                val listPreference = ListPreference(preferenceScreen.context).apply {
                    title = name
                    summary = getMinerFeeSummary(name)
                    value = mbwManager.getMinerFee(name).toString()
                    entries = minerFeeNames
                    entryValues = minerFees
                    onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                        mbwManager.setMinerFee(name, fromString(newValue.toString()))
                        summary = getMinerFeeSummary(name)
                        val description = mbwManager.getMinerFee(name).getMinerFeeDescription(requireActivity())
                        Utils.showSimpleMessageDialog(requireContext(), description)
                        true
                    }
                    layoutResource = R.layout.preference_layout_no_icon
                    widgetLayoutResource = R.layout.preference_arrow
                    dialogTitle = "$name miner fee"
                }
                prefCat.addPreference(listPreference)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            requireFragmentManager().popBackStack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        displayPreferenceDialogHandler!!.onDisplayPreferenceDialog(preference)
    }

    private fun getMinerFeeSummary(coinName: String): String {
        val mbwManager = MbwManager.getInstance(requireContext())
        val blocks =
                when (coinName) {
                    EthMain.name, EthTest.name ->
                        when (mbwManager.getMinerFee(coinName)) {
                            LOWPRIO -> 120
                            ECONOMIC -> 20
                            NORMAL -> 8
                            PRIORITY -> 2
                            null -> 8
                        }
                    else ->
                        when (mbwManager.getMinerFee(coinName)) {
                            LOWPRIO -> 20
                            ECONOMIC -> 10
                            NORMAL -> 3
                            PRIORITY -> 1
                            null -> 3
                        }
                }

        return resources.getString(R.string.pref_miner_fee_block_summary,
                blocks.toString())
    }

    companion object {
        private const val ARG_PREFS_ROOT = "preference_root_key"

        @JvmStatic
        fun create(pageId: String) = MinerFeeFragment().apply {
            arguments = Bundle().apply { putString(ARG_PREFS_ROOT, pageId) }
        }
    }
}
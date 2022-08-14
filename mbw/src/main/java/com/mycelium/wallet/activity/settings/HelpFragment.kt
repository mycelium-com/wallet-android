package com.mycelium.wallet.activity.settings

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.ConnectionLogsActivity
import com.mycelium.wallet.databinding.FragmentHelpBinding
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.fio.FioModule


class HelpFragment : Fragment() {

    private var binding: FragmentHelpBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentHelpBinding.inflate(inflater).apply { binding = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as SettingsActivity).supportActionBar?.setTitle(R.string.help_menu)
        val mbwManager = MbwManager.getInstance(requireContext())
        binding?.missingTxs?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(Html.fromHtml(getString(R.string.help_missing_tx_dialog_text)))
                    .setPositiveButton(getString(R.string.boost_gap_limit)) { _, _ ->
                        mbwManager.getWalletManager(false).startSynchronization(SyncMode.BOOSTED)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
        binding?.shareLogs?.setOnClickListener {
            ConnectionLogsActivity.callMe(requireActivity())
        }
        binding?.contactSupportTeam?.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:support@mycelium.com")), getString(R.string.send_mail)));
        }
        if (BuildConfig.DEBUG) {
            binding?.fioServerErrorLogs?.visibility = View.VISIBLE
            binding?.fioServerErrorLogs?.setOnClickListener {
                val fioModule = mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule?
                val logs = fioModule!!.getFioServerLogsListAndClear()
                if (logs.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.no_logs), Toast.LENGTH_SHORT).show()
                } else {
                    val joined = TextUtils.join("\n", logs)
                    Utils.setClipboardString(joined, requireContext())
                    Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            if (item.itemId == android.R.id.home) {
                requireFragmentManager().popBackStack()
                true
            } else super.onOptionsItemSelected(item)

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
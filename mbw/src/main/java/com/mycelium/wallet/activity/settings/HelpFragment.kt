package com.mycelium.wallet.activity.settings

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.ConnectionLogsActivity
import com.mycelium.wallet.databinding.FragmentHelpBinding
import com.mycelium.wallet.databinding.LayoutGapInputBinding
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount


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
        // set the whole action bar like the other sub-screens: after a rotation the activity is recreated
        // and the root screen no longer configures it for us
        (requireActivity() as SettingsActivity).supportActionBar?.run {
            setTitle(R.string.help_menu)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayShowHomeEnabled(false)
            setDisplayHomeAsUpEnabled(true)
        }
        val mbwManager = MbwManager.getInstance(requireContext())
        binding?.missingTxs?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                    .setMessage(Html.fromHtml(getString(R.string.help_missing_tx_dialog_text)))
                .setPositiveButton(getString(R.string.boost_gap_limit)) { _, _ ->
                    boostGapLimitDialog(mbwManager)
                }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
        binding?.shareLogs?.setOnClickListener {
            ConnectionLogsActivity.callMe(requireActivity())
        }
        binding?.contactSupportTeam?.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO)
                    .setData("mailto:${getString(R.string.contact_email)}".toUri()), getString(R.string.send_mail)));
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


fun Fragment.boostGapLimitDialog(
    mbwManager: MbwManager,
    vararg accounts: WalletAccount<*>
) {
    val bind = LayoutGapInputBinding.inflate(LayoutInflater.from(requireContext()))
    bind.text.setText(SyncMode.Mode.BOOSTED.lookAhead.toString())
    val dialog = AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.input_look_ahead))
        .setView(bind.root)
        // the click listener is set after show() so an invalid value keeps the dialog open
        .setPositiveButton(R.string.button_ok, null)
        .setNegativeButton(R.string.cancel, null)
        .show()
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
        // toIntOrNull() is null for empty input and for values overflowing Int
        val lookAheadCount = bind.text.text.toString().trim().toIntOrNull()
        if (lookAheadCount == null || lookAheadCount !in 1..MAX_BOOSTED_LOOK_AHEAD) {
            bind.text.error = getString(R.string.input_look_ahead_invalid, MAX_BOOSTED_LOOK_AHEAD)
        } else {
            mbwManager.getWalletManager(false)
                .startSynchronization(SyncMode.boosted(lookAheadCount), accounts.toList())
            dialog.dismiss()
        }
    }
}

// Upper bound for the user defined gap limit: 5x the default BOOSTED look ahead (200).
// Every address of the window is derived for each derivation type and queried from the server,
// so huge values (e.g. 200200) keep the BOOSTED sync busy for many minutes without progress.
private const val MAX_BOOSTED_LOOK_AHEAD = 1000
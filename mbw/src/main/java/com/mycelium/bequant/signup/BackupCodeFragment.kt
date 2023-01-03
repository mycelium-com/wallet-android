package com.mycelium.bequant.signup

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.client.models.TotpCreateResponse
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.external.partner.openLink
import kotlinx.android.synthetic.main.fragment_bequant_backup_code.*
import kotlinx.android.synthetic.main.layout_dialog_view_msg.view.*


class BackupCodeFragment : Fragment(R.layout.fragment_bequant_backup_code) {

    private var response: TotpCreateResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_backup_code)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        next.isEnabled = false
        backupCodeWritten.setOnCheckedChangeListener { _, checked ->
            updateUI()
        }
        next.setOnClickListener {
            findNavController().navigate(BackupCodeFragmentDirections.actionNext(response!!))
        }

        createTotp()
    }

    private fun createTotp() {
        loader(true)
        Api.signRepository.totpCreate(lifecycleScope, {
            response = it
            val (backupPassword, otpId, otpLink) = it!!
            backupCodeView.text = backupPassword.substring(0, backupPassword.length / 2 + 1) + "\n" + backupPassword.substring(backupPassword.length / 2 + 1)
        }, error = { _, _ ->
            AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.backup_not_generated))
                    .setView(LayoutInflater.from(requireContext()).inflate(R.layout.layout_dialog_view_msg, null, false).apply {
                        this.message?.let {
                            it.text = Html.fromHtml(context.getString(R.string.try_request_backup_again))
                            it.setOnClickListener {
                                openLink(BequantConstants.LINK_SUPPORT_CENTER)
                            }
                        }
                        this.message
                    })
                    .setCancelable(false)
                    .setPositiveButton(R.string.close) { _, _ -> requireActivity().finish() }
                    .setNegativeButton(R.string.retry) { _, _ ->
                        createTotp()
                    }.show()
        }, finally = {
            loader(false)
            updateUI()
        })
    }

    private fun updateUI() {
        val checked = backupCodeWritten.isChecked && response != null
        backupCodeNote.visibility = if (checked) GONE else VISIBLE
        next.isEnabled = checked
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
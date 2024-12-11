package com.mycelium.bequant.signup

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentBequantBackupCodeBinding
import com.mycelium.wallet.databinding.LayoutDialogViewMsgBinding
import com.mycelium.wallet.external.partner.openLink


class BackupCodeFragment : Fragment(R.layout.fragment_bequant_backup_code) {

    private var response: TotpCreateResponse? = null
    private var binding: FragmentBequantBackupCodeBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantBackupCodeBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_backup_code)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        binding?.next?.isEnabled = false
        binding?.backupCodeWritten?.setOnCheckedChangeListener { _, checked ->
            updateUI()
        }
        binding?.next?.setOnClickListener {
            findNavController().navigate(BackupCodeFragmentDirections.actionNext(response!!))
        }

        createTotp()
    }

    private fun createTotp() {
        loader(true)
        Api.signRepository.totpCreate(lifecycleScope, {
            response = it
            val (backupPassword, otpId, otpLink) = it!!
            binding?.backupCodeView?.text = backupPassword.substring(0, backupPassword.length / 2 + 1) + "\n" + backupPassword.substring(backupPassword.length / 2 + 1)
        }, error = { _, _ ->
            val dialogBinding = LayoutDialogViewMsgBinding.inflate(LayoutInflater.from(requireContext()))
            AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.backup_not_generated))
                    .setView(dialogBinding.apply {
                        this.message?.let {
                            it.text = Html.fromHtml(context?.getString(R.string.try_request_backup_again))
                            it.setOnClickListener {
                                openLink(BequantConstants.LINK_SUPPORT_CENTER)
                            }
                        }
                        this.message
                    }.root)
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
        val checked = binding?.backupCodeWritten?.isChecked == true && response != null
        binding?.backupCodeNote?.visibility = if (checked) GONE else VISIBLE
        binding?.next?.isEnabled = checked
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
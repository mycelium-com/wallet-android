package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.client.models.TotpCreateResponse
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_backup_code.*


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
            backupCodeNote.visibility = if (checked) GONE else VISIBLE
            next.isEnabled = checked
        }
        next.setOnClickListener {
            findNavController().navigate(BackupCodeFragmentDirections.actionNext(response!!))
        }

        loader(true)
        Api.signRepository.totpCreate(lifecycleScope, {
            response = it
            val (backupPassword, otpId, otpLink) = it!!
            backupCodeView.text = backupPassword.substring(0, backupPassword.length / 2 + 1) + "\n" + backupPassword.substring(backupPassword.length / 2 + 1)
        }, error = { _, message ->
            ErrorHandler(requireContext()).handle(message)
        }, finally = {
            loader(false)
        })
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
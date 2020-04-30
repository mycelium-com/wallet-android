package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_backup_code.*


class BackupCodeFragment : Fragment(R.layout.fragment_bequant_backup_code) {

    private var otpId = 0
    private var otpLink = ""
    private var backupCode = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_backup_code)
        next.isEnabled = false
        backupCodeWritten.setOnCheckedChangeListener { _, checked ->
            next.isEnabled = checked
        }
        next.setOnClickListener {
            findNavController().navigate(BackupCodeFragmentDirections.actionNext(otpId, otpLink, backupCode))
        }
        val loader = LoaderFragment()
        loader.show(parentFragmentManager, "loader")
        SignRepository.repository.totpCreate({ otpId, otpLink, backupCode ->
            this.otpId = otpId
            this.otpLink = otpLink
            this.backupCode = backupCode
            loader.dismissAllowingStateLoss()
            backupCodeView.text = backupCode.substring(0, backupCode.length / 2 + 1) + "\n" + backupCode.substring(backupCode.length / 2 + 1)
        }, {
            loader.dismissAllowingStateLoss()
            ErrorHandler(requireContext()).handle(it)
        })
    }
}
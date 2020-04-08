package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_backup_code.*


class BackupCodeFragment : Fragment(R.layout.fragment_bequant_backup_code) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        next.isEnabled = false
        backupCodeWritten.setOnCheckedChangeListener { compoundButton, checked ->
            next.isEnabled = checked
        }
        next.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
    }

}
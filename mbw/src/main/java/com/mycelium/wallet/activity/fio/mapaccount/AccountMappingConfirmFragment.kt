package com.mycelium.wallet.activity.fio.mapaccount

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import kotlinx.android.synthetic.main.fragment_fio_account_mapping_confirm.*
import java.util.*


class AccountMappingConfirmFragment : Fragment(R.layout.fragment_fio_account_mapping_confirm) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = " Confirm accounts mapping"
        }
        mapNameMsg.text = String.format("Please double check and confirm that the following accounts will be associated (mapped) with the FIO name \"%s\"", "test@fio.com")
        val accounts = arguments?.getStringArray("accounts")
        val manager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        accountLabels.text = accounts?.map { manager.getAccount(UUID.fromString(it))?.label }?.joinToString("\n")
        confirmButton.setOnClickListener {
            Toaster(requireActivity()).toast("Accounts were successfully mapped", false)
            requireActivity().finish()
        }
        updateButton()
        acknowledge.setOnCheckedChangeListener { compoundButton, b ->
            updateButton()
        }
    }

    private fun updateButton() {
        confirmButton.isEnabled = acknowledge.isChecked
    }
}
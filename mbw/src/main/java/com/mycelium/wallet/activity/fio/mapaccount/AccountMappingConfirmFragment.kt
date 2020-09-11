package com.mycelium.wallet.activity.fio.mapaccount

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import kotlinx.android.synthetic.main.fragment_fio_account_mapping_confirm.*
import java.util.*


class AccountMappingConfirmFragment : Fragment(R.layout.fragment_fio_account_mapping_confirm) {
    private val viewModel: FIOMapPubAddressViewModel by activityViewModels()

    @ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.run {
            title = " Confirm accounts mapping"
        }
        mapNameMsg.text = String.format("Please double check and confirm that the following accounts will be associated (mapped) with the FIO name \"%s\"", viewModel.fioAddress.value)
        val accounts = arguments?.getStringArray("accounts")
        val manager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        accountLabels.text = accounts?.map { manager.getAccount(UUID.fromString(it))?.label }?.joinToString("\n")
        confirmButton.setOnClickListener {
            viewModel.account.value!!.addPubAddress(viewModel.fioAddress.value!!,
                    accounts?.map { manager.getAccount(UUID.fromString(it))?.receiveAddress }!!.map {
                        // for the cryptocurrencies we currently deal with chainCode == tokenCode
                        val tokenChainCode = getChainCode(it!!.coinType.name)
                        TokenPublicAddress(it.toString(), tokenChainCode, tokenChainCode)
                    })
            Toaster(requireActivity()).toast("Accounts were successfully mapped", false)
            requireActivity().finish()
        }
        updateButton()
        acknowledge.setOnCheckedChangeListener { _, _ ->
            updateButton()
        }
    }

    private fun getChainCode(name: String): String =
            if (name.startsWith("t")) {
                name.substring(1)
            } else {
                name
            }

    private fun updateButton() {
        confirmButton.isEnabled = acknowledge.isChecked
    }
}
package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.receive.viewmodel.ShowQRViewModel
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.bequant.remote.model.DepositAddress
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentBequantReceiveShowQrBinding
import kotlinx.android.synthetic.main.fragment_bequant_receive_show_qr.*


class ShowQRFragment : Fragment() {
    lateinit var viewModel: ShowQRViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ShowQRViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantReceiveShowQrBinding>(inflater, R.layout.fragment_bequant_receive_show_qr, container, false)
                    .apply {
                        viewModel = this@ShowQRFragment.viewModel
                        lifecycleOwner = this@ShowQRFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        copyAddress.setOnClickListener {
            Utils.setClipboardString(viewModel.address.value, requireContext())
            Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
        createNewAddress.setOnClickListener {
            createDepositAddress("BTC")
        }
        requestDepositAddress("BTC")
    }

    fun requestDepositAddress(currency: String) {
        val loader = LoaderFragment()
        loader.show(parentFragmentManager, Constants.LOADER_TAG)
        ApiRepository.repository.depositAddress(currency, {
            loader.dismissAllowingStateLoss()
            updateData(it, currency)
        }, { code, message ->
            loader.dismissAllowingStateLoss()
            ErrorHandler(requireContext()).handle(message)
        })
    }

    fun createDepositAddress(currency: String) {
        val loader = LoaderFragment()
        loader.show(parentFragmentManager, Constants.LOADER_TAG)
        ApiRepository.repository.createDepositAddress(currency, {
            loader.dismissAllowingStateLoss()
            updateData(it, currency)
        }, { code, message ->
            loader.dismissAllowingStateLoss()
            ErrorHandler(requireContext()).handle(message)
        })
    }

    private fun updateData(it: DepositAddress, currency: String) {
        qrCodeView?.qrCode = it.address
        viewModel.address.value = it.address
        viewModel.addressLabel.value = "$currency Deposit Address"
        viewModel.tagLabel.value = "$currency Deposit Tag"
    }
}
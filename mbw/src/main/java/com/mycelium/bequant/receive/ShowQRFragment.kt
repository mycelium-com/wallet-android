package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import com.mycelium.bequant.receive.viewmodel.ShowQRViewModel
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentBequantReceiveShowQrBinding
import kotlinx.android.synthetic.main.fragment_bequant_receive_show_qr.*

class ShowQRFragment : Fragment() {
    lateinit var viewModel: ShowQRViewModel
    var parentViewModel: ReceiveCommonViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ShowQRViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantReceiveShowQrBinding>(inflater, R.layout.fragment_bequant_receive_show_qr, container, false)
                    .apply {
                        viewModel = this@ShowQRFragment.viewModel
                        parentViewModel = this@ShowQRFragment.parentViewModel
                        lifecycleOwner = this@ShowQRFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        copyAddress.setOnClickListener {
            Utils.setClipboardString(parentViewModel?.address?.value, requireContext())
            Toast.makeText(requireContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
        createNewAddress.setOnClickListener {
            createDepositAddress(parentViewModel?.currency?.value ?: "BTC")
        }
    }

    private fun createDepositAddress(currency: String) {
        loader(true)
        viewModel.createDepositAddress()
        ApiRepository.repository.createDepositAddress(currency, {
            loader(false)
            parentViewModel?.address?.value = it.address
            parentViewModel?.tag?.value = it.paymentId?.toString()
        }, { code, message ->
            loader(false)
            ErrorHandler(requireContext()).handle(message)
        })
    }
}
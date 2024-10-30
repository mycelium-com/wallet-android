package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.receive.adapter.ReceiveFragmentAdapter
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantReceiveBinding
import java.util.*


class ReceiveFragment : Fragment(R.layout.fragment_bequant_receive) {
    val args by navArgs<ReceiveFragmentArgs>()
    val viewModel: ReceiveCommonViewModel by viewModels()
    var binding: FragmentBequantReceiveBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.currency.value = args.currency

        fetchDepositAddress()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantReceiveBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val supportedByMycelium = getSupportedByMycelium(args.currency)
        binding?.pager?.adapter = ReceiveFragmentAdapter(this, viewModel, supportedByMycelium)
        binding?.tabs?.setupWithViewPager(binding?.pager)
        viewModel.error.observe(viewLifecycleOwner) {
            //if no address just suppress this message, because it is not error
//            ErrorHandler(requireContext()).handle(it)
        }
        viewModel.currency.observe(viewLifecycleOwner, Observer {
            fetchDepositAddress()
        })
    }

    private fun getSupportedByMycelium(currency: String): Boolean {
        return currency.toLowerCase(Locale.getDefault()) in listOf("eth", "btc")
    }

    private fun fetchDepositAddress() {
        this.loader(true)
        viewModel.fetchDepositAddress {
            this.loader(false)
        }
    }
}
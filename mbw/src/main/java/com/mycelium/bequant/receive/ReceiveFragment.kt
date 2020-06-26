package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.receive.adapter.ReceiveFragmentAdapter
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_receive.*


class ReceiveFragment : Fragment(R.layout.fragment_bequant_receive) {

    val args by navArgs<ReceiveFragmentArgs>()
    lateinit var viewModel: ReceiveCommonViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ReceiveCommonViewModel::class.java)
        viewModel.currency.value = args.currency

        requestDepositAddress(viewModel.currency.value!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = ReceiveFragmentAdapter(this, viewModel)
        tabs.setupWithViewPager(pager)
        viewModel.error.observe(viewLifecycleOwner) {
            ErrorHandler(requireContext()).handle(it)
        }
        viewModel.currency.observe(viewLifecycleOwner, Observer {
            requestDepositAddress(viewModel.currency.value!!)
        })
    }

    private fun requestDepositAddress(currency: String) {
        this.loader(true)
        viewModel.depositAddress {
            this.loader(false)
        }
    }
}
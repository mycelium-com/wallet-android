package com.mycelium.bequant.market

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.Constants.REQUEST_CODE_EXCHANGE_COINS
import com.mycelium.bequant.exchange.SelectCoinActivity
import com.mycelium.bequant.kyc.BequantKycActivity
import com.mycelium.bequant.market.model.ExchangeViewModel
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import kotlinx.android.synthetic.main.fragment_bequant_exchange.*


class ExchangeFragment : Fragment(R.layout.fragment_bequant_exchange) {

    private lateinit var viewModel: ExchangeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 25..100 step 25) {
            send_percent.addTab(send_percent.newTab().setText("$i%"))
        }
        viewModel = ViewModelProviders.of(this).get(ExchangeViewModel::class.java)
        viewModel.available.observe(viewLifecycleOwner, Observer {
            available.text = it.toStringWithUnit(Denomination.UNIT)
        })
        viewModel.youSend.observe(viewLifecycleOwner, Observer {
            sendView.text = it.toString(Denomination.UNIT)
        })
        viewModel.youGet.observe(viewLifecycleOwner, Observer {})

        sendView.setOnClickListener {
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java), REQUEST_CODE_EXCHANGE_COINS)
        }
        getView.setOnClickListener {
            startActivityForResult(Intent(requireContext(), SelectCoinActivity::class.java), REQUEST_CODE_EXCHANGE_COINS)
        }
        exchange.setOnClickListener {
            startActivity(Intent(requireActivity(),BequantKycActivity::class.java))
        }
    }
}
package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.withdraw.adapter.WithdrawFragmentAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawCommonViewModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_withdraw.*


class WithdrawFragment : Fragment(R.layout.fragment_bequant_withdraw) {

    lateinit var viewModel: WithdrawCommonViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WithdrawCommonViewModel::class.java)
        if (arguments?.containsKey("currency") == true) {
            viewModel.currency.value = arguments?.getString("currency")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = WithdrawFragmentAdapter(this, viewModel)
        pager.offscreenPageLimit = 2
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Mycelium Wallet"
                1 -> tab.text = "Address"
            }
        }.attach()
    }
}
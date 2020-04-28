package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.receive.viewmodel.FromMyceliumViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentBequantReceiveFromMyceliumBinding
import com.mycelium.wallet.databinding.FragmentBequantRegistrationInfoBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_receive_from_mycelium.*


class FromMyceliumFragment : Fragment() {

    lateinit var viewModel: FromMyceliumViewModel
    val adapter = AccountPagerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(FromMyceliumViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantReceiveFromMyceliumBinding>(inflater, R.layout.fragment_bequant_receive_from_mycelium, container, false)
                    .apply {
                        viewModel = this@FromMyceliumFragment.viewModel
                        lifecycleOwner = this@FromMyceliumFragment
                    }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mbwManager = MbwManager.getInstance(requireContext())
        adapter.submitList(mbwManager.getWalletManager(false).getSpendingAccountsWithBalance())
        fromAccounts.adapter = adapter
        TabLayoutMediator(fromAccountsTab, fromAccounts) { tab, _ ->
        }.attach()


        if (mbwManager.hasFiatCurrency()) {
            val coin = Utils.getBtcCoinType()
            val value = mbwManager.exchangeRateManager.get(coin.oneCoin(), mbwManager.getFiatCurrency(coin))
            if (value == null) {
                viewModel.oneCoinFiatRate.value = getString(R.string.exchange_source_not_available
                        , mbwManager.exchangeRateManager.getCurrentExchangeSourceName(coin.symbol))
            } else {
                oneCoinFiatRate.text = resources.getString(R.string.balance_rate
                        , coin.symbol, mbwManager.getFiatCurrency(coin).symbol, value.toString())
            }
        }
    }
}
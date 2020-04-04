package com.mycelium.bequant.market.adapter

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.market.AccountFragment
import com.mycelium.bequant.market.ExchangeFragment
import com.mycelium.bequant.market.MarketsFragment
import com.mycelium.wallet.R


class MarketFragmentAdapter(val fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> MarketsFragment()
                1 -> ExchangeFragment()
                2 -> AccountFragment().apply {
                    receiveListener = {
                        fragment.findNavController()
                                .navigate(R.id.action_market_to_receive)
                    }

                    withdrawListener = {
                        fragment.findNavController()
                                .navigate(R.id.action_market_to_withdraw)
                    }
                }
                else -> TODO("not implemented")
            }
}
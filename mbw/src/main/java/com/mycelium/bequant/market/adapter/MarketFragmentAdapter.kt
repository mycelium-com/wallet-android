package com.mycelium.bequant.market.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.market.AccountFragment
import com.mycelium.bequant.market.ExchangeFragment
import com.mycelium.bequant.market.MarketsFragment


class MarketFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> MarketsFragment()
                1 -> ExchangeFragment()
                2 -> AccountFragment()
                else -> TODO("not implemented")
            }
}
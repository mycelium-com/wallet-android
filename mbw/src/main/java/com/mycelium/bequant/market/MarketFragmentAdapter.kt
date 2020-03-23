package com.mycelium.bequant.market

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class MarketFragmentAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> MarketsFragment()
                1 -> ExchangeFragment()
                2 -> AccountFragment()
                else -> TODO("not implemented")
            }
}
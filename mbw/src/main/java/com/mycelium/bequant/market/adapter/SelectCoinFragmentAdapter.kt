package com.mycelium.bequant.market.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.exchange.SelectCoinFragment

class SelectCoinFragmentAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> SelectCoinFragment()
                1 -> SelectCoinFragment()
                else -> TODO("not implemented")
            }
}
package com.mycelium.giftbox.cards.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.giftbox.cards.CardsFragment
import com.mycelium.giftbox.cards.OrdersFragment
import com.mycelium.giftbox.cards.StoresFragment


class CardsFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> StoresFragment()
                1 -> OrdersFragment()
                2 -> CardsFragment()
                else -> TODO("not implemented")
            }
}
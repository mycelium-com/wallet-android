package com.mycelium.giftbox.cards.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.giftbox.cards.PurchasedFragment
import com.mycelium.giftbox.cards.StoresFragment


class CardsFragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> StoresFragment()
                1 -> PurchasedFragment()
                else -> TODO("not implemented")
            }
}
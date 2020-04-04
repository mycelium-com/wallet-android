package com.mycelium.bequant.withdraw.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mycelium.bequant.withdraw.WithdrawAddressFragment
import com.mycelium.bequant.withdraw.WithdrawWalletFragment


class WithdrawFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> WithdrawWalletFragment()
                1 -> WithdrawAddressFragment()
                else -> TODO("not implemented")
            }

}
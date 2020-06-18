package com.mycelium.bequant.receive.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.mycelium.bequant.receive.FromMyceliumFragment
import com.mycelium.bequant.receive.ShowQRFragment
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel


class ReceiveFragmentAdapter(fa: Fragment, val vm: ReceiveCommonViewModel) : FragmentStatePagerAdapter(fa.childFragmentManager) {

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> FromMyceliumFragment().apply { parentViewModel = vm }
        1 -> ShowQRFragment().apply { parentViewModel = vm }
        else -> TODO("not implemented")
    }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence? =
            when (position) {
                0 -> "From Mycelium"
                1 -> "Show QR"
                else -> ""
            }
}
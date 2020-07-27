package com.mycelium.bequant.receive.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.mycelium.bequant.receive.FromMyceliumFragment
import com.mycelium.bequant.receive.ShowQRFragment
import com.mycelium.bequant.receive.viewmodel.ReceiveCommonViewModel
import org.web3j.abi.datatypes.Bool


class ReceiveFragmentAdapter(fa: Fragment, val vm: ReceiveCommonViewModel, val supportedByMycelium: Boolean) :
        FragmentStatePagerAdapter(fa.childFragmentManager) {

    override fun getItem(position: Int): Fragment = getFragment(position)

    private fun getFragment(position: Int): Fragment {
        return when (position) {
            0 -> ShowQRFragment().apply { parentViewModel = vm }
            1 -> FromMyceliumFragment().apply { parentViewModel = vm }
            else -> TODO("not implemented")
        }
    }

    override fun getCount(): Int = if (supportedByMycelium) 2 else 1

    override fun getPageTitle(position: Int): CharSequence? =
            when (position) {
                0 -> "Show QR"
                1 -> "From Mycelium"
                else -> ""
            }
}
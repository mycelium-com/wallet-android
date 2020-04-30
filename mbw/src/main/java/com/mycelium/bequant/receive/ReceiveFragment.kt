package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.receive.adapter.ReceiveFragmentAdapter
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_receive.*


class ReceiveFragment : Fragment(R.layout.fragment_bequant_receive) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = ReceiveFragmentAdapter(this)
        tabs.setupWithViewPager(pager)
//        TabLayoutMediator(tabs, pager) { tab, position ->
//            when (position) {
//                0 -> tab.text = "From Mycelium"
//                1 -> tab.text = "Show QR"
//            }
//        }.attach()
    }
}
package com.mycelium.bequant.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.common.adapter.CoinAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_receive_choose_coin.*


class ChoseCoinFragment : Fragment(R.layout.fragment_bequant_receive_choose_coin) {
    val adapter = CoinAdapter()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
    }
}
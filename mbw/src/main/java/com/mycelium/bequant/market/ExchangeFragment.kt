package com.mycelium.bequant.market

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_exchange.*


class ExchangeFragment : Fragment(R.layout.fragment_bequant_exchange) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in 25..100 step 25) {
            send_percent.addTab(send_percent.newTab().setText("$i%"))
        }
    }
}
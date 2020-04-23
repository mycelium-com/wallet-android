package com.mycelium.bequant.market

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.market.adapter.MarketAdapter
import com.mycelium.bequant.market.model.MarketItem
import com.mycelium.bequant.market.model.MarketTitleItem
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_markets.*


class MarketsFragment : Fragment(R.layout.fragment_bequant_markets) {

    val adapter = MarketAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        adapter.submitList(listOf(MarketTitleItem(0),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "+99.63%"),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "+99.63%"),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "-0.63%"),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "+99.63%"),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "+99.63%"),
                MarketItem("BCH / BTC", "Vol 5,636", "0.038077", "$334,52", "+99.63%")))
    }
}
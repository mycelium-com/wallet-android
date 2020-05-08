package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.market.adapter.MarketAdapter
import com.mycelium.bequant.market.viewmodel.MarketItem
import com.mycelium.bequant.market.viewmodel.MarketTitleItem
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.bequant.remote.model.Ticker
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_markets.*


class MarketsFragment : Fragment(R.layout.fragment_bequant_markets) {
    val adapter = MarketAdapter()
    var tickersData = listOf<Ticker>()
    val receive = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            requestTickers()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receive, IntentFilter(Constants.ACTION_BEQUANT_KEYS))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        search.doOnTextChanged { text, start, count, after ->
            updateList(text?.toString() ?: "")
        }
        requestTickers()
    }

    private fun requestTickers() {
        if (BequantPreference.hasKeys()) {
            ApiRepository.repository.tickers({
                tickersData = it
                updateList()
            }, { code, error ->
                ErrorHandler(requireContext()).handle(error)
            })
        }
    }

    private fun updateList(filter: String = "") {
        if (filter.isEmpty()) {
            adapter.submitList(listOf(MarketTitleItem(0)) + tickersData
                    .map {
                        val change = if (it.last == null || it.open == null) null
                        else {
                            100 - it.last / it.open * 100
                        }
                        MarketItem("${it.symbol.substring(0, 3)} / ${it.symbol.substring(3)}",
                                it.volume, it.last, getUSDForPriceCurrency(it.symbol.substring(0, 3)), change)
                    })
        } else {
            adapter.submitList(listOf(MarketTitleItem(0)) + tickersData
                    .filter { it.symbol.contains(filter, true) }
                    .map {
                        val change = if (it.last == null || it.open == null) null
                        else {
                            100 - it.last / it.open * 100
                        }
                        MarketItem("${it.symbol.substring(0, 3)} / ${it.symbol.substring(3)}",
                                it.volume, it.last, getUSDForPriceCurrency(it.symbol.substring(0, 3)), change)
                    })
        }
    }

    private fun getUSDForPriceCurrency(currency: String): Double? =
            tickersData.firstOrNull { it.symbol.equals("${currency}USD", true) }?.last

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receive)
        super.onDestroyView()
    }
}
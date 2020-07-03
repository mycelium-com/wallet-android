package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.market.adapter.MarketAdapter
import com.mycelium.bequant.market.viewmodel.MarketItem
import com.mycelium.bequant.market.viewmodel.MarketTitleItem
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Ticker
import com.mycelium.wallet.R
import com.mycelium.wapi.api.lib.CurrencyCode
import kotlinx.android.synthetic.main.fragment_bequant_markets.*


class MarketsFragment : Fragment(R.layout.fragment_bequant_markets) {
    private var sortDirection = true // true means desc, false - asc
    private var sortField = 1 // 1 - volume
    private val adapter = MarketAdapter { pos: Int, desc: Boolean ->
        sortField = pos
        sortDirection = desc
        updateList()
    }
    private var tickersData = listOf<Ticker>()
    private val receive = object : BroadcastReceiver() {
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
            updateList(text?.toString()?.trim() ?: "")
        }
        requestTickers()
    }

    private fun requestTickers() {
        loader(true)
        Api.publicRepository.publicTickerGet(viewLifecycleOwner.lifecycle.coroutineScope,null,{
            tickersData = it?.toList()?: emptyList()
            updateList()
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        },{
            loader(false)
        })
    }

    private fun updateList(filter: String = "") {
        var marketItems = tickersData
                .filter { c -> !CurrencyCode.values().any { code -> c.symbol.contains(code.shortString, true) } }
                .filter { if (filter.isNotEmpty()) it.symbol.contains(filter, true) else true }
                .map {
                    val change = if (it.last == null || it.open == null) null
                    else {
                        100 - it.last / it.open * 100
                    }
                    //TODO get correct symbol from symbols call (maybe from bqexchangermanager)
                    MarketItem(it.symbol.substring(0, 3), it.symbol.substring(3),
                            it.volume?:0.0, it.last, getUSDForPriceCurrency(it.symbol.substring(0, 3)), change)
                }
        marketItems = when (sortField) {
            0 -> if (sortDirection) {
                marketItems.sortedByDescending { it.from + it.to }
            } else {
                marketItems.sortedBy { it.from + it.to }
            }
            1 -> if (sortDirection) {
                marketItems.sortedByDescending { it.volume }
            } else {
                marketItems.sortedBy { it.volume }
            }
            2 -> if (sortDirection) {
                marketItems.sortedByDescending { it.price }
            } else {
                marketItems.sortedBy { it.price }
            }
            3 -> if (sortDirection) {
                marketItems.sortedByDescending { it.change }
            } else {
                marketItems.sortedBy { it.change }
            }
            else -> marketItems
        }
        adapter.submitList(listOf(MarketTitleItem(sortField)) + marketItems)
    }

    private fun getUSDForPriceCurrency(currency: String): Double? =
            tickersData.firstOrNull { it.symbol.equals("${currency}USD", true) }?.last

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receive)
        super.onDestroyView()
    }
}
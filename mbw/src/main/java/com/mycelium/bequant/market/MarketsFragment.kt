package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.Constants
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.market.adapter.MarketAdapter
import com.mycelium.bequant.market.viewmodel.MarketItem
import com.mycelium.bequant.market.viewmodel.MarketTitleItem
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Symbol
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
        search.setOnEditorActionListener { textView, i, keyEvent ->
            updateList(search.text?.toString()?.trim() ?: "")
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(search.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
            true
        }
        requestTickers()
    }

    private fun requestTickers() {
        loader(true)
        Api.publicRepository.publicTickerGet(viewLifecycleOwner.lifecycleScope, null, {
            tickersData = it?.toList() ?: emptyList()
            updateList()
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
    }

    private fun updateList(filter: String = "") {
        val filteredTickers = tickersData
                .filter { ticker ->
                    if (filter.isNotEmpty())
                        filter.split(" ", "/").filter { it.trim().isNotEmpty() }
                                .all { ticker.symbol.contains(it, true) }
                    else true
                }
        Api.publicRepository.publicSymbolGet(viewLifecycleOwner.lifecycleScope,
                filteredTickers.joinToString(",") { it.symbol }, { symbols: Array<Symbol>? ->
            filteredTickers.mapNotNull { tiker ->
                val change = if (tiker.last == null || tiker.open == null) null
                else {
                    100 - tiker.last / tiker.open * 100
                }
                symbols?.find {
                    it.id == tiker.symbol
                }?.let { symbol ->
                    MarketItem(symbol.baseCurrency, symbol.quoteCurrency,
                            tiker.volume ?: 0.0, tiker.last,
                            getUSDForPriceCurrency(symbol.baseCurrency), change)
                }
            }.filter { c ->
                !CurrencyCode.values().any { code ->
                    c.from.equals(code.shortString, true) || c.to.equals(code.shortString, true)
                }
            }.let { marketItems ->
                adapter.submitList(listOf(MarketTitleItem(sortField)) + when (sortField) {
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
                })
            }
        }, { code, msg -> ErrorHandler(requireContext()).handle(msg) }, {})
    }

    private fun getUSDForPriceCurrency(currency: String): Double? =
            tickersData.firstOrNull { it.symbol.equals("${currency}USD", true) }?.last

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receive)
        super.onDestroyView()
    }
}
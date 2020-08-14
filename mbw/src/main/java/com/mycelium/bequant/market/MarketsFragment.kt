package com.mycelium.bequant.market

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import java.util.*
import kotlin.math.roundToInt


class MarketsFragment : Fragment(R.layout.fragment_bequant_markets) {
    private var sortDirection = true // true means desc, false - asc
    private var sortField = 1 // 1 - volume
    private val adapter = MarketAdapter { pos: Int, desc: Boolean ->
        sortField = pos
        sortDirection = desc
        updateList(search?.text?.toString()?.trim() ?: "")
    }
    private var tickersData = listOf<Ticker>()
    private var tickerTimer: Timer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        list.itemAnimator = null
        search.doOnTextChanged { text, _, _, _ ->
            updateList(text?.toString()?.trim() ?: "")
        }
        search.setOnEditorActionListener { _, _, _ ->
            updateList(search.text?.toString()?.trim() ?: "")
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(search.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        tickerTimer = Timer("market-tickets", false).apply {
            schedule(object : TimerTask() {
                override fun run() {
                    list?.post {
                        requestTickers()
                    }
                }
            }, 0L, 5000L)
        }
    }

    override fun onPause() {
        tickerTimer?.cancel()
        tickerTimer = null
        super.onPause()
    }

    private fun requestTickers() {
        if (tickersData.isEmpty()) {
            loader(true)
        }
        Api.publicRepository.publicTickerGet(viewLifecycleOwner.lifecycleScope, null, {
            tickersData = it?.toList() ?: emptyList()
            updateList(search?.text?.toString()?.trim() ?: "")
        }, { _, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
    }

    private fun updateList(searchString: String = "") {
        val filteredTickers = tickersData
                .filter { ticker ->
                    searchString.isEmpty() ||
                        searchString.split(" ", "/").filter {
                            it.trim().isNotEmpty()
                        }.all {
                            ticker.symbol.contains(it, true)
                        }
                }
        Api.publicRepository.publicSymbolGet(viewLifecycleOwner.lifecycleScope,
                filteredTickers.joinToString(",", transform = { it.symbol }),
                success = { symbols ->
                    filteredTickers.mapNotNull { ticker ->
                        val change = if (ticker.last == null || ticker.open == null) {
                            null
                        } else {
                            ticker.last / ticker.open * 100 - 100
                        }
                        symbols?.find {
                            it.id == ticker.symbol
                        }?.let { symbol ->
                            MarketItem(symbol.baseCurrency, symbol.quoteCurrency,
                                    (convertToVolumeInUSDT(symbol, ticker.volume!!) ?: 0.0).roundToInt(), ticker.last,
                                    getUSDForPriceCurrency(symbol.baseCurrency), change)
                        }
                    }.filter { marketItem ->
                        !CurrencyCode.values().any { code ->
                            marketItem.from.equals(code.shortString, true) || marketItem.to.equals(code.shortString, true)
                        }
                    }.let { marketItems ->
                        fun <T, R : Comparable<R>> Iterable<T>.mSort(selector: (T) -> R?) =
                                if (sortDirection) sortedByDescending(selector) else sortedBy(selector)
                        adapter.submitList(listOf(MarketTitleItem(sortField)) + when (sortField) {
                            0 -> marketItems.mSort { it.from + it.to }
                            1 -> marketItems.mSort { it.volume }
                            2 -> marketItems.mSort { it.price }
                            3 -> marketItems.mSort { it.change }
                            else -> marketItems
                        })
                    }
                },
                error = { _, msg ->
                    ErrorHandler(requireContext()).handle(msg)
                }, finally = {
        })
    }

    private fun convertToVolumeInUSDT(symbol: Symbol, volume: Double): Double? {
        return when {
            getUSDForPriceCurrency(symbol.baseCurrency) != null -> {
                volume * getUSDForPriceCurrency(symbol.baseCurrency)!!
            }
            getUSDForPriceCurrency(symbol.quoteCurrency) != null -> {
                volume * getUSDForPriceCurrency(symbol.quoteCurrency)!!
            }
            else -> {
                null
            }
        }
    }

    // for symbols with USD the USD can be represented either as "USD" or "USDB"
    private fun getUSDForPriceCurrency(currency: String): Double? =
            tickersData.firstOrNull { it.symbol.equals("${currency}USD", true) ||
                    it.symbol.equals("${currency}USDB", true)}?.last

    override fun onDestroyView() {
        list.adapter = null
        super.onDestroyView()
    }
}
package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.assetInfoById
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_bequant_exchange_select_coin.*

class SelectCoinFragment : Fragment(R.layout.fragment_bequant_exchange_select_coin) {
    val adapter = CoinAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        list.adapter = adapter

        var response: List<Currency>? = null

        loader(true)
        Api.publicRepository.publicCurrencyGet(viewLifecycleOwner.lifecycle.coroutineScope, null, { list ->
            response = list?.toList() ?: emptyList()
            // and create CoinListItem with them
            val coinsList = mutableListOf(CoinListItem(CoinAdapter.TYPE_SEARCH), CoinListItem(CoinAdapter.TYPE_SPACE))
            response?.forEach {
                coinsList.add(CoinListItem(CoinAdapter.TYPE_ITEM, it.assetInfoById()))
            }
            adapter.submitList(coinsList)
        }, { code, error ->
            ErrorHandler(requireContext()).handle(error)
        }, {
            loader(false)
        })
    }
}
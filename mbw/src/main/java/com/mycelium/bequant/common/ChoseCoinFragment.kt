package com.mycelium.bequant.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.Constants.TYPE_ITEM
import com.mycelium.bequant.Constants.TYPE_SEARCH
import com.mycelium.bequant.common.adapter.CoinAdapter
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.remote.repositories.ApiRepository
import com.mycelium.bequant.remote.model.Currency
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_bequant_receive_choose_coin.*


class ChoseCoinFragment : Fragment(R.layout.fragment_bequant_receive_choose_coin) {
    val adapter = CoinAdapter()

    val args by navArgs<ChoseCoinFragmentArgs>()
    var currencies = listOf<Currency>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCurrencies()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        adapter.coinClickListener = {
            when (args.action) {
                "deposit" -> findNavController().navigate(ChoseCoinFragmentDirections.actionDeposit(it.symbol))
                "withdraw" -> findNavController().navigate(ChoseCoinFragmentDirections.actionWithdraw(it.symbol))
            }
        }
        adapter.searchChangeListener = {
            updateList(it)
        }
    }

    private fun loadCurrencies() {
        loader(true)
        Api.apiRepository.currencies({ list ->
            currencies = list
            loader(false)
            updateList()
        }, { code, error ->
            loader(false)
            ErrorHandler(requireContext()).handle(error)
        })
    }

    private fun updateList(filter: String = "") {
        adapter.submitList(mutableListOf<CoinListItem>().apply {
            add(CoinListItem(TYPE_SEARCH, null))
            if (filter.isEmpty()) {
                addAll(currencies.map { CoinListItem(TYPE_ITEM, it.assetInfoById()) })
            } else {
                addAll(currencies
                        .map { CoinListItem(TYPE_ITEM, it.assetInfoById()) }
                        .filter { it.coin?.symbol?.contains(filter, true) == true || it.coin?.name?.contains(filter, true) == true })
            }
        })
    }
}
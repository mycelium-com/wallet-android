package com.mycelium.bequant.common

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.BequantConstants.TYPE_ITEM
import com.mycelium.bequant.BequantConstants.TYPE_SEARCH
import com.mycelium.bequant.common.adapter.CoinAdapter
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.trading.model.Currency
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_bequant_receive_choose_coin.*


class ChoseCoinFragment : Fragment(R.layout.fragment_bequant_receive_choose_coin) {
    val adapter = CoinAdapter()

    val args by navArgs<ChoseCoinFragmentArgs>()
    var currencies = listOf<Currency>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrencies()
        list.adapter = adapter
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        adapter.coinClickListener = {
            when (it.symbol) {
                "EURB", "USDB", "GBPB" -> {
                    AlertDialog.Builder(requireContext())
                            .setMessage(getString(R.string.bequant_fiat_tx_not_supported))
                            .setPositiveButton(R.string.button_ok) { _, _ ->
                            }.show()
                }
                else -> when (args.action) {
                    "deposit" -> findNavController().navigate(ChoseCoinFragmentDirections.actionDeposit(it.symbol))
                    "withdraw" -> findNavController().navigate(ChoseCoinFragmentDirections.actionWithdraw(it.symbol))
                }
            }
        }
        adapter.searchChangeListener = {
            updateList(it)
        }
    }

    private fun loadCurrencies() {
        loader(true)
        Api.publicRepository.publicCurrencyGet(viewLifecycleOwner.lifecycle.coroutineScope,null, {
            currencies = it?.toList()?: listOf()
            updateList()
        }, { _, error ->
            ErrorHandler(requireContext()).handle(error)
        },{
            loader(false)
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
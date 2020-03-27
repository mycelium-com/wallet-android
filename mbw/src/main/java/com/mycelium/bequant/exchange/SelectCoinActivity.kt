package com.mycelium.bequant.exchange

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.bequant.exchange.model.CoinListItem
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_exchange_select_coin.*


class SelectCoinActivity : AppCompatActivity(R.layout.activity_bequant_exchange_select_coin) {

    val adapter = CoinAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list.adapter = adapter

        adapter.submitList(listOf(CoinListItem(CoinAdapter.TYPE_SEARCH), CoinListItem(CoinAdapter.TYPE_SPACE)))
    }
}
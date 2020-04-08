package com.mycelium.bequant.market

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.adapter.MarketFragmentAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_main.*


class MarketFragment : Fragment(R.layout.fragment_bequant_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pager.adapter = MarketFragmentAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Markets"
                1 -> tab.text = "Exchange"
                2 -> tab.text = "Account"
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bequant_market, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                else -> super.onOptionsItemSelected(item)
            }
}
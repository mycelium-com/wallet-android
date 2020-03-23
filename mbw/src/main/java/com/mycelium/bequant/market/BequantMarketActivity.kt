package com.mycelium.bequant.market

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_market.*


class BequantMarketActivity : AppCompatActivity(R.layout.activity_bequant_market) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            title = "Powered by"
            subtitle = "BEQUANT"
            setDisplayShowHomeEnabled(true)
            setIcon(R.drawable.action_bar_logo)
        }
        pager.adapter = MarketFragmentAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Markets"
                1 -> tab.text = "Exchange"
                2 -> tab.text = "Account"
            }
        }.attach()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bequant_market, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                else -> super.onOptionsItemSelected(item)
            }
}
package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.adapter.SelectCoinFragmentAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_exchange_select_coin.*


class SelectCoinActivity : AppCompatActivity(R.layout.activity_bequant_exchange_select_coin) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pager.adapter = SelectCoinFragmentAdapter(this)
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "You send"
                1 -> tab.text = "You get"
            }
        }.attach()
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_back_arrow))
            setTitle(R.string.exchange)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.ExchangeFragment
import com.mycelium.bequant.market.adapter.SelectCoinFragmentAdapter
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.coins.AssetInfo
import kotlinx.android.synthetic.main.activity_bequant_exchange_select_coin.*


class SelectCoinActivity : AppCompatActivity(R.layout.activity_bequant_exchange_select_coin) {
    val youSendYouGetPair = MutableLiveData<Pair<AssetInfo, AssetInfo>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pager.adapter = SelectCoinFragmentAdapter(this)
        pager.offscreenPageLimit = 2
        TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.bequant_you_send)
                1 -> tab.text = getString(R.string.bequant_you_get)
            }
        }.attach()
        pager.setCurrentItem(intent.getIntExtra(ExchangeFragment.PARENT, 0), true)
        youSendYouGetPair.value = intent.getSerializableExtra(ExchangeFragment.YOU_SEND_YOU_GET_PAIR) as Pair<AssetInfo, AssetInfo>
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_back_arrow))
            setTitle(R.string.exchange)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
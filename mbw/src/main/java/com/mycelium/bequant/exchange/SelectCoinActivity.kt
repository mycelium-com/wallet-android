package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.market.ExchangeFragment
import com.mycelium.bequant.market.adapter.SelectCoinFragmentAdapter
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantExchangeSelectCoinBinding
import com.mycelium.wapi.wallet.coins.AssetInfo


class SelectCoinActivity : AppCompatActivity() {
    val youSendYouGetPair = MutableLiveData<Pair<AssetInfo, AssetInfo>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBequantExchangeSelectCoinBinding.inflate(layoutInflater)
        binding.pager.adapter = SelectCoinFragmentAdapter(this)
        binding.pager.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.bequant_you_send)
                1 -> tab.text = getString(R.string.bequant_you_get)
            }
        }.attach()
        binding.pager.setCurrentItem(intent.getIntExtra(ExchangeFragment.PARENT, 0), true)
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
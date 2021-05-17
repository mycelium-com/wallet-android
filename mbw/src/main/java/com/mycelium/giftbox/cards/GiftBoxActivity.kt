package com.mycelium.giftbox.cards

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.giftbox.cards.adapter.CardsFragmentAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_main.*

class GiftBoxActivity : AppCompatActivity(R.layout.activity_gift_box) {

    var mediator: TabLayoutMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pager.adapter = CardsFragmentAdapter(supportFragmentManager, lifecycle)
        pager.offscreenPageLimit = 2
        mediator = TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Stores"
                1 -> tab.text = "Purchased cards"
            }
        }
        mediator?.attach()

    }

    override fun onDestroy() {
        pager.adapter = null
        super.onDestroy()
    }
}
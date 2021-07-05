package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.giftbox.cards.adapter.CardsFragmentAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_main.*

class GiftBoxFragment : Fragment(R.layout.fragment_gift_box) {

    var mediator: TabLayoutMediator? = null

    val args by navArgs<GiftBoxFragmentArgs>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
        }
        pager.adapter = CardsFragmentAdapter(childFragmentManager, lifecycle)
        pager.offscreenPageLimit = 2
        mediator = TabLayoutMediator(tabs, pager) { tab, position ->
            when (position) {
                0 -> tab.text = "Stores"
                1 -> tab.text = "Purchases"
                2 -> tab.text = "Gift cards"
            }
        }
        mediator?.attach()
        pager.setCurrentItem(if (args.startWithPurchased) 1 else 0, false)
    }
}
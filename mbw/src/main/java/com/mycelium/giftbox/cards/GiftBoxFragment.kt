package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.mrd.bitlib.crypto.toBiMap
import com.mycelium.giftbox.cards.adapter.CardsFragmentAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_main.*

class GiftBoxFragment : Fragment(R.layout.fragment_gift_box) {

    var mediator: TabLayoutMediator? = null

    val args by navArgs<GiftBoxFragmentArgs>()
    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val tabMap = mapOf(0 to STORES,
            1 to PURCHASES,
            2 to CARDS).toBiMap()

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
        pager.currentItem = tabMap.inverse()[activityViewModel.currentTab.value] ?: 0
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activityViewModel.currentTab.value = tabMap[position] ?: STORES
            }
        })
    }

    companion object {
        const val STORES = "stores"
        const val PURCHASES = "purchases"
        const val CARDS = "cards"
    }
}
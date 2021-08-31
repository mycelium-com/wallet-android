package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.mrd.bitlib.crypto.toBiMap
import com.mycelium.giftbox.cards.adapter.CardsFragmentAdapter
import com.mycelium.giftbox.cards.event.RefreshOrdersRequest
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_main.*

class GiftBoxFragment : Fragment(R.layout.fragment_gift_box) {

    var mediator: TabLayoutMediator? = null

    val args by navArgs<GiftBoxFragmentArgs>()
    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val tabMap = mapOf(0 to STORES,
            1 to PURCHASES,
            2 to CARDS).toBiMap()

    private var refreshItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
                0 -> tab.text = getString(R.string.stores)
                1 -> tab.text = getString(R.string.mygiftcards)
                2 -> tab.text = getString(R.string.purchases)
            }
        }
        mediator?.attach()
        activityViewModel.orderLoading.observe(viewLifecycleOwner, Observer {
            if (it) {
                showRefresh()
            } else {
                hideRefresh()
            }
        })
    }

    val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            activityViewModel.currentTab.value = tabMap[position] ?: STORES
        }
    }

    override fun onResume() {
        super.onResume()
        pager.postDelayed({
            pager.currentItem = tabMap.inverse()[activityViewModel.currentTab.value] ?: 0
            pager.registerOnPageChangeCallback(pageChangeCallback)
        }, 10)
    }

    override fun onPause() {
        pager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.refresh, menu);
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        refreshItem = menu.findItem(R.id.miRefresh)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.miRefresh -> {
                    MbwManager.getEventBus().post(RefreshOrdersRequest())
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun hideRefresh() {
        refreshItem?.actionView = null
    }

    private fun showRefresh() {
        refreshItem?.setActionView(R.layout.actionbar_indeterminate_progress)?.apply {
            actionView?.findViewById<ImageView>(R.id.ivTorIcon)?.visibility = View.GONE
        }
    }

    companion object {
        const val STORES = "stores"
        const val PURCHASES = "purchases"
        const val CARDS = "cards"
    }
}
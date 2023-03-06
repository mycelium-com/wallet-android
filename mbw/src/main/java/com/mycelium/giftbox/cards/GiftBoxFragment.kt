package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
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
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.collapse
import com.mycelium.wallet.activity.util.expand
import com.mycelium.wallet.databinding.FragmentGiftBoxBinding
import com.mycelium.wallet.event.NetworkConnectionStateChanged
import com.squareup.otto.Subscribe

class GiftBoxFragment : Fragment() {

    var mediator: TabLayoutMediator? = null

    val args by navArgs<GiftBoxFragmentArgs>()
    val activityViewModel: GiftBoxViewModel by activityViewModels()
    val tabMap = mapOf(0 to STORES,
            1 to CARDS,
            2 to PURCHASES).toBiMap()

    private var refreshItem: MenuItem? = null
    private var binding: FragmentGiftBoxBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentGiftBoxBinding.inflate(inflater)
        .apply {
            binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
        }
        binding?.pager?.adapter = CardsFragmentAdapter(childFragmentManager, lifecycle)
        binding?.pager?.offscreenPageLimit = 2
        mediator = TabLayoutMediator(binding?.tabs!!, binding?.pager!!) { tab, position ->
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
        updateNetworkConnectionState()
    }

    val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            activityViewModel.currentTab.value = tabMap[position] ?: STORES
        }
    }

    override fun onResume() {
        super.onResume()
        MbwManager.getEventBus().register(this)
        binding?.pager?.postDelayed({
            binding?.pager?.currentItem = tabMap.inverse()[activityViewModel.currentTab.value] ?: 0
            binding?.pager?.registerOnPageChangeCallback(pageChangeCallback)
        }, 10)
    }

    override fun onPause() {
        binding?.pager?.unregisterOnPageChangeCallback(pageChangeCallback)
        MbwManager.getEventBus().unregister(this)
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
    private fun hideRefresh() {
        refreshItem?.actionView = null
    }

    private fun showRefresh() {
        refreshItem?.setActionView(R.layout.actionbar_indeterminate_progress)?.apply {
            actionView?.findViewById<ImageView>(R.id.ivTorIcon)?.visibility = View.GONE
        }
    }

    @Subscribe
    fun networkConnectionChanged(event: NetworkConnectionStateChanged){
        updateNetworkConnectionState()
    }

    fun updateNetworkConnectionState() {
        if (Utils.isConnected(requireContext())) {
            binding?.connectionError?.collapse()
        } else {
            binding?.connectionError?.expand()
        }
    }

    companion object {
        const val STORES = "stores"
        const val PURCHASES = "purchases"
        const val CARDS = "cards"
    }
}
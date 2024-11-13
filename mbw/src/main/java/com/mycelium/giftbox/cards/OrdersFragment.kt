package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.cards.adapter.OrderAdapter
import com.mycelium.giftbox.cards.adapter.PurchasedGroupItem
import com.mycelium.giftbox.cards.adapter.PurchasedLoadingItem
import com.mycelium.giftbox.cards.adapter.PurchasedOrderItem
import com.mycelium.giftbox.cards.event.OrdersUpdate
import com.mycelium.giftbox.cards.event.RefreshOrdersRequest
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.common.ListState
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.news.adapter.PaginationScrollListener
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding
import com.mycelium.wallet.startCoroutineTimer
import com.squareup.otto.Subscribe
import java.util.concurrent.TimeUnit


class OrdersFragment : Fragment() {
    private val adapter = OrderAdapter()
    private val viewModel: PurchasedViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxPurchasedBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            FragmentGiftboxPurchasedBinding.inflate(inflater).apply {
                binding = this
                this.viewModel = this@OrdersFragment.viewModel
                this.lifecycleOwner = this@OrdersFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.noResultTitle?.text = getString(R.string.no_purchased_order)
        binding?.noResultText?.text = getString(R.string.no_order_linked)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(
                DividerItemDecoration(
                        resources.getDrawable(R.drawable.divider_bequant),
                        VERTICAL
                )
        )
        binding?.list?.addOnScrollListener(object : PaginationScrollListener(binding!!.list.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                loadData(viewModel.orders.value?.size?.toLong() ?: 0)
            }

            override fun isLastPage() = viewModel.orders.value?.size ?: 0 <= viewModel.ordersSize.value ?: 0

            override fun isLoading() = viewModel.state.value == ListState.LOADING
        })
        adapter.itemClickListener = {
            findNavController().navigate(GiftBoxFragmentDirections.actionOrderDetails(null, null, null, null, null, null, null, it))
        }
        adapter.groupListener = { group ->
            GiftboxPreference.setGroupOpen(group, !GiftboxPreference.isGroupOpen(group))
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
            binding?.list?.postDelayed({
                binding?.list?.smoothScrollToPosition(adapter.currentList.indexOfFirst { it is PurchasedGroupItem && it.title == group } + 5)
            }, 300)
        }
        activityViewModel.currentTab.observeForever { binding?.list?.scrollToPosition(0) }
        startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.MINUTES.toMillis(1)) {
            loadData()
        }
        MbwManager.getEventBus().register(this)
    }

    private fun loadData(offset: Long = 0) {
        when {
            offset == 0L -> {
                adapter.submitList(List(8) { PurchasedLoadingItem })
                activityViewModel.orderLoading.value = true
            }
            offset >= viewModel.ordersSize.value ?: 0 -> {
                return
            }
            else -> {
                adapter.submitList(adapter.currentList + PurchasedLoadingItem)
            }
        }
        viewModel.state.value = ListState.LOADING
        GitboxAPI.mcGiftRepository.getOrders(lifecycleScope, offset, success = {
            viewModel.setOrdersResponse(it?.list, offset != 0L)
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
            MbwManager.getEventBus().post(OrdersUpdate())
        }, error = { code, msg ->
            adapter.submitList(listOf())
            viewModel.state.value = ListState.ERROR
            if(code != 400) {
                Toaster(this).toast(msg, true)
            }
        }, finally = {
            activityViewModel.orderLoading.value = false
        })
    }

    private fun generateList(data: List<MCOrderResponse>) = data.map { PurchasedOrderItem(it) }

    override fun onDestroyView() {
        MbwManager.getEventBus().unregister(this)
        binding = null
        super.onDestroyView()
    }

    @Subscribe
    internal fun updateOrder(request: RefreshOrdersRequest) {
        loadData()
    }
}
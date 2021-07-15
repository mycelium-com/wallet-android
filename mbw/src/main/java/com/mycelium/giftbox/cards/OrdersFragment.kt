package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.cards.adapter.OrderAdapter
import com.mycelium.giftbox.cards.adapter.PurchasedGroupItem
import com.mycelium.giftbox.cards.adapter.PurchasedLoadingItem
import com.mycelium.giftbox.cards.adapter.PurchasedOrderItem
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.Order
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding


class OrdersFragment : Fragment() {
    private val adapter = OrderAdapter()
    private val viewModel: PurchasedViewModel by viewModels()
    private var binding: FragmentGiftboxPurchasedBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            FragmentGiftboxPurchasedBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(
                DividerItemDecoration(
                        resources.getDrawable(R.drawable.divider_bequant),
                        VERTICAL
                )
        )
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
        loadData()
    }

    private fun loadData(offset: Long = 0) {
        if (offset == 0L) {
            adapter.submitList(List(8) { PurchasedLoadingItem })
        } else if (offset >= viewModel.ordersSize) {
            return
        }
        GitboxAPI.giftRepository.getOrders(lifecycleScope, offset, success = {
            viewModel.setOrdersResponse(it, offset != 0L)
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
        }, error = { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun generateList(data: List<Order>) = data.map { PurchasedOrderItem(it) }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
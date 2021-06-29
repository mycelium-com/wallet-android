package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.cards.adapter.*
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.details.MODE
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding


class PurchasedFragment : Fragment() {

    private val adapter = PurchasedAdapter()
    private val viewModel: PurchasedViewModel by viewModels()
    private var binding: FragmentGiftboxPurchasedBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
            findNavController().navigate(GiftBoxFragmentDirections.actionDetails(it, MODE.INFO))
        }
        adapter.itemShareListener = {

        }
        adapter.itemRedeemListener = {
            GiftboxPreference.redeem(it)
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
        }
        adapter.itemDeleteListener = {
            AlertDialog.Builder(requireContext(), R.style.MyceliumModern_Dialog)
                    .setTitle(getString(R.string.delete_gift_card))
                    .setMessage(getString(R.string.delete_gift_card_msg))
                    .setNegativeButton(R.string.button_cancel) { _, _ -> }
                    .setPositiveButton(R.string.delete) { _, _ ->
                        GiftboxPreference.remove(it)
                        adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
                    }
                    .create().show()
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
        GitboxAPI.giftRepository.getOrders(lifecycleScope, offset, 30, {
            viewModel.setOrdersResponse(it, offset != 0L)
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
        }, error = { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun generateList(dataAll: List<Order>) = mutableListOf<PurchasedItem>().apply {
        val data = dataAll.filter { !GiftboxPreference.isRemoved(it) }
        addAll(data.filter { !GiftboxPreference.isRedeemed(it) }.map { PurchasedOrderItem(it) })
        val redeemed = data.filter { GiftboxPreference.isRedeemed(it) }.map {
            PurchasedOrderItem(it, true)
        }
        if (redeemed.isNotEmpty()) {
            val redeemedGroup = getString(R.string.redeemed_gift_cards)
            val isOpened = GiftboxPreference.isGroupOpen(redeemedGroup)
            add(PurchasedGroupItem(redeemedGroup, isOpened))
            if (isOpened) {
                addAll(redeemed)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_store, menu)
        val searchItem = menu.findItem(R.id.actionSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnCloseListener {
            adapter.submitList(generateList(viewModel.orders.value ?: emptyList()))
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                findSearchResult(s)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                findSearchResult(s)
                return true
            }

            private fun findSearchResult(s: String) {
                adapter.submitList(generateList(viewModel.orders.value?.filter {
                    it.productName?.contains(s, true) ?: false
                } ?: emptyList()))
            }
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
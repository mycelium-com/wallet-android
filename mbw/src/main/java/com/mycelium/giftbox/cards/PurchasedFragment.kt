package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.giftbox.cards.adapter.PurchasedAdapter
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.details.MODE
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding


class PurchasedFragment : Fragment() {

    private val adapter = PurchasedAdapter()
            .apply {
                submitList(listOf(PurchasedAdapter.LOADING_ITEM, PurchasedAdapter.LOADING_ITEM, PurchasedAdapter.LOADING_ITEM))
            }
    private val viewModel: PurchasedViewModel by viewModels()
    private var binding: FragmentGiftboxPurchasedBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        loadData()
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

    }

    private fun loadData(offset: Long = 0) {
        if (offset != 0L && offset >= viewModel.ordersSize) {
            return
        }
        GitboxAPI.giftRepository.getOrders(lifecycleScope, offset, 30, {
            viewModel.setOrdersResponse(it, offset != 0L)
            adapter.submitList(viewModel.orders.value)
        }, error = { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_store, menu)
        val searchItem = menu.findItem(R.id.actionSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnCloseListener {
            adapter.submitList(viewModel.orders.value)
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
                adapter.submitList(viewModel.orders.value?.filter {
                    it.productName?.contains(s, true) ?: false
                })
            }
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
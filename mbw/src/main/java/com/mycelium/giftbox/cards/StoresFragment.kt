package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.remote.Status
import com.mycelium.giftbox.cards.adapter.SearchTagAdapter
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.cards.viewmodel.StoresViewModel
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.models.Product
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxStoresBinding


class StoresFragment : Fragment() {

    private val tagsAdapter = SearchTagAdapter()
    private val adapter = StoresAdapter()
    private val viewModel: StoresViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxStoresBinding? = null
    private var products = emptyList<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxStoresBinding.inflate(inflater).apply {
                binding = this
                this.activityViewModel = this@StoresFragment.activityViewModel
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tags?.adapter = tagsAdapter
        tagsAdapter.submitList(listOf("Popular", "Food & Drink", "Books", "Clothes"))
        tagsAdapter.clickListener = {

        }
        binding?.list?.adapter = adapter
        adapter.itemClickListener = {
            findNavController().navigate(
                    GiftBoxFragmentDirections.toCardDetailsFragment(
                            Constants.CLIENT_USER_ID,
                            Constants.CLIENT_ORDER_ID,
                            it.code!!
                    )
            )
        }
        binding?.counties?.setOnClickListener {
            findNavController().navigate(GiftBoxFragmentDirections.actionSelectCountries())
        }
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loader(false)
                    products = it.data?.products ?: emptyList()
                    adapter.submitList(it.data?.products)
                }
                Status.ERROR -> {
                    Toaster(this).toast(it.error?.localizedMessage ?: "", true)
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }

        }
        viewModel.load(StoresViewModel.Params(Constants.CLIENT_USER_ID, Constants.CLIENT_ORDER_ID))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_store, menu)
        val searchItem = menu.findItem(R.id.actionSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnCloseListener {
            adapter.submitList(products)
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
                adapter.submitList(products.filter {
                    it.name?.contains(s, true) ?: false
                })
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
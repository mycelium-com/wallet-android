package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.cards.adapter.SearchTagAdapter
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.cards.viewmodel.StoresViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxStoresBinding


class StoresFragment : Fragment() {

    private val tagsAdapter = SearchTagAdapter()
    private val adapter = StoresAdapter()
    private val viewModel: StoresViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxStoresBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        loadData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxStoresBinding.inflate(inflater).apply {
                binding = this
                this.activityViewModel = this@StoresFragment.activityViewModel
                this.lifecycleOwner = this@StoresFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tags?.adapter = tagsAdapter
        tagsAdapter.clickListener = {
            viewModel.category = it
            loadData()
        }
        activityViewModel.categories.observe(viewLifecycleOwner) {
            tagsAdapter.submitList(it)
        }
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        adapter.itemClickListener = {
            findNavController().navigate(GiftBoxFragmentDirections.toCardDetailsFragment(it))
        }
        binding?.counties?.setOnClickListener {
            findNavController().navigate(GiftBoxFragmentDirections.actionSelectCountries())
        }
        binding?.list?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = binding?.list?.layoutManager
                if (layoutManager is LinearLayoutManager) {
                    if (layoutManager.findLastCompletelyVisibleItemPosition() > adapter.itemCount - 10 &&
                            viewModel.loading.value == false) {
                        loadData(viewModel.products.value?.size?.toLong() ?: 0)
                    }
                }
            }
        })
        activityViewModel.selectedCountries.observe(viewLifecycleOwner) {
            loadData()
        }
    }

    private fun loadData(offset: Long = 0) {
        if (offset == 0L) {
            loader(true)
        } else if (offset >= viewModel.productsSize) {
            return
        }
        viewModel.loading.value = true
        GitboxAPI.giftRepository.getProducts(lifecycleScope,
                search = viewModel.search,
                category = viewModel.category,
                country = activityViewModel.selectedCountries.value,
                offset = offset, limit = 30,
                success = {
                    activityViewModel.categories.value = it?.categories
                    activityViewModel.countries.value = it?.countries?.mapNotNull {
                        CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
                    }
                    viewModel.setProductsResponse(it)
                    adapter.submitList(it?.products)
                },
                error = { _, msg ->
                    Toaster(this).toast(msg, true)
                },
                finally = {
                    loader(false)
                    viewModel.loading.value = false
                })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_store, menu)
        val searchItem = menu.findItem(R.id.actionSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnCloseListener {
            viewModel.search = null
            loadData()
            true
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                viewModel.search = s
                loadData()
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                adapter.submitList(viewModel.products.value?.filter {
                    it.name?.contains(s, true) ?: false
                })
                return true
            }
        })
    }

    override fun onDestroyView() {
        binding?.list?.clearOnScrollListeners()
        binding = null
        super.onDestroyView()
    }
}
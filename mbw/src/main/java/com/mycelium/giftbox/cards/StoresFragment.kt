package com.mycelium.giftbox.cards

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
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
import com.google.android.material.tabs.TabLayout
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.cards.viewmodel.GiftBoxViewModel
import com.mycelium.giftbox.cards.viewmodel.StoresViewModel
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.details.GiftBoxStoreDetailsFragmentArgs
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentGiftboxStoresBinding
import kotlinx.android.synthetic.main.fragment_bequant_markets.*
import kotlinx.android.synthetic.main.media_flow_tab_item.view.*
import kotlinx.coroutines.Job


class StoresFragment : Fragment() {

    private val adapter = StoresAdapter()
    private val viewModel: StoresViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()
    private var binding: FragmentGiftboxStoresBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxStoresBinding.inflate(inflater).apply {
                binding = this
                this.viewModel = this@StoresFragment.viewModel
                this.activityViewModel = this@StoresFragment.activityViewModel
                this.lifecycleOwner = this@StoresFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tags?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.category = if (tab.tag == "All") null else tab.tag.toString()
                loadData()
            }
        })
        activityViewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding?.tags?.let { tags ->
                categories.forEach {
                    if (getTab(it, tags) == null) {
                        val tab = tags.newTab().setCustomView(
                                layoutInflater.inflate(R.layout.media_flow_tab_item, tags, false).apply {
                                    this.text.text = it.replace("-", " ").capitalize()
                                })
                        tab.tag = it
                        tags.addTab(tab)
                    }
                }
                cleanTabs(categories, tags)
            }
        }
        binding?.list?.adapter = adapter
        binding?.list?.itemAnimator = null
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        adapter.itemClickListener = {
            findNavController().navigate(GiftBoxFragmentDirections.actionStoreDetails(it))
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
                            viewModel.loading.value == false && !viewModel.quickSearch) {
                        loadData(viewModel.products.size.toLong())
                    }
                }
            }
        })
        viewModel.search.observe(viewLifecycleOwner) {
            loadData()
        }
        binding?.searchClose?.setOnClickListener {
            viewModel.search.value = null
            hideKeyboard()
        }
        loadData()
    }

    private fun hideKeyboard() {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(search.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private var productsJob : Job? = null
    private fun loadData(offset: Long = -1) {
        if (offset == -1L) {
            adapter.submitList(List(8) { StoresAdapter.LOADING_ITEM })
            productsJob?.cancel()
        } else if (offset >= viewModel.productsSize) {
            return
        }
        viewModel.loading.value = true
        productsJob = GitboxAPI.giftRepository.getProducts(lifecycleScope,
                search = viewModel.search.value,
                category = viewModel.category,
                country = activityViewModel.selectedCountries.value,
                offset = if (offset == -1L) 0 else offset, limit = 30,
                success = {
                    activityViewModel.categories.value = listOf("All") + (it?.categories
                            ?: emptyList())
                    activityViewModel.countries.value = it?.countries?.mapNotNull {
                        CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
                    }
                    viewModel.setProductsResponse(it, offset != -1L)
                    adapter.submitList(viewModel.products)
                },
                error = { _, msg ->
                    Toaster(this).toast(msg, true)
                },
                finally = {
                    viewModel.loading.value = false
                })
    }

    override fun onDestroyView() {
        binding?.list?.clearOnScrollListeners()
        binding = null
        super.onDestroyView()
    }

    private fun getTab(category: String, tabLayout: TabLayout): TabLayout.Tab? {
        for (i in 0 until tabLayout.tabCount) {
            if (tabLayout.getTabAt(i)?.tag == category) {
                return tabLayout.getTabAt(i)
            }
        }
        return null
    }

    private fun cleanTabs(list: List<String>, tabLayout: TabLayout) {
        for (i in tabLayout.tabCount - 1 downTo 0) {
            if (!list.contains(tabLayout.getTabAt(i)?.tag)) {
                tabLayout.removeTab(tabLayout.getTabAt(i)!!)
            }
        }
    }
}
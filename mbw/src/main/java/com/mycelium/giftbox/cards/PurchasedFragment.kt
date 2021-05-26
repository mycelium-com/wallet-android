package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import com.mycelium.bequant.remote.Status
import com.mycelium.giftbox.cards.adapter.PurchasedAdapter
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxPurchasedBinding


class PurchasedFragment : Fragment(R.layout.fragment_giftbox_purchased) {

    private val adapter = PurchasedAdapter()
    private val viewModel: PurchasedViewModel by viewModels()
    private var binding: FragmentGiftboxPurchasedBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxPurchasedBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL))
        adapter.itemClickListener = {
//            findNavController().navigate(PurchasedFragmentDirections.to)
//            startActivity(Intent(requireContext(), GiftBoxDetailsActivity::class.java))
        }
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loader(false)
                    adapter.submitList(it.data?.items)
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
        viewModel.load()
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
                    it.name?.contains(s, true) ?: false
                })
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }
}
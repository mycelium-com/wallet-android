package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.View
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
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import kotlinx.android.synthetic.main.fragment_giftbox_stores.*


class StoresFragment : Fragment(R.layout.fragment_giftbox_stores) {

    private val tagsAdapter = SearchTagAdapter()
    private val adapter = StoresAdapter()
    private val viewModel: StoresViewModel by viewModels()
    private val activityViewModel: GiftBoxViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tags.adapter = tagsAdapter
        tagsAdapter.submitList(listOf("Popular", "Food & Drink", "Books", "Clothes"))
        tagsAdapter.clickListener = {

        }
        list.adapter = adapter
        adapter.itemClickListener = {
            findNavController().navigate(
                    GiftBoxFragmentDirections.toCardDetailsFragment(
                            Constants.CLIENT_USER_ID,
                            Constants.CLIENT_ORDER_ID,
                            it.code!!
                    )
            )
        }
        activityViewModel.counties.observe(viewLifecycleOwner) {
            counties.text = resources.getQuantityString(R.plurals.d_countries, it.size, it.size)
        }
        counties.setOnClickListener {
            findNavController().navigate(GiftBoxFragmentDirections.actionSelectCountries())
        }
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loader(false)
                    adapter.submitList(it.data?.products)
                }
                Status.ERROR -> {
                    Toaster(this).toast(it.error?.localizedMessage, true)
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }

        }
        viewModel.load(StoresViewModel.Params(Constants.CLIENT_USER_ID, Constants.CLIENT_ORDER_ID))
    }
}
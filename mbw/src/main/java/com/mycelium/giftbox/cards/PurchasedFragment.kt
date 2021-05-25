package com.mycelium.giftbox.cards

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.mycelium.bequant.remote.Status
import com.mycelium.giftbox.cards.adapter.PurchasedAdapter
import com.mycelium.giftbox.cards.viewmodel.PurchasedViewModel
import com.mycelium.giftbox.cards.viewmodel.StoresViewModel
import com.mycelium.giftbox.client.Constants
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import kotlinx.android.synthetic.main.fragment_giftbox_purchased.*


class PurchasedFragment : Fragment(R.layout.fragment_giftbox_purchased) {

    private val adapter = PurchasedAdapter()
    val viewModel: PurchasedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter

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
                    Toaster(this).toast(it.error?.localizedMessage?:"", true)
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }
        }
        viewModel.load(PurchasedViewModel.Params(Constants.CLIENT_USER_ID))
    }
}
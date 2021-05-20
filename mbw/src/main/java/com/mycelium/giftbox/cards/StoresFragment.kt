package com.mycelium.giftbox.cards

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.buycard.GiftBoxBuyActivity
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductsResponse
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_giftbox_stores.*


class StoresFragment : Fragment(R.layout.fragment_giftbox_stores) {
    private val adapter = StoresAdapter()

    //    val args by navArgs<StoresFragmentArgs>()
    val vm: StoresFragmentViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
        adapter.itemClickListener = {
            startActivity(Intent(requireContext(), GiftBoxBuyActivity::class.java))
        }

        vm.productsResponse.observe(viewLifecycleOwner) {
            adapter.submitList(
                it.products.map { Card(it.card_image_url, it.name, it.description, it.description) }
            )
        }
//        vm.load(args.clientOrderId, args.clientUserId, args.productId)
        vm.load(Constants.CLIENT_USER_ID, Constants.CLIENT_USER_ID, null)
    }
}

class StoresFragmentViewModel : ViewModel() {

    val productsResponse = MutableLiveData<ProductsResponse>()
    fun load(clientOrderId: String, clientUserId: String, productId: String?) {
//        GitboxAPI.giftRepository.api.products().live
    }
}
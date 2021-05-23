package com.mycelium.giftbox.cards

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import com.mycelium.bequant.remote.Status
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.buycard.GiftBoxBuyActivity
import com.mycelium.giftbox.cards.adapter.StoresAdapter
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.loader
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

        vm.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loader(false)
                    adapter.submitList(
                        it.data?.products
                    )
                }
                Status.ERROR -> {
                    Toast.makeText(requireContext(), it.error?.localizedMessage, Toast.LENGTH_SHORT).show()
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }

        }
        vm.load(StoresFragmentViewModel.Params(Constants.CLIENT_USER_ID, Constants.CLIENT_ORDER_ID))
    }
}

class StoresFragmentViewModel : ViewModel() {

    private val load = MutableLiveData<Params>()
    fun load(params: Params) {
        load.value = params
    }

    val loadSubsription = {
        load.switchMap {
            val (clientUserId, clientOrderId) = it
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.products(
//                    clientUserId = clientUserId,
//                    clientOrderId = clientOrderId
                )
            }.asLiveData()
        }
    }

    data class Params(val clientUserId: String, val clientOrderId: String)
}
package com.mycelium.giftbox.cardDetails

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.remote.Status
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.event.AmountListener
import com.mycelium.wallet.activity.view.loader

class CardDetailsFragment : Fragment(R.layout.fragment_giftbox_card_details), AmountListener {
    val args by navArgs<CardDetailsFragmentArgs>()

    val vm: CardDetailsFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    vm.productResponse.value = it.data
                    loader(false)
                }
                Status.ERROR -> {
                    loader(false)
                }
                Status.LOADING -> {
//                        loader(true)

                }
            }

        }
        vm.load(
            CardDetailsFragmentViewModel.Params(
                args.clientUserId,
                args.clientOrderId,
                args.productId
            )
        )

    }

    override fun onClickAmount() {

    }


}

class CardDetailsFragmentViewModel : ViewModel() {
    val productResponse = MutableLiveData<ProductResponse>()
    private val load = MutableLiveData<Params>()
    fun load(params: Params) {
        load.value = params
    }

    val loadSubsription = {
        load.switchMap {
            val (clientUserId, clientOrderId, productId) = it
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.product(
                    clientUserId = clientUserId,
                    clientOrderId = clientOrderId,
                    productId = productId
                )
            }.asLiveData()
        }
    }

    data class Params(val clientUserId: String, val clientOrderId: String, val productId: String)
}
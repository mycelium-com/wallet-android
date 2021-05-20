package com.mycelium.giftbox.cardDetails

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.receive.ReceiveFragmentArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.event.AmountListener

class CardDetailsFragment : Fragment(R.layout.fragment_giftbox_card_details), AmountListener {
    val args by navArgs<CardDetailsFragmentArgs>()

    val vm: CardDetailsFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewModel.currency.value = args.currency

    }

    override fun onClickAmount() {

    }


}

class CardDetailsFragmentViewModel : ViewModel() {
    val productResponse = MutableLiveData<ProductResponse>()
    fun load() {
        GitboxAPI.giftRepository.product(viewModelScope, "", "", "", {
            productResponse.value = it
        }, { i, s ->

        }, {

        })
    }
}
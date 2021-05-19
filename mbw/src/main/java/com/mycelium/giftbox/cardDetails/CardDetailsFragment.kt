package com.mycelium.giftbox.cardDetails

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.receive.ReceiveFragmentArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.wallet.R

class CardDetailsFragment : Fragment(R.layout.fragment_giftbox_card_details) {
    val args by navArgs<ReceiveFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewModel.currency.value = args.currency
        GitboxAPI.giftRepository.product(lifecycleScope, "","","",{

        },{ i,s->

        },{

        })
    }
}
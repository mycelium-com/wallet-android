package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxBuyResultBinding


class GiftBoxBuyResultFragment : Fragment() {
    val viewModel: GiftboxBuyResultViewModel by viewModels()
    private var binding: FragmentGiftboxBuyResultBinding? = null

    val args by navArgs<GiftBoxBuyResultFragmentArgs>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentGiftboxBuyResultBinding.inflate(inflater).apply {
            binding = this
            vm = viewModel
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.totalAmountFiatString.value = args.totalFiat.toStringWithUnit()
        viewModel.totalAmountCryptoString.value = "~" + args.totalCrypto.toStringWithUnit()
        viewModel.minerFeeFiat.value = args.minerFeeFiat.toStringWithUnit()
        viewModel.minerFeeCrypto.value = "~" + args.minerFeeCrypto.toStringWithUnit()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

class GiftboxBuyResultViewModel : ViewModel() {
    val totalAmountFiatString = MutableLiveData("")
    val totalAmountCryptoString = MutableLiveData("")
    val minerFeeFiat = MutableLiveData("")
    val minerFeeCrypto = MutableLiveData("")
}
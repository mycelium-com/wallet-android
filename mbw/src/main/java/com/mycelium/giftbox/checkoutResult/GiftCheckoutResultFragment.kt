package com.mycelium.giftbox.checkoutResult

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentGiftboxCheckoutResultBinding

class GiftCheckoutResultFragment : Fragment() {
    private lateinit var binding: FragmentGiftboxCheckoutResultBinding
    val args by navArgs<GiftCheckoutResultFragmentArgs>()

    val viewModel: GiftCheckoutResultFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGiftboxCheckoutResultBinding>(
            inflater,
            R.layout.fragment_giftbox_checkout_result,
            container,
            false
        )
            .apply {
                lifecycleOwner = this@GiftCheckoutResultFragment
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GitboxAPI.giftRepository.checkoutProduct(viewModel.viewModelScope,
            args.orderResponse.productCode!!,
            args.orderResponse.quantity?.toInt()!!,
            args.orderResponse.amount?.toInt()!!, "btc", success = {
//                findNavController().navigate(GiftboxSubmitFragmentDirections.toCheckoutResult(it!!))
                loader(false)
            }, error = { _, error ->
                ErrorHandler(requireContext()).handle(error)
                loader(false)
            }, finally = {
                loader(false)
            })
    }
}


class GiftCheckoutResultFragmentViewModel : ViewModel() {

}
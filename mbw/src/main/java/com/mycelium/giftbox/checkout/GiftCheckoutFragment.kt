package com.mycelium.giftbox.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.remote.Status
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxCheckoutBinding
import com.mycelium.wapi.wallet.coins.Value

class GiftCheckoutFragment : Fragment() {
    private lateinit var binding: FragmentGiftboxCheckoutBinding
    val args by navArgs<GiftCheckoutFragmentArgs>()

    val viewModel: GiftCheckoutFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGiftboxCheckoutBinding>(
            inflater,
            R.layout.fragment_giftbox_checkout,
            container,
            false
        )
            .apply {
                lifecycleOwner = this@GiftCheckoutFragment
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillProduct()
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    findNavController().navigate(GiftCheckoutFragmentDirections.toCheckoutResult(it.data!!))

                    loader(false)
                }
                Status.ERROR -> {
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }

        }
        viewModel.load(
            GiftCheckoutFragmentViewModel.Params(
                Constants.CLIENT_USER_ID,
                Constants.CLIENT_ORDER_ID,
                args.product.code!!,
                args.quantity,
                args.amount
            )
        )
    }

    private fun fillProduct() {
        with(binding) {
            val product = args.product
            tvTitle.text = product.name
            tvGiftCardAmount.text = args.amount.toStringWithUnit()
            tvExpire.text = product?.expiry_date_policy
            tvDiscount.text =
                """from ${product?.minimum_value} to ${product?.maximum_value}"""
        }
    }
}


class GiftCheckoutFragmentViewModel : ViewModel() {
    private val load = MutableLiveData<Params>()
    fun load(params: Params) {
        load.value = params
    }

    val loadSubsription = {
        load.switchMap {
            val (clientUserId, clientOrderId, code, quantity, amount) = it
            doRequest {
                return@doRequest GitboxAPI.giftRepository.api.checkoutProduct(
                    clientUserId,
                    clientOrderId,
                    code,
                    quantity,
                    amount.value.toInt()
                )
            }.asLiveData()
        }
    }

    data class Params(
        val clientUserId: String,
        val clientOrderId: String,
        val code: String,
        val quantity: Int = 1,
        val amount: Value
    )
}
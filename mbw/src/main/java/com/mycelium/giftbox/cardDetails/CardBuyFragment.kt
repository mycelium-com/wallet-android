package com.mycelium.giftbox.cardDetails

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
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.event.AmountListener
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxCardBuyBinding
import kotlinx.android.synthetic.main.giftcard_send_info.*
import kotlinx.coroutines.flow.onEach

class CardBuyFragment : Fragment(), AmountListener {
    private lateinit var binding: FragmentGiftboxCardBuyBinding
    val args by navArgs<CardBuyFragmentArgs>()

    val viewModel: CardDetailsFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGiftboxCardBuyBinding>(
            inflater,
            R.layout.fragment_giftbox_card_buy,
            container,
            false
        )
            .apply {
                lifecycleOwner = this@CardBuyFragment
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    val product = it.data?.product
                    with(binding) {
                        ivImage.loadImage(product?.card_image_url)
                        tvDescription.text = product?.description
                        tvCurrency.text = product?.currency_code
                        tvExpire.text = product?.expiry_date_policy
                        tvCountry.text = product?.countries?.joinToString(separator = ", ")
                        tvDiscount.text =
                            """from ${product?.minimum_value} to ${product?.maximum_value}"""

                        amountRoot.setOnClickListener {
//                            findNavController().navigate(CardBuyFragment)
                        }
                    }
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
            CardDetailsFragmentViewModel.Params(
                args.clientUserId,
                args.clientOrderId,
                args.productId
            )
        )
        binding.btSend.setOnClickListener {
            findNavController().navigate(CardBuyFragmentDirections.actionNext(viewModel.productResponse.value?.product!!, 100, 0))
        }
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
            }.onEach {
                it.data?.let {
                    productResponse.value = it
                }
            }.asLiveData()
        }
    }

    data class Params(val clientUserId: String, val clientOrderId: String, val productId: String)
}
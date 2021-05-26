package com.mycelium.giftbox.cardDetails

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.remote.Status
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxCardBuyBinding
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.giftcard_send_info.*
import kotlinx.coroutines.flow.onEach

class CardBuyFragment : Fragment() {
    private lateinit var binding: FragmentGiftboxCardBuyBinding
    val args by navArgs<CardBuyFragmentArgs>()

    val viewModel: CardDetailsFragmentViewModel by viewModels()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            intent?.getSerializableExtra(AmountInputFragment.AMOUNT_KEY)?.let {
                viewModel.amount.value = it as Value
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(receiver, IntentFilter(AmountInputFragment.ACTION_AMOUNT_SELECTED))
    }

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
        binding.btSend.isEnabled = viewModel.amount.value != null
        binding.tvAmount.text = viewModel.amount.value?.toStringWithUnit()
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
                            findNavController().navigate(
                                CardBuyFragmentDirections.enterAmount(
                                    product!!
                                )
                            )
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
            findNavController().navigate(
                CardBuyFragmentDirections.actionNext(
                    viewModel.productResponse.value?.product!!,
                    viewModel.amount.value?.value?.toInt() ?: 0,
                    1
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

}


class CardDetailsFragmentViewModel : ViewModel() {
    val amount = MutableLiveData<Value>()
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
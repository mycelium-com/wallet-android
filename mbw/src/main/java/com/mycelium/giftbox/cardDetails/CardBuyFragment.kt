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
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.giftbox.client.Constants
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentGiftboxCardBuyBinding
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.giftcard_send_info.*

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
        loader(true)
        GitboxAPI.giftRepository.getProduct(viewModel.viewModelScope,
            clientUserId = Constants.CLIENT_USER_ID,
            clientOrderId = Constants.CLIENT_ORDER_ID,
            productId = args.product.code!!, success = { productResponse ->
                viewModel.productResponse.value = productResponse
                val product = productResponse?.product
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
                                product!!, viewModel.amount.value
                            )
                        )
                    }
                }
            },
            error = { _, error ->
                ErrorHandler(requireContext()).handle(error)
                loader(false)
            }, finally = {
                loader(false)
            })

        binding.btSend.setOnClickListener {
            findNavController().navigate(
                CardBuyFragmentDirections.actionNext(
                    viewModel.productResponse.value?.product!!,
                    viewModel.amount.value!!,
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
}

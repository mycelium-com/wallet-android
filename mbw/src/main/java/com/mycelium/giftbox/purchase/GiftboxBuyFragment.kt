package com.mycelium.giftbox.purchase

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
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.loadImage
import com.mycelium.view.Denomination
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.util.zip2
import com.mycelium.wallet.databinding.FragmentGiftboxBuyBinding
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.*
import kotlinx.android.synthetic.main.giftcard_send_info.tvCountry
import kotlinx.android.synthetic.main.giftcard_send_info.tvExpire
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class GiftboxBuyFragment : Fragment() {
    private lateinit var binding: FragmentGiftboxBuyBinding
    val args by navArgs<GiftboxBuyFragmentArgs>()

    val viewModel: GiftboxBuyViewModel by viewModels()

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
        binding = DataBindingUtil.inflate<FragmentGiftboxBuyBinding>(
            inflater,
            R.layout.fragment_giftbox_buy,
            container,
            false
        )
            .apply {
                vm = viewModel
                lifecycleOwner = this@GiftboxBuyFragment
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btSend.isEnabled = viewModel.amount.value != null
        binding.tvAmount.text = viewModel.amount.value?.toStringWithUnit()
        viewModel.accountId.value = args.accountId

        loader(true)

        GitboxAPI.giftRepository.getProduct(viewModel.viewModelScope,
            productId = args.product.code!!, success = { productResponse ->
                viewModel.productResponse.value = productResponse
                val product = productResponse?.product
                with(binding) {
                    ivImage.loadImage(product?.cardImageUrl)
                    tvName.text = product?.name
                    tvExpire.text = product?.expiryDatePolicy
                    tvCountry.text = product?.countries?.joinToString(separator = ", ")
                    btMinusQuantity.setOnClickListener {
                        viewModel.quantity.value =
                            ((viewModel.quantity.value?.toInt() ?: 0) - 1).toString()
                    }
                    btPlusQuantity.setOnClickListener {
                        viewModel.quantity.value =
                            ((viewModel.quantity.value?.toInt() ?: 0) + 1).toString()
                    }
                    amountRoot.setOnClickListener {
                        findNavController().navigate(
                            GiftboxBuyFragmentDirections.enterAmount(
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
            GitboxAPI.giftRepository.createOrder(viewModel.viewModelScope,
                code = args.product.code!!,
                quantity = 1,
                amount = viewModel.amount.value?.valueAsBigDecimal?.toInt() ?: 0,
                currencyId = "btc", success = { orderResponse ->

//                    findNavController().navigate(
//                        GiftboxBuyFragmentDirections.actionNext(
//                            orderResponse!!
//                        )
//                    )
                },
                error = { _, error ->
                    ErrorHandler(requireContext()).handle(error)
                    loader(false)
                }, finally = {
                    loader(false)
                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }
}

class GiftboxBuyViewModel : ViewModel() {
    val accountId = MutableLiveData<UUID>()
    val amount = MutableLiveData<Value>()
    val quantity = MutableLiveData<String>()
    val productResponse = MutableLiveData<ProductResponse>()
    val priceResponse = MutableLiveData<PriceResponse>()
    val errorMessage: MutableLiveData<String> = MutableLiveData("")

    val totalAmountFiat: LiveData<PriceResponse> =
        Transformations.switchMap(
            zip2(
                amount,
                quantity.map { it.toInt() }) { amount: Value, quantity: Int ->
                Pair(
                    amount,
                    quantity
                )
            }) {
            callbackFlow {
                GitboxAPI.giftRepository.getPrice(viewModelScope,
                    code = productResponse.value?.product?.code ?: "",
                    quantity = quantity.value?.toIntOrNull() ?: 0,
                    amount = amount.value?.valueAsLong?.toInt() ?: 0,
                    currencyId = productResponse.value?.product?.currencyCode ?: "",
                    success = { priceResponse ->
                        errorMessage.value = ""
                        offer(priceResponse!!)
                    },
                    error = { _, error ->
                        errorMessage.value = error
                        close()
                    },
                    finally = {
                        close()
                    })
                awaitClose { }
            }.asLiveData()
        }


//    val totalAmountCryptoString: LiveData<String> = Transformations.map(totalAmountFiat) {
//        getAsCrypto(it).asString()
//    }

    //    val isGrantedPlus = Transformations.map(totalAmountFiat) {
//        return@map it.lessOrEqualThan(getAccountBalance().minus(getAsCrypto(amount.value!!)))
//    }
    val isGrantedMinus = Transformations.map(totalAmountFiat) {
        val valueOf = Value.valueOf(
            FiatType(productResponse.value?.priceCurrency!!),
            it.priceOffer?.toLong()!!
        )
        return@map valueOf.moreOrEqualThanZero()
    }
    val isGranted = Transformations.map(totalAmountFiat) {
        val valueOf = Value.valueOf(
            FiatType(productResponse.value?.priceCurrency!!),
            it.priceOffer?.toLong()!!
        )
        return@map valueOf.lessOrEqualThan(getAccountBalance())
    }

    private fun getAccountBalance(): Value {
        return MbwManager.getInstance(WalletApplication.getInstance()).getWalletManager(false)
            .getAccount(accountId.value!!)?.accountBalance?.confirmed!!
    }
//
//    private fun getAsCrypto(value: Value): Value {
//        return MbwManager.getInstance(WalletApplication.getInstance())
//            .exchangeRateManager.get(value, FiatType("BTC"))
//    }

    private fun Value.asString(): String = toStringWithUnit(Denomination.UNIT)


}

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
import com.mycelium.wallet.*
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.helper.FeeItemsBuilder
import com.mycelium.wallet.activity.send.model.FeeItem
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.util.zip2
import com.mycelium.wallet.databinding.FragmentGiftboxBuyBinding
import com.mycelium.wapi.api.lib.CurrencyCode
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.*
import kotlinx.android.synthetic.main.giftcard_send_info.tvCountry
import kotlinx.android.synthetic.main.giftcard_send_info.tvExpire
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.math.BigDecimal
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
        viewModel.accountId.value = args.accountId
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

    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val account by lazy {
        mbwManager.getWalletManager(false).getAccount(accountId.value!!)
    }
    private val feeItemsBuilder by lazy {
        FeeItemsBuilder(
            mbwManager.exchangeRateManager,
            mbwManager.getFiatCurrency(account?.coinType)
        )
    }
    private val feeEstimation by lazy {
        mbwManager.getFeeProvider(account?.basedOnCoinType).estimation
    }

    private fun getFeeItemList(): List<FeeItem> {
        return feeItemsBuilder.getFeeItemList(
            account!!.basedOnCoinType,
            feeEstimation, MinerFee.NORMAL, estimateTxSize()
        )
    }

    private fun getFeeItem(): FeeItem {
        return getFeeItemList()[0]
    }

    private fun estimateTxSize() = account!!.typicalEstimatedTransactionSize
    val totalAmountCrypto: LiveData<Value> =
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
                        offer(getBtcAmount(priceResponse!!))
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


    val totalAmountFiatString = Transformations.map(totalAmountCrypto) {
        val fiatValue = convert(it, Utils.getTypeByName(CurrencyCode.USD.shortString)!!)
        return@map fiatValue?.toStringWithUnit()
    }

    val totalAmountCryptoString = Transformations.map(totalAmountCrypto) {
        return@map "~" + it.toStringWithUnit()
    }

    private fun getBtcAmount(priceResponse: PriceResponse): Value {
        val cryptoUnit =
            BigDecimal(priceResponse.priceOffer).movePointRight(Utils.getBtcCoinType().unitExponent)
                .toBigInteger()
        return Value.valueOf(Utils.getBtcCoinType()!!, cryptoUnit)
    }

    val minerFeeFiat: MutableLiveData<String> by lazy { MutableLiveData(getFeeItem().fiatValue.toStringWithUnit()) }
    val minerFeeCrypto: MutableLiveData<String> by lazy { MutableLiveData("~"+getFeeItem().value.toStringWithUnit()) }

    //    val isGrantedPlus = Transformations.map(totalAmountCrypto) {
//        val cryptoValue = Value.valueOf(Utils.getBtcCoinType()!!,it.priceOffer!!)
//        return@map cryptoValue.lessOrEqualThan(getAccountBalance().minus())
//    }
    val isGrantedMinus = Transformations.map(totalAmountCrypto) {
        return@map it.moreOrEqualThanZero() && quantity.value?.toInt() ?: 0 > 0
    }
    val isGranted = Transformations.map(totalAmountCrypto) {
        return@map it.lessOrEqualThan(getAccountBalance())
    }

    private fun getAccountBalance(): Value {
        return account?.accountBalance?.confirmed!!
    }

    private fun convert(value: Value, assetInfo: AssetInfo): Value? =
        MbwManager.getInstance(WalletApplication.getInstance()).exchangeRateManager.get(
            value,
            assetInfo
        )
}

package com.mycelium.giftbox.purchase

import android.app.Activity
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
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.ProductResponse
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.*
import com.mycelium.wallet.Constants.TRANSACTION_ID_INTENT_KEY
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.helper.FeeItemsBuilder
import com.mycelium.wallet.activity.send.model.FeeItem
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.util.zip2
import com.mycelium.wallet.databinding.FragmentGiftboxBuyBinding
import com.mycelium.wapi.wallet.btc.BtcAddress
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

    val viewModel: GiftboxBuyViewModel by viewModels { ViewModelFactory(args.product) }

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
                    tvCardValueHeader.text =
                        "From " + product?.minimumValue + " to " + product?.maximumValue
                    tvCountry.text = product?.countries?.joinToString(separator = ", ")
                    btMinusQuantity.setOnClickListener {
                        viewModel.quantity.value = (viewModel.quantity.value?.toInt() ?: 0) - 1
                    }
                    btPlusQuantity.setOnClickListener {
                        viewModel.quantity.value = (viewModel.quantity.value?.toInt() ?: 0) + 1
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
            loader(true)
            GitboxAPI.giftRepository.createOrder(
                viewModel.viewModelScope,
                code = args.product.code!!,
                amount = viewModel.totalAmountFiat.value?.valueAsBigDecimal?.toInt()!!,
                quantity = viewModel.quantity.value!!,
                //TODO Do we need to hardcode this
                currencyId = "btc",//Utils.getBtcCoinType().symbol
                success = { orderResponse ->
                    viewModel.orderResponse.value = orderResponse
                    //TODO tBTC for debug for send test, do we need BTC instead?
                    val type = Utils.getBtcCoinType()
                    val address =
                        BtcAddress(type, BitcoinAddress.fromString(orderResponse?.payinAddress))
                    startActivityForResult(
                        SendCoinsActivity.getIntent(
                            requireActivity(),
                            viewModel.accountId.value!!,
                            viewModel.totalAmountCrypto.value?.valueAsLong!!,
                            address,
                            false
                        ), 101
                    )

                }, error = { _, error ->
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val txHash = data?.getStringExtra(TRANSACTION_ID_INTENT_KEY)
            findNavController().navigate(
                GiftboxBuyFragmentDirections.toResult(
                    args.accountId,
                    txHash!!,
                    viewModel.productResponse.value!!,
                    viewModel.totalAmountFiat.value!!,
                    viewModel.totalAmountCrypto.value!!,
                    viewModel.minerFeeFiat(),
                    viewModel.minerFeeCrypto(),
                    viewModel.orderResponse.value!!
                )
            )
        }
    }
}

class GiftboxBuyViewModel(val product: ProductInfo) : ViewModel() {
    val accountId = MutableLiveData<UUID>()
    val zeroFiatValue = zeroValue(product)
    val amount = MutableLiveData<Value>(zeroFiatValue)
    val orderResponse = MutableLiveData<OrderResponse>()
    val quantity = MutableLiveData(1)
    val productResponse = MutableLiveData<ProductResponse>()
    val errorMessage: MutableLiveData<String> = MutableLiveData("")
    val totalProgress = MutableLiveData<Boolean>(false)
    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val account by lazy {
        mbwManager.getWalletManager(false).getAccount(accountId.value!!)
    }

    val quantityString = Transformations.map(quantity) {
        it.toString()
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

    fun zeroValue(product: ProductInfo): Value {
        return Value.zeroValue(Utils.getTypeByName(product.currencyCode)!!)
    }

    private fun getFeeItem(): FeeItem {
        return getFeeItemList()[0]
    }

    private fun estimateTxSize() = account!!.typicalEstimatedTransactionSize
    val totalAmountCrypto: LiveData<Value> = totalAmountCrypto()
    val totalAmountCryptoSingle: LiveData<Value> = totalAmountCrypto(forSingleItem = true)
    val totalAmountCryptoSingleString = Transformations.map(totalAmountCryptoSingle) {
        it.toStringWithUnit()
    }

    private fun totalAmountCrypto(forSingleItem: Boolean = false) = Transformations.switchMap(
        zip2(
            amount,
            quantity.map { if (forSingleItem) 1 else it.toInt() }) { amount: Value, quantity: Int ->
            Pair(
                amount,
                quantity
            )
        }) {
        callbackFlow {
            val (amount, quantity) = it
            if (!forSingleItem) {
                totalAmountFiat.value = amount.times(quantity.toLong())
            }
            totalProgress.value = true
            GitboxAPI.giftRepository.getPrice(viewModelScope,
                code = productResponse.value?.product?.code ?: "",
                quantity = quantity,
                amount = amount.valueAsBigDecimal.toInt(),
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
                    totalProgress.value = false
                })
            awaitClose { }
        }.asLiveData()
    }

    val totalAmountFiat = MutableLiveData<Value>()
    val totalAmountFiatString = Transformations.map(totalAmountFiat) {
        return@map it?.toStringWithUnit()
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

    val minerFeeFiatString: MutableLiveData<String> by lazy { MutableLiveData(minerFeeFiat().toStringWithUnit()) }
    fun minerFeeFiat(): Value {
        return convert(minerFeeCrypto(), zeroFiatValue.type) ?: zeroFiatValue
    }

    val minerFeeCryptoString: MutableLiveData<String> by lazy { MutableLiveData("~" + minerFeeCrypto().toStringWithUnit()) }
    fun minerFeeCrypto() = getFeeItem().value

    val isGrantedPlus =
        Transformations.map(
            zip2(
                totalAmountCrypto,
                totalAmountCryptoSingle
            ) { total: Value, single: Value ->
                Pair(total, single)
            }) {
            val (total, single) = it
            total.plus(single).lessOrEqualThan(getAccountBalance())
        }

    val isGrantedMinus = Transformations.map(quantity) {
        return@map it > 1
    }
    val isGranted = Transformations.map(totalAmountCrypto) {
        return@map it.lessOrEqualThan(getAccountBalance())
    }

    private fun convert(value: Value, assetInfo: AssetInfo): Value? =
        MbwManager.getInstance(WalletApplication.getInstance()).exchangeRateManager.get(
            value,
            assetInfo
        )

    private fun getAccountBalance(): Value {
        return account?.accountBalance?.confirmed!!
    }
}

class ViewModelFactory(param: ProductInfo) :
    ViewModelProvider.Factory {
    private val mParam: ProductInfo = param
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GiftboxBuyViewModel(mParam) as T
    }

}
package com.mycelium.giftbox.purchase

import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.OrderResponse
import com.mycelium.giftbox.client.models.PriceResponse
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.getCardValue
import com.mycelium.giftbox.common.OrderHeaderViewModel
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.purchase.adapter.CustomSimpleAdapter
import com.mycelium.wallet.*
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.*
import com.mycelium.wallet.databinding.FragmentGiftboxBuyBinding
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import kotlinx.android.synthetic.main.fragment_giftbox_buy.*
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.*
import kotlinx.android.synthetic.main.giftcard_send_info.tvCountry
import kotlinx.android.synthetic.main.giftcard_send_info.tvExpire
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*


class GiftboxBuyFragment : Fragment() {
    private var binding: FragmentGiftboxBuyBinding? = null
    private val args by navArgs<GiftboxBuyFragmentArgs>()

    val viewModel: GiftboxBuyViewModel by viewModels { ViewModelFactory(args.product) }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            intent?.getSerializableExtra(AmountInputFragment.AMOUNT_KEY)?.let {
                viewModel.totalAmountFiatSingle.value = it as Value
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
    ): View =
        FragmentGiftboxBuyBinding.inflate(inflater).apply {
            binding = this
            vm = viewModel
            lifecycleOwner = this@GiftboxBuyFragment
        }.root

    val preselectedClickListener: (View) -> Unit = {
        showChoicePreselectedValuesDialog()
    }

    val defaultClickListener: (View) -> Unit = {
        findNavController().navigate(
            GiftboxBuyFragmentDirections.enterAmount(
                args.product,
                viewModel.totalAmountFiatSingle.value,
                viewModel.quantityInt.value!!,
                args.accountId
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.btSend?.isEnabled = viewModel.totalAmountFiatSingle.value != null
        viewModel.totalAmountFiatSingle.value = viewModel.totalAmountFiatSingle.value

        loader(true)

        if (args.product.availableDenominations != null) {
            btEnterAmount.isVisible = false
            btEnterAmountPreselected.isVisible = true
            btEnterAmountPreselected.background = null
            val isNotSetYet =
                viewModel.totalAmountFiatSingle.value == null || viewModel.totalAmountFiatSingle.value?.isZero() ?: true
            if (isNotSetYet && viewModel.getPreseletedValues().isNotEmpty()) {
                viewModel.totalAmountFiatSingle.value = viewModel.getPreseletedValues()[0]
            }
            btEnterAmountPreselected.setOnClickListener(preselectedClickListener)
        } else {
            btEnterAmountPreselected.isVisible = false
        }

        viewModel.errorQuantityMessage.observe(viewLifecycleOwner) {
            binding?.tlQuanity?.error = it
            val isError = !it.isNullOrEmpty()
            binding?.tvQuanity?.setTextColor(
                ContextCompat.getColor(
                    requireContext(), if (isError) R.color.red_error else R.color.white
                )
            )
            amountRoot.isEnabled = !isError
            btEnterAmountPreselected.isEnabled = !isError
            btEnterAmount.isEnabled = !isError
        }

        GitboxAPI.giftRepository.getProduct(viewModel.viewModelScope,
            productId = args.product.code!!, success = { productResponse ->
                val product = productResponse?.product
                with(binding) {
                    ivImage.loadImage(product?.cardImageUrl)
                    tvName.text = product?.name
                    tvQuantityLabel.isVisible = false
                    tvQuantity.isVisible = false
                    tvCardValueHeader.text = product?.getCardValue()
                    tvExpire.text =
                        if (product?.expiryInMonths != null) "${product.expiryDatePolicy} (${product.expiryInMonths} months)" else "Does not expire"

                    tvCountry.text = product?.countries?.mapNotNull {
                        CountriesSource.countryModels.find { model ->
                            model.acronym.equals(
                                it,
                                true
                            )
                        }
                    }?.joinToString { it.name }

                    btMinusQuantity.setOnClickListener {
                        if (viewModel.isGrantedMinus.value!!) {
                            viewModel.quantityString.value =
                                ((viewModel.quantityInt.value ?: 0) - 1).toString()
                        } else {

                        }
                    }
                    btPlusQuantity.setOnClickListener {
                        if (viewModel.isGrantedPlus.value!!) {
                            viewModel.quantityString.value =
                                ((viewModel.quantityInt.value ?: 0) + 1).toString()
                        }
                    }
                    if (args.product.availableDenominations == null) {
                        amountRoot.setOnClickListener(defaultClickListener)
                    } else {
                        amountRoot.setOnClickListener(preselectedClickListener)
                    }
                }
            },
            error = { _, error ->
                ErrorHandler(requireContext()).handle(error)
                loader(false)
            }, finally = {
                loader(false)
            })

        binding?.btSend?.setOnClickListener {
            loader(true)
            GitboxAPI.giftRepository.createOrder(
                viewModel.viewModelScope,
                code = args.product.code!!,
                amount = viewModel.totalAmountFiat.value?.valueAsBigDecimal?.toInt()!!,
                quantity = viewModel.quantityString.value?.toInt()!!,
                currencyId = viewModel.zeroCryptoValue?.currencySymbol?.removePrefix("t")!!,
                success = { orderResponse ->
                    viewModel.orderResponse.value = orderResponse
                    loader(true)
                    viewModel.sendTransactionAction.value = Unit
                }, error = { _, error ->
                    ErrorHandler(requireContext()).handle(error)
                    loader(false)
                }, finally = {
                    loader(false)
                })

            viewModel.sendTransaction.observe(viewLifecycleOwner) {
                loader(false)
                val (transaction, broadcastResult) = it
                broadcastResult(transaction, broadcastResult)
            }
        }
    }


    private fun showChoicePreselectedValuesDialog(
    ) {
        val preselectedList = viewModel.getPreseletedValues()
        val preselectedValue = viewModel.totalAmountFiatSingle.value
        val selectedIndex = if (preselectedValue != null) {
            preselectedList.indexOfFirst { it.equalsTo(preselectedValue) }
        } else -1
        val valueAndEnableMap =
            preselectedList.associateWith { it.lessOrEqualThan(viewModel.maxSpendableAmount.value!!) }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_card_value_dialog)
            .setSingleChoiceItems(
                CustomSimpleAdapter(requireContext(), valueAndEnableMap),
                selectedIndex
            )
            { dialog, which ->
                val candidateToSelectIsOk = valueAndEnableMap[preselectedList[which]]
                if (candidateToSelectIsOk == true) {
                    viewModel.totalAmountFiatSingle.value = preselectedList[which]
                    dialog.dismiss()
                } else {
                    Toaster(requireContext()).toast(R.string.gift_insufficient_funds, true)
                }
            }
            .create().show()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    fun broadcastResult(transaction: Transaction, broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            findNavController().navigate(
                GiftboxBuyFragmentDirections.toResult(
                    args.accountId,
                    transaction,
                    viewModel.productInfo,
                    viewModel.totalAmountFiat.value!!,
                    viewModel.totalAmountCrypto.value!!,
                    viewModel.minerFeeFiat(),
                    viewModel.minerFeeCrypto(),
                    viewModel.orderResponse.value!!
                )
            )
        } else {
            Toaster(requireActivity()).toast(broadcastResult.errorMessage, false)
        }
    }


}

class GiftboxBuyViewModel(val productInfo: ProductInfo) : ViewModel(), OrderHeaderViewModel {
    val gson = Gson()
    val MAX_QUANTITY = 19
    val accountId = MutableLiveData<UUID>()
    val zeroFiatValue = zeroFiatValue(productInfo)
    val orderResponse = MutableLiveData<OrderResponse>()
    val errorQuantityMessage: MutableLiveData<String> = MutableLiveData("")
    val totalProgress = MutableLiveData<Boolean>(false)
    val lastPriceResponse = MutableLiveData<PriceResponse>()
    private val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    val account by lazy {
        mbwManager.getWalletManager(false).getAccount(accountId.value!!)
    }
    val zeroCryptoValue by lazy {
        account?.basedOnCoinType?.value(0)
    }

    fun getPreseletedValues(): List<Value> {
        return productInfo.availableDenominations?.map {
            Value.valueOf(getAssetInfo(), toUnits(zeroFiatValue.type, it))
        }?.sortedBy { it.value } ?: listOf()
    }


    override val productName = MutableLiveData("")
    override val expire = MutableLiveData("")
    override val country = MutableLiveData("")
    override val cardValue = MutableLiveData("")
    override val quantity = MutableLiveData(0)

    val sendTransactionAction = MutableLiveData<Unit>()
    val sendTransaction = Transformations.switchMap(sendTransactionAction) {
        callbackFlow<Pair<Transaction, BroadcastResult>> {
            try {
                val address = when (account) {
                    is EthAccount -> {
                        EthAddress(Utils.getEthCoinType(), orderResponse.value!!.payinAddress!!)
                    }
                    is AbstractBtcAccount -> {
                        BtcAddress(
                            Utils.getBtcCoinType(),
                            BitcoinAddress.fromString(orderResponse.value!!.payinAddress)
                        )
                    }
                    else -> TODO("Account not supported yet")
                }
                val price = orderResponse.value?.amountExpectedFrom!!
                val createTx = account?.createTx(
                    address, getCryptoAmount(price),
                    FeePerKbFee(feeEstimation.normal), null
                )
                account?.signTx(createTx, AesKeyCipher.defaultKeyCipher())
                offer(createTx!! as Transaction to account!!.broadcastTx(createTx))
                close()
            } catch (ex: Exception) {
                cancel(CancellationException("Tx", ex))
            }
        }.asLiveData(IO)
    }

    val hasDenominations = productInfo.availableDenominations.isNullOrEmpty().not()
    val quantityString: MutableLiveData<String> = MutableLiveData("1")
    val quantityInt = Transformations.map(quantityString) {
        if (it.isDigitsOnly() && !it.isNullOrBlank()) it.toInt() else 1
    }

    private val feeEstimation by lazy {
        mbwManager.getFeeProvider(account?.basedOnCoinType).estimation
    }

    fun zeroFiatValue(product: ProductInfo): Value {
        return Value.zeroValue(Utils.getTypeByName(product.currencyCode)!!)
    }

    val totalAmountFiatSingle = MutableLiveData<Value>(zeroFiatValue)
    val totalAmountFiatSingleString = Transformations.map(totalAmountFiatSingle) {
        it.toStringFriendlyWithUnit()
    }

    val totalAmountCrypto: LiveData<Value> = totalAmountCrypto()
    val totalAmountCryptoSingle: LiveData<Value> = totalAmountCrypto(forSingleItem = true)
    val totalAmountCryptoSingleString = Transformations.map(totalAmountCryptoSingle) {
        it.toStringFriendlyWithUnit()
    }

    private fun totalAmountCrypto(forSingleItem: Boolean = false) = Transformations.switchMap(
        zip2(
            totalAmountFiatSingle,
            quantityInt
                .debounce(300)
                .map { if (forSingleItem) 1 else it.toInt() }) { amount: Value, quantity: Int ->
            Pair(
                amount,
                quantity
            )
        }) {
        callbackFlow {
            val (amount, quantity) = it
            if (quantity == 0 || amount.isZero()) {
                offer(zeroCryptoValue!!)
                return@callbackFlow
            }
            if (!forSingleItem) {
                totalAmountFiat.value = amount.times(quantity.toLong())
            }
            if (quantity > MAX_QUANTITY) {
                errorQuantityMessage.value = "Max available cards: $MAX_QUANTITY cards"
                return@callbackFlow
            } else {
                if (!forSingleItem) {
                    errorQuantityMessage.value = ""
                }
            }
            totalProgress.value = true
            GitboxAPI.giftRepository.getPrice(viewModelScope,
                code = productInfo?.code ?: "",
                quantity = quantity,
                amount = amount.valueAsBigDecimal.toInt(),
                currencyId = zeroCryptoValue!!.currencySymbol.removePrefix("t"),
                success = { priceResponse ->
                    if (priceResponse!!.status == PriceResponse.Status.eRROR) {
                        return@getPrice
                    }
                    lastPriceResponse.value = priceResponse
                    offer(getCryptoAmount(priceResponse))
                },
                error = { _, error ->
                    close()
                },
                finally = {
                    close()
                    totalProgress.value = false
                })
            awaitClose { }
        }.asLiveData()
    }

    val errorAmountMessage: LiveData<String> = Transformations.map(totalAmountCrypto) {
        val enough = it.lessOrEqualThan(getMaxSpendable())
        return@map if (enough) "" else WalletApplication.getInstance()
            .getString(R.string.gift_insufficient_funds)
    }
    val totalAmountFiat = MutableLiveData<Value>(zeroFiatValue)
    val totalAmountFiatString = Transformations.map(totalAmountFiat) {
        return@map it?.toStringFriendlyWithUnit()
    }

    val totalAmountCryptoString = Transformations.map(totalAmountCrypto) {
        return@map "~" + it.toStringFriendlyWithUnit()
    }

    private fun getCryptoAmount(price: PriceResponse): Value = getCryptoAmount(price.priceOffer!!)

    private fun getCryptoAmount(price: String): Value {
        val cryptoUnit = BigDecimal(price).movePointRight(account?.basedOnCoinType?.unitExponent!!)
            .toBigInteger()
        return Value.valueOf(account?.basedOnCoinType!!, cryptoUnit)
    }

    fun getAssetInfo() = Utils.getTypeByName(productInfo.currencyCode)!!

    val minerFeeFiatString: MutableLiveData<String> by lazy {
        val value = minerFeeFiat()
        val asString = if (value.lessThan(Value(value.type, 1.toBigInteger()))) {
            "<0.01 " + value.type.symbol
        } else value.toStringFriendlyWithUnit()
        MutableLiveData(asString)
    }

    fun minerFeeFiat(): Value {
        return convertToFiat(minerFeeCrypto()) ?: zeroFiatValue
    }

    val maxSpendableAmount: MutableLiveData<Value> by lazy { MutableLiveData(maxFiatSpendableAmount()) }
    fun maxFiatSpendableAmount(): Value {
        return convertToFiat(getMaxSpendable()) ?: zeroFiatValue
    }

    private fun getMaxSpendable() = mbwManager.getWalletManager(false)
        .getAccount(accountId.value!!)?.accountBalance?.spendable!!

    val minerFeeCryptoString: MutableLiveData<String> by lazy { MutableLiveData("~" + minerFeeCrypto().toStringFriendlyWithUnit()) }
    fun minerFeeCrypto() = feeEstimation.normal

    val isGrantedPlus =
        Transformations.map(
            zip4(
                totalAmountCrypto,
                totalAmountCryptoSingle,
                errorQuantityMessage,
                totalProgress
            ) { total: Value, single: Value, quantityError: String, progress: Boolean ->
                Quad(total, single, quantityError, progress)
            }
        ) {
            val (total, single, quantityError, progress) = it
            total.plus(single)
                .lessOrEqualThan(getAccountBalance()) && quantityError.isNullOrEmpty() && !progress
        }

    val isGrantedMinus = Transformations.map(quantityInt.debounce(300)) {
        return@map it > 1
    }

    val isGranted = Transformations.map(
        zip2(
            totalAmountCrypto,
            totalProgress
        ) { total: Value, progress: Boolean -> Pair(total, progress) }) {
        val (total, progress) = it
        return@map total.lessOrEqualThan(getAccountBalance()) && total.moreThanZero() && !progress
    }

    val plusBackground = Transformations.map(isGrantedPlus) {
        ContextCompat.getDrawable(
            WalletApplication.getInstance(),
            if (!it) R.drawable.ic_plus_disabled else R.drawable.ic_plus
        )
    }

    val minusBackground = Transformations.map(isGrantedMinus) {
        ContextCompat.getDrawable(
            WalletApplication.getInstance(),
            if (!it) R.drawable.ic_minus_disabled else R.drawable.ic_minus
        )
    }

    private fun convertToFiat(value: Value): Value? {
        lastPriceResponse.value?.exchangeRate?.let {
            val fiat = value.valueAsBigDecimal.multiply(BigDecimal(it))
            return Value.valueOf(zeroFiatValue.type, toUnits(zeroFiatValue.type, fiat))
        }
        return null
    }

    private fun getAccountBalance(): Value {
        return account?.accountBalance?.spendable!!
    }

    //colors
    val totalAmountSingleCryptoColor = Transformations.map(totalAmountCryptoSingle) {
        getColorByCryptoValue(it)
    }

    val totalAmountCryptoColor = Transformations.map(totalAmountCrypto) {
        getColorByCryptoValue(it)
    }

    val minerFeeCryptoColor by lazy { MutableLiveData(getColorByCryptoValue(minerFeeCrypto())) }

    val totalAmountFiatColor = Transformations.map(totalAmountFiat) {
        getColorByFiatValue(it)
    }

    val totalAmountFiatSingleColor = Transformations.map(totalAmountFiatSingle) {
        getColorByFiatValue(it)
    }

    val minerFeeFiatColor by lazy {
        val value = minerFeeFiat()
        MutableLiveData(
            ContextCompat.getColor(
                WalletApplication.getInstance(),
                if (value.moreOrEqualThanZero()) R.color.white else R.color.darkgrey
            )
        )
    }

    private fun toUnits(assetInfo: AssetInfo, amount: BigDecimal): BigInteger =
        amount.movePointRight(assetInfo.unitExponent).setScale(0, RoundingMode.HALF_UP)
            .toBigIntegerExact()

    private fun getColorByCryptoValue(it: Value) =
        ContextCompat.getColor(
            WalletApplication.getInstance(),
            if (it.moreThanZero()) R.color.white_alpha_0_6 else R.color.darkgrey
        )

    private fun getColorByFiatValue(it: Value) =
        ContextCompat.getColor(
            WalletApplication.getInstance(),
            if (it.moreThanZero()) R.color.white else R.color.darkgrey
        )


}

data class ErrorMessage(val message: String)

class ViewModelFactory(param: ProductInfo) :
    ViewModelProvider.Factory {
    private val mParam: ProductInfo = param
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GiftboxBuyViewModel(mParam) as T
    }

}
package com.mycelium.giftbox.purchase

import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.giftbox.client.models.getCardValue
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.purchase.viewmodel.GiftboxBuyViewModel.Companion.MAX_QUANTITY
import com.mycelium.giftbox.purchase.adapter.CustomSimpleAdapter
import com.mycelium.giftbox.purchase.viewmodel.GiftboxBuyViewModel
import com.mycelium.wallet.*
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.*
import com.mycelium.wallet.databinding.FragmentGiftboxBuyBinding
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.fragment_giftbox_buy.*
import kotlinx.android.synthetic.main.fragment_giftbox_details_header.*
import kotlinx.android.synthetic.main.giftcard_send_info.tvCountry
import kotlinx.android.synthetic.main.giftcard_send_info.tvExpire
import kotlinx.coroutines.*
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

        viewModel.warningQuantityMessage.observe(viewLifecycleOwner) {
            binding?.tlQuanity?.error = it
            val isError = !it.isNullOrEmpty()
            binding?.tvQuanity?.setTextColor(
                ContextCompat.getColor(
                    requireContext(), if (isError) R.color.red_error else R.color.white
                )
            )
        }

        loader(true)
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
                        }
                    }
                    btPlusQuantity.setOnClickListener {
                        if (viewModel.isGrantedPlus.value!!) {
                            viewModel.quantityString.value =
                                ((viewModel.quantityInt.value ?: 0) + 1).toString()
                        } else {
                            if (viewModel.quantityInt.value!! >= MAX_QUANTITY) {
                                viewModel.warningQuantityMessage.value =
                                    "Max available cards: $MAX_QUANTITY cards"
                            } else if (viewModel.totalProgress.value != true) {
                                viewModel.warningQuantityMessage.value =
                                    getString(R.string.gift_insufficient_funds)
                            }
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
            MbwManager.getInstance(WalletApplication.getInstance()).runPinProtectedFunction(activity) {
                loader(true)
                GitboxAPI.giftRepository.createOrder(
                        viewModel.viewModelScope,
                        code = args.product.code!!,
                        amount = (viewModel.totalAmountFiatSingle.value?.valueAsLong?.div(100))?.toInt()!!,
                        quantity = viewModel.quantityString.value?.toInt()!!,
                        currencyId = viewModel.zeroCryptoValue?.currencySymbol?.removePrefix("t")!!,
                        success = { orderResponse ->
                            viewModel.orderResponse.value = orderResponse
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

    fun broadcastResult(transaction: Transaction?, broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            findNavController().navigate(
                GiftboxBuyFragmentDirections.toResult(
                    args.accountId,
                    transaction!!,
                    viewModel.productInfo,
                    viewModel.totalAmountFiat.value!!,
                    viewModel.totalAmountCrypto.value!!,
                    viewModel.minerFeeFiat.value,
                    viewModel.minerFeeCrypto.value,
                    viewModel.orderResponse.value!!
                )
            )
        } else {
            Toaster(requireActivity())
                .toast(broadcastResult.errorMessage ?: broadcastResult.resultType.toString(), false)
        }
    }
}

class ViewModelFactory(param: ProductInfo) :
    ViewModelProvider.Factory {
    private val mParam: ProductInfo = param
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return GiftboxBuyViewModel(mParam) as T
    }
}
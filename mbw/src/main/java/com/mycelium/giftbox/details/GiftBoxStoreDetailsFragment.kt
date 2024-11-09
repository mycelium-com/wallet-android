package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.request.RequestOptions
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.CurrencyInfos
import com.mycelium.giftbox.details.viewmodel.GiftBoxStoreDetailsViewModel
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.setupDescription
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletConfiguration
import com.mycelium.wallet.databinding.FragmentGiftboxStoreDetailsBinding
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import java.util.*

class GiftBoxStoreDetailsFragment : Fragment() {
    private var binding: FragmentGiftboxStoreDetailsBinding? = null
    private val args by navArgs<GiftBoxStoreDetailsFragmentArgs>()
    private val viewModel: GiftBoxStoreDetailsViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = FragmentGiftboxStoreDetailsBinding.inflate(inflater)
            .apply {
                binding = this
                lifecycleOwner = this@GiftBoxStoreDetailsFragment
                model = this@GiftBoxStoreDetailsFragment.viewModel
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.title = args.mcproduct.name
        binding?.btSend?.setOnClickListener {
            viewModel.productInfo.value?.let {
                findNavController().navigate(GiftBoxStoreDetailsFragmentDirections.actionNext(it))
//                        it,
//                        CurrencyInfos().apply {
//                            addAll(viewModel.currencies!!)
//                        }))
            }
        }
        val descriptionClick: (View) -> Unit = {
            viewModel.more.value = !(viewModel.more.value ?: false)
            binding?.layoutDescription?.tvDescription?.setupDescription(
                    viewModel.description.value ?: "",
                    viewModel.more.value ?: false) {
                viewModel.moreVisible.value = true
            }
        }
        binding?.layoutDescription?.more?.setOnClickListener(descriptionClick)
        binding?.layoutDescription?.redeem?.setOnClickListener {
            viewModel.productInfo.value?.let { productInto ->
//                findNavController().navigate(GiftBoxStoreDetailsFragmentDirections.actionRedeem(productInto))
            }
        }
        binding?.layoutDescription?.terms?.setOnClickListener {
//            openLink(viewModel.productInfo.value?.termsAndConditionsPdfUrl)
        }
        viewModel.description.observe(viewLifecycleOwner) {
            binding?.layoutDescription?.tvDescription?.setupDescription(it,
                    viewModel.more.value ?: false) {
                viewModel.moreVisible.value = true
            }
        }
        loadData()
    }

    private fun loadData() {
//        loader(true)
//        GitboxAPI.mcGiftRepository.checkoutProduct(viewModel.viewModelScope,
//                quantity = 1,
//                amount = args.mcproduct.minFaceValue,
//                code = args.mcproduct.code!!,
//                success = { checkout ->
        val checkout = args.mcproduct
//                    viewModel.currencies = arrayOf(Utils.getBtcCoinType())
                    if (BuildConfig.FLAVOR == "btctestnet") {
                        changeToTestnetAddresses()
                    }
                    viewModel.setProduct(checkout)
                    binding?.ivImage?.loadImage(checkout?.cardImageUrl,
                            RequestOptions()
                                    .error(DefaultCardDrawable(resources, args.mcproduct.name ?: "")))
//                },
//                error = { _, error ->
//                    binding?.ivImage?.setImageDrawable(DefaultCardDrawable(resources, args.product.name ?: ""))
//                    ErrorHandler(requireContext()).handle(error)
//                },
//                finally = {
//                    loader(false)
//                })
    }

    private fun changeToTestnetAddresses() {
        val currencies = viewModel.currencies?.clone() ?: return
        val tokensWithTestnetAddress = WalletConfiguration.TOKENS.filter { it.testnetAddress != null }
        val intersectedCurrencies = currencies.filter { currencyInfo ->
            currencyInfo.contractAddress != null &&
                    currencyInfo.contractAddress!!.toLowerCase(Locale.US) in tokensWithTestnetAddress.map { it.prodAddress.toLowerCase(Locale.US) }
        }
        intersectedCurrencies.forEach { currencyInfo ->
            val testnetAddr = tokensWithTestnetAddress.first { it.prodAddress.equals(currencyInfo.contractAddress!!, ignoreCase = true) }.testnetAddress
            currencyInfo.contractAddress = testnetAddr
        }
        viewModel.currencies = currencies
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

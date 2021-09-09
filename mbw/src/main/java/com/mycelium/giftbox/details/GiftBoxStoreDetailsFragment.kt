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
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.CurrencyInfos
import com.mycelium.giftbox.details.viewmodel.GiftBoxStoreDetailsViewModel
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.setupDescription
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.FragmentGiftboxStoreDetailsBinding
import kotlinx.android.synthetic.main.giftcard_send_info.*

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
        (activity as AppCompatActivity).supportActionBar?.title = args.product.name
        binding?.btSend?.setOnClickListener {
            viewModel.productInfo.value?.let {
                findNavController().navigate(GiftBoxStoreDetailsFragmentDirections.actionNext(
                        it,
                        CurrencyInfos().apply {
                            addAll(viewModel.currencies!!)
                        }))
            }
        }
        val descriptionClick: (View) -> Unit = {
            viewModel.more.value = !(viewModel.more.value ?: false)
            binding?.layoutDescription?.tvDescription?.setupDescription(
                    viewModel.description.value ?: "",
                    viewModel.more.value ?: false)
        }
        binding?.layoutDescription?.more?.setOnClickListener(descriptionClick)
        binding?.layoutDescription?.less?.setOnClickListener(descriptionClick)
        binding?.layoutDescription?.redeem?.setOnClickListener {
            viewModel.productInfo.value?.let {
                findNavController().navigate(GiftBoxStoreDetailsFragmentDirections.actionRedeem(it))
            }
        }
        binding?.layoutDescription?.terms?.setOnClickListener {
            Utils.openWebsite(requireContext(), viewModel.productInfo.value?.termsAndConditionsPdfUrl)
        }
        viewModel.description.observe(viewLifecycleOwner) {
            binding?.layoutDescription?.tvDescription?.setupDescription(it,
                    viewModel.more.value ?: false)
        }
        loadData()
    }

    private fun loadData() {
        loader(true)
        GitboxAPI.giftRepository.checkoutProduct(viewModel.viewModelScope,
                quantity = 1,
                amount = args.product.minimumValue.toInt(),
                code = args.product.code!!,
                success = { checkout ->
                    viewModel.currencies = checkout?.currencies
                    viewModel.setProduct(checkout?.product)
                    binding?.ivImage?.loadImage(checkout?.product?.cardImageUrl)
                },
                error = { _, error ->
                    ErrorHandler(requireContext()).handle(error)
                },
                finally = {
                    loader(false)
                })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}

package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.details.viewmodel.GiftBoxDetailsViewModel
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxDetailsBinding


class GiftBoxDetailsFragment : Fragment() {
    private var binding: FragmentGiftboxDetailsBinding? = null
    private val args by navArgs<GiftBoxDetailsFragmentArgs>()
    private val viewModel: GiftBoxDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxDetailsBinding.inflate(inflater).apply {
                binding = this
                this.viewModel = this@GiftBoxDetailsFragment.viewModel
                this.lifecycleOwner = this@GiftBoxDetailsFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.ivImage?.loadImage(args.order.product_img)
        (activity as AppCompatActivity).supportActionBar?.title = args.order.name
        loadOrder()
        loadProduct()
    }

    private fun loadProduct() {
        GitboxAPI.giftRepository.getProduct(lifecycleScope, args.order.client_order_id!!, args.order.product_code!!, {
            viewModel.setProduct(it!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun loadOrder() {
        loader(true)
        GitboxAPI.giftRepository.getOrder(lifecycleScope, args.order, {
            viewModel.setOrder(it!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        }, {
            loader(false)
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
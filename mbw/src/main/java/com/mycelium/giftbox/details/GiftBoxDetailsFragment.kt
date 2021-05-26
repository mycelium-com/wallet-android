package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.ivImage?.loadImage(args.order.product_img)
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
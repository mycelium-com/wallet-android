package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.remote.Status
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
        viewModel.loadSubsription().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    loader(false)
                    viewModel.setOrder(it.data!!)
                }
                Status.ERROR -> {
                    Toaster(this).toast(it.error?.localizedMessage ?: "", true)
                    loader(false)
                }
                Status.LOADING -> {
                    loader(true)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
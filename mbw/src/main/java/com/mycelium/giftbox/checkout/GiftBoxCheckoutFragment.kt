package com.mycelium.giftbox.checkout

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.details.viewmodel.GiftBoxDetailsViewModel
import com.mycelium.giftbox.loadImage
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxDetailsBinding


class GiftBoxCheckoutFragment : Fragment() {
    private var binding: FragmentGiftboxDetailsBinding? = null
    private val args by navArgs<GiftBoxCheckoutFragmentArgs>()
    private val viewModel: GiftBoxDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentGiftboxDetailsBinding.inflate(inflater).apply {
                binding = this
                this.viewModel = this@GiftBoxCheckoutFragment.viewModel
                this.lifecycleOwner = this@GiftBoxCheckoutFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val order = args.order
        binding?.ivImage?.loadImage(order.productImg)
        (activity as AppCompatActivity).supportActionBar?.title = order.productName
        loadOrder()
        loadProduct()
    }

    private fun loadProduct() {
        GitboxAPI.giftRepository.getProduct(lifecycleScope, args.order.productCode!!, {
            viewModel.setProduct(it!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }

    private fun loadOrder() {
        loader(true)
        GitboxAPI.giftRepository.getOrder(scope = lifecycleScope, success =  {
            viewModel.setOrder(it!!)
        }, error = { _, msg ->
            Toaster(this).toast(msg, true)
        }, finally = {
            loader(false)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.share -> {
                    //TODO fill share text
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_SUBJECT, "")
                            .putExtra(Intent.EXTRA_TEXT, "")
                            .setType("text/plain"), "share gift card"))
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(requireContext(), R.style.MyceliumModern_Dialog)
                            .setTitle("Delete gift card?")
                            .setMessage("Are you sure you want to delete this gift card?")
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .setPositiveButton(R.string.delete) { _, _ -> }
                            .create().show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
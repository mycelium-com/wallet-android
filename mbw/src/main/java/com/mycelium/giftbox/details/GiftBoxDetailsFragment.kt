package com.mycelium.giftbox.details

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.*
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.details.viewmodel.GiftBoxDetailsViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxDetailsBinding
import com.mycelium.wallet.startCoroutineTimer

enum class MODE { STATUS, INFO }

class GiftBoxDetailsFragment : Fragment() {
    private var binding: FragmentGiftboxDetailsBinding? = null
    private val args by navArgs<GiftBoxDetailsFragmentArgs>()
    private val viewModel: GiftBoxDetailsViewModel by viewModels()
    private val repeatMillis: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View =
            FragmentGiftboxDetailsBinding.inflate(inflater).apply {
                binding = this
                this.viewModel = this@GiftBoxDetailsFragment.viewModel
                this.lifecycleOwner = this@GiftBoxDetailsFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.ivImage?.loadImage(args.order.productImg)
        (activity as AppCompatActivity).supportActionBar?.title = args.order.productName
        val descriptionClick: (View) -> Unit = {
            viewModel.more.value = !(viewModel.more.value ?: false)
            binding?.layoutDescription?.tvDescription?.setupDescription(
                    viewModel.description.value ?: "",
                    viewModel.more.value ?: false)
        }
        binding?.layoutDescription?.more?.setOnClickListener(descriptionClick)
        binding?.layoutDescription?.less?.setOnClickListener(descriptionClick)
        binding?.layoutDescription?.redeem?.setOnClickListener {
            findNavController().navigate(GiftBoxDetailsFragmentDirections.actionRedeem(viewModel.productInfo!!))
        }
        binding?.layoutDescription?.terms?.setOnClickListener {
            Utils.openWebsite(requireContext(), viewModel.productInfo?.termsAndConditionsPdfUrl)
        }
        binding?.share?.setOnClickListener {
            shareGiftcard(viewModel.orderResponse!!)
        }
        binding?.layoutCode?.redeemCode?.setOnClickListener {
            Utils.setClipboardString(viewModel.redeemCode.value, it.context)
        }
        binding?.layoutCode?.pinCode?.setOnClickListener {
            Utils.setClipboardString(viewModel.pinCode.value, it.context)
        }
        args.order.items?.firstOrNull()?.let {
            viewModel.setCodes(it)
        }
        viewModel.description.observe(viewLifecycleOwner) {
            binding?.layoutDescription?.tvDescription?.setupDescription(it,
                    viewModel.more.value ?: false)
        }
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
        when (args.mode) {
            MODE.STATUS -> {
                startCoroutineTimer(
                        scope = this.lifecycleScope,
                        delayMillis = 0,
                        repeatMillis = repeatMillis
                ) {
                    load(true)
                }
            }
            MODE.INFO -> {
                load(false)
            }
        }
    }

    private fun load(showStatus: Boolean = false) {
        loader(true)
        GitboxAPI.giftRepository.getOrder(lifecycleScope, args.order.clientOrderId!!, {
            if (showStatus) {
                when (it?.status) {
                    Status.sUCCESS -> setTitle("Success")
                    Status.eRROR -> setTitle("Error")
                    Status.pROCESSING -> setTitle("Processing")
                    null -> {
                    }
                }
            }
            viewModel.setOrder(it!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        }, {
            loader(false)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.share -> {
                    shareGiftcard(viewModel.orderResponse!!)
                    true
                }
                R.id.delete -> {
                    AlertDialog.Builder(requireContext(), R.style.MyceliumModern_Dialog)
                            .setTitle(getString(R.string.delete_gift_card))
                            .setMessage(getString(R.string.delete_gift_card_msg))
                            .setNegativeButton(R.string.button_cancel) { _, _ -> }
                            .setPositiveButton(R.string.delete) { _, _ ->
                                GiftboxPreference.remove(args.order)
                                findNavController().popBackStack()
                            }
                            .create().show()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    fun setTitle(title: String) {
        (activity as AppCompatActivity?)!!.supportActionBar!!.title = title
    }


}
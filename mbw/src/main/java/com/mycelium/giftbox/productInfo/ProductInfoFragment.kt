package com.mycelium.giftbox.productInfo

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.client.GitboxAPI
import com.mycelium.giftbox.loadImage
import com.mycelium.giftbox.productInfo.viewmodel.ProductInfoViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.databinding.FragmentGiftboxDetailsBinding
import com.mycelium.wallet.databinding.FragmentGiftboxProductInfoBinding

class ProductInfoFragment : Fragment() {
    private var binding: FragmentGiftboxProductInfoBinding? = null
    private val args by navArgs<ProductInfoFragmentArgs>()
    private val viewModel: ProductInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentGiftboxProductInfoBinding.inflate(inflater).apply {
            binding = this
            this.viewModel = this@ProductInfoFragment.viewModel
            this.lifecycleOwner = this@ProductInfoFragment
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.ivImage?.loadImage(args.productInfo.cardImageUrl)
        (activity as AppCompatActivity).supportActionBar?.title = args.productInfo.name

        binding?.btProceed?.setOnClickListener {
            findNavController().navigate(ProductInfoFragmentDirections.toSelectAccount(args.productInfo))
        }
        loadProduct()
    }

    private fun loadProduct() {
        GitboxAPI.giftRepository.getProduct(lifecycleScope, args.productInfo.code!!, {
            viewModel.setProduct(it!!)
        }, { _, msg ->
            Toaster(this).toast(msg, true)
        })
    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.giftbox_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.share -> {
                //TODO fill share text
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_SUBJECT, "")
                            .putExtra(Intent.EXTRA_TEXT, "")
                            .setType("text/plain"), "share gift card"
                    )
                )
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

    fun setTitle(title: String) {
        (activity as AppCompatActivity?)!!.supportActionBar!!.title = title
    }
}
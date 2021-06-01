package com.mycelium.giftbox.checkout

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.navArgs
import com.mrd.bitlib.model.BitcoinAddress
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.SendCoinsActivity.Companion.getIntent
import com.mycelium.wallet.databinding.FragmentGiftboxSubmitBinding
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.coins.Value
import java.text.SimpleDateFormat
import java.util.*

class GiftboxSubmitFragment : Fragment() {
    private lateinit var binding: FragmentGiftboxSubmitBinding
    val args by navArgs<GiftboxSubmitFragmentArgs>()

    val sdf by lazy {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        sdf
    }
    val viewModel: GiftCheckoutFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentGiftboxSubmitBinding>(
            inflater,
            R.layout.fragment_giftbox_submit,
            container,
            false
        )
            .apply {
                lifecycleOwner = this@GiftboxSubmitFragment
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillProduct()
        binding.btBuy.setOnClickListener {
            startBuyActivity()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            GiftboxSubmitFragmentDirections.toCheckoutResult(args.orderResponse, args.accountId)
        } else if (resultCode == Activity.RESULT_CANCELED) {

        }

    }

    private fun fillProduct() {
        with(binding) {
            tvTitle.text = args.orderResponse.productName
            tvGiftCardAmount.text = args.orderResponse.amount
            tvExpire.text = sdf.format(args.orderResponse.payTill)
//            args.orderResponse.currencyFromInfo?.
//            tvDiscount.text =
//                """from ${product?.minimumValue} to ${product?.maximumValue}"""
        }
    }

    fun startBuyActivity() {
        val value =
            Value.parse(Utils.getBtcCoinType(), args.orderResponse.amountExpectedFrom.toString())
        val address = BtcAddress(
            BitcoinMain.get(),
            BitcoinAddress.fromString(args.orderResponse.payinAddress)
        )

        startActivity(
            getIntent(requireActivity(), args.accountId, value.valueAsLong, address, false)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        )
    }
}


class GiftCheckoutFragmentViewModel : ViewModel() {
}
package com.mycelium.bequant.withdraw

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.withdraw.viewmodel.WithdrawAddressViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.ScanActivity
import com.mycelium.wallet.activity.StringHandlerActivity
import com.mycelium.wallet.activity.util.getAddress
import com.mycelium.wallet.activity.util.getAssetUri
import com.mycelium.wallet.content.ResultType
import com.mycelium.wallet.content.StringHandleConfig
import com.mycelium.wallet.content.actions.AddressAction
import com.mycelium.wallet.content.actions.UriAction
import com.mycelium.wallet.databinding.FragmentBequantWithdrawAddressBinding
import kotlinx.android.synthetic.main.fragment_bequant_withdraw_address.*


class WithdrawAddressFragment : Fragment() {

    lateinit var viewModel: WithdrawAddressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WithdrawAddressViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantWithdrawAddressBinding>(inflater, R.layout.fragment_bequant_withdraw_address, container, false)
                    .apply {
                        viewModel = this@WithdrawAddressFragment.viewModel
                        lifecycleOwner = this@WithdrawAddressFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scanQR.setOnClickListener {
            val config = StringHandleConfig().apply {
                addressAction = AddressAction()
                bitcoinUriAction = UriAction()
            }
            ScanActivity.callMe(this, SCAN_REQUEST, config)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCAN_REQUEST) {
            handleScanResults(resultCode, data)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleScanResults(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            val error = data?.getStringExtra(StringHandlerActivity.RESULT_ERROR)
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        } else {
            when (data?.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY) as ResultType) {
                ResultType.ADDRESS -> {
//                    if (data.getAddress().coinType == getAccount().coinType) {
                    viewModel.address.value = data.getAddress().toString()
//                    } else {
//                        Toast.makeText(activity, context.getString(R.string.not_correct_address_type), Toast.LENGTH_LONG).show()
//                    }
                }
                ResultType.ASSET_URI -> {
                    val uri = data.getAssetUri()
//                    if (uri.address?.coinType == getAccount().coinType) {
                    viewModel.address.value = uri.address.toString()
                    if (uri.value != null && uri.value!!.isPositive()) {
                        viewModel.amount.value = uri.value?.valueAsBigDecimal.toString()
                    }
//                    } else {
//                        Toast.makeText(activity, context.getString(R.string.not_correct_address_type), Toast.LENGTH_LONG).show()
//                    }
                }
                else -> {
                    throw IllegalStateException("Unexpected result type from scan: " +
                            data.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY).toString())
                }
            }
        }
    }

    companion object {
        const val SCAN_REQUEST = 4
    }
}
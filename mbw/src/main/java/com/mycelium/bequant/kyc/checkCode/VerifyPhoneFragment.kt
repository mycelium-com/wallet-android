package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.remote.repositories.KYCRepository
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycVerifyPhoneBinding
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.activity_bequant_kyc_verify_phone.*

class VerifyPhoneFragment : Fragment(R.layout.activity_bequant_kyc_verify_phone) {

    lateinit var viewModel: VerifyPhoneViewModel

    //    val args:VerifyPhoneFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VerifyPhoneViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<ActivityBequantKycVerifyPhoneBinding>(inflater, R.layout.activity_bequant_kyc_verify_phone, container, false)
                    .apply {
                        viewModel = this@VerifyPhoneFragment.viewModel
                        lifecycleOwner = this@VerifyPhoneFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                checkCode(enteredText)
                return true
            }
        }
        tvTryAgain.setOnClickListener {
            resendCode()
        }
    }

    private fun resendCode() {
        loader(true)
        Api.kycRepository.mobileVerification(viewModel.viewModelScope) {
            loader(false)
            AlertDialog.Builder(requireContext())
                    .setMessage(it)
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                    }.show()
        }
    }

    private fun checkCode(code: String) {
        loader(true)
        Api.kycRepository.checkMobileVerification(viewModel.viewModelScope, code, {
            loader(false)
            findNavController().navigate(VerifyPhoneFragmentDirections.actionNext())
        }, {
            loader(false)
            showError()
        })
    }

    private fun showError() {
        otp_view.error = "Error code"
    }
}
package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.KYCRepository
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycVerifyPhoneBinding
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.activity_bequant_kyc_verify_phone.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                tryCheckCode(enteredText)
                return true
            }
        }
        tvResendVerificationCode.setOnClickListener {
            resendCode()
        }
    }

    private fun resendCode() {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
//                val postKycSaveMobilePhone = KYCApi.create().postKycSaveMobilePhone(args.phoneRequest)
//                if (postKycSaveMobilePhone.isSuccessful) {
//                } else {
//                    showError(postKycSaveMobilePhone.code())
//                }
            }
        }
    }

    private fun tryCheckCode(code: String) {
        loader(true)
        KYCRepository.repository.checkMobileVerification(viewModel.viewModelScope, code) {
            loader(false)
            findNavController().navigate(VerifyPhoneFragmentDirections.actionNext())
        }


//        viewModel.fillModel()?.let {
//            viewModel.viewModelScope.launch {
//                progress(true)
//                withContext(Dispatchers.IO) {
//                    val postKycCheckMobilePhone = KYCApi.create().postKycCheckMobilePhone(it)
//                    if (postKycCheckMobilePhone.isSuccessful) {
//                        goNext()
//                    } else {
//                        showError(postKycCheckMobilePhone.code())
//                    }
//                    progress(false)
//                }
//            }
//        }
    }

    private fun showError(code: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            otp_view.error = "Error code"
        }
    }
}
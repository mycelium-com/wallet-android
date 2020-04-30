package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.os.Handler
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
import androidx.navigation.NavArgs
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.kyc.inputPhone.InputPhoneFragmentDirections
import com.mycelium.bequant.remote.client.apis.KYCApi
import com.mycelium.bequant.remote.client.models.KycSaveMobilePhoneRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycVerifyPhoneBinding
import kotlinx.android.synthetic.main.activity_bequant_kyc_verify_phone.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyPhoneFragment : Fragment(R.layout.activity_bequant_kyc_verify_phone) {
    private val CODE_LENGHT: Int = 6
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

        //FOR DEMO
        Handler().postDelayed({
            pinCode.setText("555555")
        },2000)
        Handler().postDelayed({
           goNext()
        },4000)
        //

        pinCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (TextUtils.isDigitsOnly(text) && text.length == CODE_LENGHT) {
                    tryCheckCode()
                }
            }
        })

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

    private fun tryCheckCode() {
        viewModel.fillModel()?.let {
            viewModel.viewModelScope.launch {
                progress(true)
                withContext(Dispatchers.IO) {
                    val postKycCheckMobilePhone = KYCApi.create().postKycCheckMobilePhone(it)
                    if (postKycCheckMobilePhone.isSuccessful) {
                        goNext()
                    } else {
                        showError(postKycCheckMobilePhone.code())
                    }
                    progress(false)
                }
            }
        }
    }

    private fun showError(code: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            otp_view.error = "Error code"
        }
    }


    private fun progress(show: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {

        }
    }

    private fun goNext() {
        findNavController().navigate(R.id.action_phoneVerifyToStep1)
    }
}
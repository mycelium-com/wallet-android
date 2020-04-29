package com.mycelium.bequant.kyc.inputPhone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.kyc.checkCode.VerifyPhoneFragmentDirections
import com.mycelium.bequant.remote.client.apis.KYCApi
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycPhoneInputBinding
import kotlinx.android.synthetic.main.activity_bequant_kyc_phone_input.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InputPhoneFragment : Fragment(R.layout.activity_bequant_kyc_phone_input) {

    lateinit var viewModel: InputPhoneViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(InputPhoneViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<ActivityBequantKycPhoneInputBinding>(inflater, R.layout.activity_bequant_kyc_phone_input, container, false)
                    .apply {
                        viewModel = this@InputPhoneFragment.viewModel
                        lifecycleOwner = this@InputPhoneFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<View>(R.id.stepsPanel)?.visibility = View.VISIBLE

        btGetCode.setOnClickListener {
            sendCode()
        }
    }

    private fun sendCode() {
        tvErrorCode.visibility = View.GONE
        viewModel.getRequest()?.let {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val postKycSaveMobilePhone = KYCApi.create().postKycSaveMobilePhone(it)
                if (postKycSaveMobilePhone.isSuccessful) {
                    findNavController().navigate(InputPhoneFragmentDirections.actionPhoneInputToPhoneVerify(it))
                } else {
                    showError(postKycSaveMobilePhone.code())
                }
            }
        } ?: run {
            tvErrorCode.visibility = View.VISIBLE
        }

    }

    private fun showError(code: Int) {
        //show real error
        tvErrorCode.visibility = View.VISIBLE
    }
}
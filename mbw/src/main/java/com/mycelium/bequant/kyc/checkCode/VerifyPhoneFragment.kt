package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.remote.client.apis.KYCApi
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityBequantKycVerifyPhoneBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyPhoneFragment : Fragment(R.layout.activity_bequant_kyc_verify_phone) {
    lateinit var viewModel: VerifyPhoneViewModel
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
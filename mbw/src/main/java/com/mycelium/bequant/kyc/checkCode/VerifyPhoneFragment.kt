package com.mycelium.bequant.kyc.checkCode

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R

class VerifyPhoneFragment: Fragment(R.layout.activity_bequant_kyc_verify_phone){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler().postDelayed({
            findNavController().navigate(R.id.action_phoneVerifyToStep1)
        },5000)
    }
}
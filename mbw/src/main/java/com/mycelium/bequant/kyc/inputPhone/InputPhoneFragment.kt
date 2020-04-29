package com.mycelium.bequant.kyc.inputPhone

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_kyc_phone_input.*

class InputPhoneFragment : Fragment(R.layout.activity_bequant_kyc_phone_input) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = null
        btGetCode.setOnClickListener {
            findNavController().navigate(R.id.action_phoneInputToPhoneVerify)
        }
    }
}
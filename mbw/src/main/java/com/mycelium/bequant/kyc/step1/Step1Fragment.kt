package com.mycelium.bequant.kyc.step1

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_steps_2.*

class Step1Fragment: Fragment(R.layout.activity_bequant_steps_1){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<View>(R.id.stepsPanel)?.visibility = View.VISIBLE
        btNext.setOnClickListener {
            findNavController().navigate(R.id.action_step1ToStep2)
        }
    }
}
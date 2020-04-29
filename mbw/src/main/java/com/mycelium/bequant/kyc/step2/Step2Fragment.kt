package com.mycelium.bequant.kyc.step2

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_steps_2.*

class Step2Fragment : Fragment(R.layout.activity_bequant_steps_2) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btNext.setOnClickListener {
            findNavController().navigate(Step2FragmentDirections.actionNext())
        }
    }
}
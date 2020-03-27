package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_change_password.*


class ResetPasswordChangeFragment : Fragment(R.layout.fragment_bequant_change_password) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changePassword.setOnClickListener {
            requireActivity().finish()
            startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
        }
    }
}
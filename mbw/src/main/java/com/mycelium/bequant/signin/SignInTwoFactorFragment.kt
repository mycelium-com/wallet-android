package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignInTwoFactorFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pasteFromClipboard.setOnClickListener {
            requireActivity().finish()
            startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
        }
    }
}
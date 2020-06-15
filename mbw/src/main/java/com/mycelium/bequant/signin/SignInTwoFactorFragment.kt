package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.client.models.AccountAuthRequest
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignInTwoFactorFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {

    val args by navArgs<SignInTwoFactorFragmentArgs>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = args.auth
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_two_factor_auth)
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        pasteFromClipboard.setOnClickListener {
            pinCode.setText(Utils.getClipboardString(requireContext()))
        }
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                SignRepository.repository.authorize(this@SignInTwoFactorFragment, auth.copy(otpCode = enteredText), {
                    startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
                    requireActivity().finish()
                },{ _, _ ->
                })
                return true
            }
        }
    }
}
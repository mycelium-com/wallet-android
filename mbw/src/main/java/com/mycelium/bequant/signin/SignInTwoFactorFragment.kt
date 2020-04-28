package com.mycelium.bequant.signin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.model.Auth
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.poovam.pinedittextfield.PinField
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_two_factor.*


class SignInTwoFactorFragment : Fragment(R.layout.fragment_bequant_sign_in_two_factor) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = arguments?.getSerializable("auth") as Auth
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_two_factor_auth_)
        pasteFromClipboard.setOnClickListener {
            pinCode.setText(Utils.getClipboardString(requireContext()))
        }
        pinCode.onTextCompleteListener = object : PinField.OnTextCompleteListener {
            override fun onTextComplete(enteredText: String): Boolean {
                val loader = LoaderFragment()
                loader.show(parentFragmentManager, "loader")
                auth.otpCode = enteredText
                SignRepository.repository.authorize(auth, {
                    loader.dismissAllowingStateLoss()
                    startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
                    requireActivity().finish()
                }, { code, message ->
                    loader.dismissAllowingStateLoss()
                    ErrorHandler(requireContext()).handle(message)
                })
                return true
            }
        }
    }
}
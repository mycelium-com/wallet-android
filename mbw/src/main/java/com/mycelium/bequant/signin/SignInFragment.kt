package com.mycelium.bequant.signin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in.*


class SignInFragment : Fragment(R.layout.fragment_bequant_sign_in) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetPassword.setOnClickListener {
            // TODO change on navigator
            requireParentFragment().parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ResetPasswordFragment(), "reset_password")
                    .commitAllowingStateLoss()
        }
        signIn.setOnClickListener {
            // TODO change on navigator
            requireParentFragment().parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SignInTwoFactorFragment(), "sign_in_two_factor")
                    .commitAllowingStateLoss()
        }
    }
}
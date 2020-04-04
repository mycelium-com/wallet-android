package com.mycelium.bequant.signin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in.*


class SignInFragment : Fragment(R.layout.fragment_bequant_sign_in) {

    var resetPasswordListener: (() -> Unit)? = null
    var signListener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetPassword.setOnClickListener {
            resetPasswordListener?.invoke()
        }
        signIn.setOnClickListener {
            signListener?.invoke()
        }
    }
}
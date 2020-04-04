package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_up.*


class SignUpFragment : Fragment(R.layout.fragment_bequant_sign_up) {
    var registerListener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        register.setOnClickListener {
            registerListener?.invoke()
        }
    }
}
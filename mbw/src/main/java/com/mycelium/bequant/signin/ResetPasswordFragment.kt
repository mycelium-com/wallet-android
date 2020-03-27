package com.mycelium.bequant.signin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_sign_in_reset_password.*


class ResetPasswordFragment : Fragment(R.layout.fragment_bequant_sign_in_reset_password) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        submit.setOnClickListener {
            // TODO change on navigator
            parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ResetPasswordInfoFragment(), "reset_password_info")
                    .commitAllowingStateLoss()
        }
    }
}
package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_registration_info.*


class RegistrationInfoFragment : Fragment(R.layout.fragment_bequant_registration_info) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        next.setOnClickListener {
            // TODO change on navigator
            parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SignUpTwoFactorFragment(), "SignUpTwoFactorFragment")
                    .commitAllowingStateLoss()
        }
    }
}
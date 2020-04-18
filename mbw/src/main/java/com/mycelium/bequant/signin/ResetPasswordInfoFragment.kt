package com.mycelium.bequant.signin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_reset_password_info.*


class ResetPasswordInfoFragment : Fragment(R.layout.fragment_bequant_reset_password_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_reset_password)
        val mail = arguments?.getString("email") ?: ""
        email.text = mail
        next.setOnClickListener {
            findNavController().navigate(ResetPasswordInfoFragmentDirections.actionNext(mail))
        }
    }
}
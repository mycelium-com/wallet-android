package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.remote.model.Register
import com.mycelium.bequant.signup.viewmodel.RegistrationInfoViewModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_registration_info.*


class RegistrationInfoFragment : Fragment(R.layout.fragment_bequant_registration_info) {

    val viewModel = ViewModelProviders.of(this).get(RegistrationInfoViewModel::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val register = arguments?.getSerializable("register") as Register
        viewModel.setRegister(register)
        next.setOnClickListener {
            findNavController().navigate(RegistrationInfoFragmentDirections.actionNext())
        }
        supportTeam.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
    }
}
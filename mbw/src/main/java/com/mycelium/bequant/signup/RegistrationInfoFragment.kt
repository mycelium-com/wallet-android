package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.model.Email
import com.mycelium.bequant.remote.model.Register
import com.mycelium.bequant.signup.viewmodel.RegistrationInfoViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantRegistrationInfoBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_registration_info.*
import kotlinx.android.synthetic.main.part_bequant_not_receive_email.*


class RegistrationInfoFragment : Fragment() {

    lateinit var viewModel: RegistrationInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RegistrationInfoViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantRegistrationInfoBindingImpl>(inflater, R.layout.fragment_bequant_registration_info, container, false)
                    .apply {
                        viewModel = this@RegistrationInfoFragment.viewModel
                        lifecycleOwner = this@RegistrationInfoFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val register = arguments?.getSerializable("register") as Register
        viewModel.setRegister(register)
        next.setOnClickListener {
            val loader = LoaderFragment()
            loader.show(parentFragmentManager, "loader")
            SignRepository.repository.totpConfirm({
                loader.dismissAllowingStateLoss()
                findNavController().navigate(RegistrationInfoFragmentDirections.actionNext())
            }, {
                loader.dismissAllowingStateLoss()
            })
        }
        resendConfirmationEmail.setOnClickListener {
            SignRepository.repository.resendRegister(Email(register.email), {}, {})
        }
        supportTeam.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
    }
}
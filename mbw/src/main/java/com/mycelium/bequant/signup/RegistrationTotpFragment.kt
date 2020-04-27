package com.mycelium.bequant.signup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.signup.viewmodel.RegistrationInfoViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantRegistrationInfoBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_registration_totp.*
import kotlinx.android.synthetic.main.part_bequant_not_receive_email.*


class RegistrationTotpFragment : Fragment() {

    lateinit var viewModel: RegistrationInfoViewModel

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            next.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RegistrationInfoViewModel::class.java)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(Constants.ACTION_BEQUANT_EMAIL_CONFIRMED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantRegistrationInfoBindingImpl>(inflater, R.layout.fragment_bequant_registration_totp, container, false)
                    .apply {
                        viewModel = this@RegistrationTotpFragment.viewModel
                        lifecycleOwner = this@RegistrationTotpFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.verify_via_email)
        viewModel.email.value = BequantPreference.getEmail()
        next.isEnabled = false
        next.setOnClickListener {
            requireActivity().finish()
            startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
        }
        resendConfirmationEmail.setOnClickListener {
//            SignRepository.repository.resendRegister(Email(register.email), {}, {})
        }
        supportTeam.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_SUPPORT_CENTER)))
        }
    }

}
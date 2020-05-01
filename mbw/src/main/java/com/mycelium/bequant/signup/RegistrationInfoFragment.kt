package com.mycelium.bequant.signup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.Constants.LINK_SUPPORT_CENTER
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

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            findNavController().navigate(RegistrationInfoFragmentDirections.actionFinish())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(RegistrationInfoViewModel::class.java)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(ACTION_BEQUANT_EMAIL_CONFIRMED))
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
        (activity as AppCompatActivity?)?.supportActionBar?.title = "Registration"
        (activity as AppCompatActivity?)?.supportActionBar?.setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        resendConfirmationEmail.setOnClickListener {
            SignRepository.repository.resendRegister(Email(register.email), {}, {})
        }
        supportTeam.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    findNavController().navigate(RegistrationInfoFragmentDirections.actionFinish())
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
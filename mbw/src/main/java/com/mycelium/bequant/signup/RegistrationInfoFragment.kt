package com.mycelium.bequant.signup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.BequantConstants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.BequantConstants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.client.models.AccountAuthRequest
import com.mycelium.bequant.remote.client.models.AccountEmailConfirmResend
import com.mycelium.bequant.remote.model.BequantUserEvent
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.signup.viewmodel.RegistrationInfoViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantRegistrationInfoBinding


class RegistrationInfoFragment : Fragment() {
    val args by navArgs<RegistrationInfoFragmentArgs>()
    val viewModel: RegistrationInfoViewModel by viewModels()
    var binding: FragmentBequantRegistrationInfoBinding? = null

    private val emailConfirmedReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            loader(true)
            val request = AccountAuthRequest(args.register.email, args.register.password)
            Api.signRepository.authorize(lifecycleScope, request, success = {
                startActivity(Intent(requireContext(), BequantMarketActivity::class.java)
                        .putExtra("from", "registration"))
                requireActivity().finish()
                BequantUserEvent.EMAIL_CONFIRMED.track()
            }, error = { _, message ->
                ErrorHandler(requireContext()).handle(message)
            }, finally = {
                loader(false)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(emailConfirmedReceiver, IntentFilter(ACTION_BEQUANT_EMAIL_CONFIRMED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantRegistrationInfoBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@RegistrationInfoFragment.viewModel
                        lifecycleOwner = this@RegistrationInfoFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setRegister(args.register)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.registration)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        binding?.layoutNotReceive?.resendConfirmationEmail?.setOnClickListener {
            loader(true)
            Api.signRepository.resendRegister(lifecycleScope, AccountEmailConfirmResend(args.register.email),
                    success = { /* Email was sent. */ },
                    error = { _, message ->
                        ErrorHandler(requireContext()).handle(message)
                    }, finally = {
                loader(false)
            })
        }
        binding?.layoutNotReceive?.supportTeam?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(emailConfirmedReceiver)
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
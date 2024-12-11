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
import android.view.View.GONE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.model.BequantUserEvent
import com.mycelium.bequant.signup.viewmodel.RegistrationInfoViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantRegistrationTotpBinding


class RegistrationTotpFragment : Fragment() {

    val viewModel: RegistrationInfoViewModel by viewModels()
    var binding: FragmentBequantRegistrationTotpBinding? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            startActivity(Intent(requireContext(), BequantMarketActivity::class.java)
                    .putExtra("from", "totp_registration"))
            requireActivity().finish()
            BequantUserEvent.TWO_FACTOR_SETUP_DONE.track()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, IntentFilter(BequantConstants.ACTION_BEQUANT_TOTP_CONFIRMED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentBequantRegistrationTotpBinding.inflate(inflater, container, false)
            .apply {
                binding = this
                viewModel = this@RegistrationTotpFragment.viewModel
                lifecycleOwner = this@RegistrationTotpFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.verify_via_email)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_clear))
        }
        viewModel.email.value = BequantPreference.getEmail()
        binding?.layoutNotReceive?.checkIsEmailCorrect?.visibility = GONE
        binding?.layoutNotReceive?.resendConfirmationEmail?.setOnClickListener {
//            Api.signRepository.resendRegister(Email(register.email), {}, {})
        }
        binding?.layoutNotReceive?.supportTeam?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    requireActivity().finish()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
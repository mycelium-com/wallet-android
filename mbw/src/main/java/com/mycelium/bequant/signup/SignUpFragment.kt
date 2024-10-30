package com.mycelium.bequant.signup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantConstants.ACTION_COUNTRY_SELECTED
import com.mycelium.bequant.BequantConstants.COUNTRY_MODEL_KEY
import com.mycelium.bequant.BequantConstants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.BequantConstants.LINK_TERMS_OF_USE
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.client.models.RegisterAccountRequest
import com.mycelium.bequant.remote.model.BequantUserEvent
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.sign.SignFragmentDirections
import com.mycelium.bequant.signup.viewmodel.SignUpViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignUpBinding


class SignUpFragment : Fragment() {
    val viewModel: SignUpViewModel by viewModels()
    private var binding: FragmentBequantSignUpBinding? = null

    private val countrySelectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            viewModel.country.value = intent?.getParcelableExtra<CountryModel>(COUNTRY_MODEL_KEY)?.name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                countrySelectedReceiver,
                IntentFilter(ACTION_COUNTRY_SELECTED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantSignUpBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        this.viewModel = this@SignUpFragment.viewModel
                        lifecycleOwner = this@SignUpFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.email.observe(viewLifecycleOwner, Observer {
            binding?.emailLayout?.error = null
        })
        viewModel.password.observe(viewLifecycleOwner, Observer { value ->
            binding?.layoutRegistration?.passwordLayout?.error = null
            viewModel.calculatePasswordLevel(value,
                binding?.layoutRegistration?.passwordLevel!!,
                binding?.layoutRegistration?.passwordLevelLabel!!)
        })
        viewModel.repeatPassword.observe(viewLifecycleOwner, Observer {
            binding?.layoutRegistration?.repeatPasswordLayout?.error = null
        })
        binding?.layoutRegistration?.password?.setOnFocusChangeListener { _, focus ->
            binding?.layoutRegistration?.passwordNote?.setTextColor(if (focus) Color.WHITE else Color.parseColor("#49505C"))
            if (focus) {
                viewModel.calculatePasswordLevel(viewModel.password.value
                        ?: "",
                    binding?.layoutRegistration?.passwordLevel!!,
                    binding?.layoutRegistration?.passwordLevelLabel!!)
            } else {
                viewModel.passwordLevelVisibility.value = GONE
            }
        }
        binding?.countrySelector?.setOnClickListener {
            findNavController().navigate(SignFragmentDirections.actionSelectCountry())
        }
        binding?.register?.setOnClickListener {
            if (validate()) {
                loader(true)
                val registerAccountRequest = RegisterAccountRequest(viewModel.email.value!!, viewModel.password.value!!)
                Api.signRepository.signUp(lifecycleScope, registerAccountRequest, success = {
                    findNavController().navigate(SignFragmentDirections.actionRegister(registerAccountRequest))
                    BequantUserEvent.REGISTRATION_COMPLETED.track()
                }, error = { _, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, finally = {
                    loader(false)
                })
            }
        }
        binding?.termsOfUse?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_TERMS_OF_USE)))
        }
        binding?.supportCenter?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
        binding?.iHaveRefCode?.setOnClickListener {
            binding?.referralLayout?.visibility = if (binding?.referralLayout?.visibility == VISIBLE) GONE else VISIBLE
            val chevron = if (binding?.referralLayout?.visibility == VISIBLE) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            binding?.iHaveRefCode?.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, chevron, 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    activity?.onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }


    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(countrySelectedReceiver)
        super.onDestroy()
    }

    private fun validate(): Boolean {
        if (viewModel.email.value?.isEmpty() != false) {
            binding?.emailLayout?.error = "Can't be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(viewModel.email.value ?: "").matches()) {
            binding?.emailLayout?.error = "Not email"
            return false
        }
        if (viewModel.password.value?.isEmpty() != false) {
            binding?.layoutRegistration?.passwordLayout?.error = "Can't be empty"
            return false
        }
        if (viewModel.password.value != viewModel.repeatPassword.value) {
            binding?.layoutRegistration?.repeatPasswordLayout?.error = "Passwords don't match"
            return false
        }
        return true
    }
}
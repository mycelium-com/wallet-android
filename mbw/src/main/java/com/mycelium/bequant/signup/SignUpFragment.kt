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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants.ACTION_COUNTRY_SELECTED
import com.mycelium.bequant.Constants.COUNTRY_MODEL_KEY
import com.mycelium.bequant.Constants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.Constants.LINK_TERMS_OF_USE
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.client.models.RegisterAccountRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.sign.SignFragmentDirections
import com.mycelium.bequant.signup.viewmodel.SignUpViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignUpBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_sign_up.*
import kotlinx.android.synthetic.main.layout_password_registration.*


class SignUpFragment : Fragment() {
    lateinit var viewModel: SignUpViewModel

    private val countrySelectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            viewModel.country.value = intent?.getParcelableExtra<CountryModel>(COUNTRY_MODEL_KEY)?.name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                countrySelectedReceiver,
                IntentFilter(ACTION_COUNTRY_SELECTED))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSignUpBindingImpl>(inflater, R.layout.fragment_bequant_sign_up, container, false)
                    .apply {
                        this.viewModel = this@SignUpFragment.viewModel
                        lifecycleOwner = this@SignUpFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.email.observe(this, Observer {
            emailLayout.error = null
        })
        viewModel.password.observe(this, Observer { value ->
            passwordLayout.error = null
            viewModel.calculatePasswordLevel(value, passwordLevel, passwordLevelLabel)
        })
        viewModel.repeatPassword.observe(this, Observer {
            repeatPasswordLayout.error = null
        })
        password.setOnFocusChangeListener { _, focus ->
            passwordNote.setTextColor(if (focus) Color.WHITE else Color.parseColor("#49505C"))
            if (focus) {
                viewModel.calculatePasswordLevel(viewModel.password.value
                        ?: "", passwordLevel, passwordLevelLabel)
            } else {
                viewModel.passwordLevelVisibility.value = GONE
            }
        }
        countrySelector.setOnClickListener {
            findNavController().navigate(SignFragmentDirections.actionSelectCountry())
        }
        register.setOnClickListener {
            if (validate()) {
                loader(true)
                val registerAccountRequest = RegisterAccountRequest(viewModel.email.value!!, viewModel.password.value!!)
                Api.signRepository.signUp(lifecycleScope, registerAccountRequest, success = {
                    findNavController().navigate(SignFragmentDirections.actionRegister(registerAccountRequest))
                }, error = { _, message ->
                    ErrorHandler(requireContext()).handle(message)
                }, finally = {
                    loader(false)
                })
            }
        }
        termsOfUse.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_TERMS_OF_USE)))
        }
        supportCenter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
        iHaveRefCode.setOnClickListener {
            referralLayout.visibility = if (referralLayout.visibility == VISIBLE) GONE else VISIBLE
            val chevron = if (referralLayout.visibility == VISIBLE) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            iHaveRefCode.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, chevron, 0)
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
            emailLayout.error = "Can't be empty"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(viewModel.email.value ?: "").matches()) {
            emailLayout.error = "Not email"
            return false
        }
        if (viewModel.password.value?.isEmpty() != false) {
            passwordLayout?.error = "Can't be empty"
            return false
        }
        if (viewModel.password.value != viewModel.repeatPassword.value) {
            repeatPasswordLayout?.error = "Passwords don't match"
            return false
        }
        return true
    }
}
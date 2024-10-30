package com.mycelium.bequant.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.BequantConstants.ACTION_BEQUANT_SHOW_REGISTER
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.client.models.AccountAuthRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.sign.SignFragmentDirections
import com.mycelium.bequant.signin.viewmodel.SignInViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignInBinding


class SignInFragment : Fragment() {

    val viewModel: SignInViewModel by viewModels()
    var binding: FragmentBequantSignInBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantSignInBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        this.viewModel = this@SignInFragment.viewModel
                        lifecycleOwner = this@SignInFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.email.observe(this, Observer { value ->
            binding?.emailLayout?.error = null
        })
        viewModel.password.observe(this, Observer { value ->
            binding?.passwordLayout?.error = null
        })
        binding?.resetPassword?.setOnClickListener {
            findNavController().navigate(SignFragmentDirections.actionResetPassword())
        }
        binding?.signIn?.setOnClickListener {
            if (validate()) {
                loader(true)
                val request = AccountAuthRequest(viewModel.email.value!!, viewModel.password.value!!)
                Api.signRepository.authorize(lifecycleScope, request, success = {
                    startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
                }, error = { code, message ->
                    if (code == 420) {
                        findNavController().navigate(SignFragmentDirections.actionSignIn(request))
                    } else {
                        ErrorHandler(requireContext()).handle(message)
                    }
                }, finally = {
                    loader(false)
                })
            }
        }
        binding?.register?.setOnClickListener {
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent(ACTION_BEQUANT_SHOW_REGISTER))
        }
        binding?.supportCenter?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BequantConstants.LINK_SUPPORT_CENTER)))
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
    private fun validate(): Boolean {
        if (viewModel.email.value?.isEmpty() != false) {
            binding?.emailLayout?.error = getString(R.string.bequant_email_empty_error)
            return false
        }
        if (viewModel.password.value?.isEmpty() != false) {
            binding?.passwordLayout?.error = getString(R.string.bequant_field_empty_error)
            return false
        }
        return true
    }
}
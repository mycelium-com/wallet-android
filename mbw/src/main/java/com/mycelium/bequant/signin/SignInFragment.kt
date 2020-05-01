package com.mycelium.bequant.signin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.Constants
import com.mycelium.bequant.Constants.ACTION_BEQUANT_SHOW_REGISTER
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.model.Auth
import com.mycelium.bequant.sign.SignFragmentDirections
import com.mycelium.bequant.signin.viewmodel.SignInViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignInBinding
import kotlinx.android.synthetic.main.fragment_bequant_sign_in.*


class SignInFragment : Fragment() {

    lateinit var viewModel: SignInViewModel
    var resetPasswordListener: (() -> Unit)? = null

    var totpSignUpListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(SignInViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSignInBinding>(inflater, R.layout.fragment_bequant_sign_in, container, false)
                    .apply {
                        this.viewModel = this@SignInFragment.viewModel
                        lifecycleOwner = this@SignInFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.email.observe(this, Observer { value ->
            emailLayout.error = null
        })
        viewModel.password.observe(this, Observer { value ->
            passwordLayout.error = null
        })
        resetPassword.setOnClickListener {
            findNavController().navigate(SignFragmentDirections.actionResetPassword())
        }
        signIn.setOnClickListener {
            if (validate()) {
                val auth = Auth(viewModel.email.value!!, viewModel.password.value!!)
                val loader = LoaderFragment()
                loader.show(parentFragmentManager, "loader")
                SignRepository.repository.authorize(auth, {
                    loader.dismissAllowingStateLoss()
                    findNavController().navigate(SignFragmentDirections.actionSignUp())
                }, { code, error ->
                    loader.dismissAllowingStateLoss()
                    if (code == 420) {
                        findNavController().navigate(SignFragmentDirections.actionSignIn(auth))
                    } else {
                        ErrorHandler(requireContext()).handle(error)
                    }
                })
            }
        }
        register.setOnClickListener {
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent(ACTION_BEQUANT_SHOW_REGISTER))
        }
        supportCenter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_SUPPORT_CENTER)))
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

    private fun validate(): Boolean {
        if (viewModel.email.value?.isEmpty() != false) {
            emailLayout.error = "Email shouldn't be empty"
            return false
        }
        if (viewModel.password.value?.isEmpty() != false) {
            passwordLayout?.error = "Can't be empty"
            return false
        }
        return true
    }
}
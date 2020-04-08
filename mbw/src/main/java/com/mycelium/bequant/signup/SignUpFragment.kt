package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.Constants.LINK_SUPPORT_CENTER
import com.mycelium.bequant.Constants.LINK_TERMS_OF_USER
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.model.Register
import com.mycelium.bequant.signup.viewmodel.SignUpViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignUpBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_sign_up.*


class SignUpFragment : Fragment(R.layout.fragment_bequant_sign_up) {

    lateinit var viewModel: SignUpViewModel
    var registerListener: ((Register) -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantSignUpBindingImpl>(inflater, R.layout.fragment_bequant_sign_up, container, false)
                    .apply {
                        this.viewModel = this@SignUpFragment.viewModel
                        lifecycleOwner = this@SignUpFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.email.observe(this, Observer { value ->
            emailLayout.error = null
        })
        viewModel.password.observe(this, Observer { value ->
            passwordLayout.error = null
            viewModel.passwordNoteVisibility.value = if (value.isEmpty()) VISIBLE else GONE
        })
        viewModel.repeatPassword.observe(this, Observer { value ->
            repeatPasswordLayout.error = null
        })
        register.setOnClickListener {
            if (validate()) {
                val register = Register(email.text.toString(), password.text.toString())
                val loader = LoaderFragment()
                loader.show(parentFragmentManager, "loader")
                SignRepository.repository.register(register, {
                    loader.dismissAllowingStateLoss()
                    registerListener?.invoke(register)
                }, {
                    ErrorHandler(requireContext()).handle()
                    loader.dismissAllowingStateLoss()
                })
            }
        }
        termsOfUse.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_TERMS_OF_USER)))
        }
        supportCenter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LINK_SUPPORT_CENTER)))
        }
        iHaveRefCode.setOnClickListener {
            referralLayout.visibility = if (referralLayout.visibility == VISIBLE) GONE else VISIBLE
        }
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
            repeatPasswordLayout?.error = "Can't be empty"
            return false
        }
        if (viewModel.password.value != viewModel.repeatPassword.value) {
            repeatPasswordLayout?.error = "Not correct"
            return false
        }
        return true
    }
}
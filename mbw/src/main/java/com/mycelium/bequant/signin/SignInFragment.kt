package com.mycelium.bequant.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.remote.model.Auth
import com.mycelium.bequant.signin.viewmodel.SignInViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantSignInBinding
import kotlinx.android.synthetic.main.fragment_bequant_sign_in.*


class SignInFragment : Fragment() {

    lateinit var viewModel: SignInViewModel
    var resetPasswordListener: (() -> Unit)? = null
    var signListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        resetPassword.setOnClickListener {
            resetPasswordListener?.invoke()
        }
        signIn.setOnClickListener {
            val auth = Auth(viewModel.email.value!!, viewModel.password.value!!, "", "")
            val loader = LoaderFragment()
            SignRepository.repository.authorize(auth, {
                loader.dismissAllowingStateLoss()
                signListener?.invoke()
            }, {
                loader.dismissAllowingStateLoss()
                signListener?.invoke()
//                ErrorHandler(requireContext()).handle()
            })
        }
    }
}
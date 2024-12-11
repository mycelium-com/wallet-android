package com.mycelium.bequant.signin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.client.models.AccountPasswordSetRequest
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.bequant.signup.viewmodel.SignUpViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantChangePasswordBinding


class ResetPasswordChangeFragment : Fragment() {

    val viewModel: SignUpViewModel by viewModels()

    val args by navArgs<ResetPasswordChangeFragmentArgs>()
    var binding: FragmentBequantChangePasswordBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentBequantChangePasswordBinding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        this.viewModel = this@ResetPasswordChangeFragment.viewModel
                        lifecycleOwner = this@ResetPasswordChangeFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.run {
            title = getString(R.string.bequant_page_title_reset_password)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_bequant_arrow_back))
        }
        val mail = args.email
        val token = args.token
        viewModel.email.value = mail
        viewModel.password.observe(this, Observer { value ->
            binding?.layoutPassword?.passwordLayout?.error = null
            viewModel.calculatePasswordLevel(value,
                binding?.layoutPassword?.passwordLevel!!,
                binding?.layoutPassword?.passwordLevelLabel!!)
        })
        viewModel.repeatPassword.observe(this, Observer { value ->
            binding?.layoutPassword?.repeatPasswordLayout?.error = null
        })
        binding?.layoutPassword?.password?.setOnFocusChangeListener { _, focus ->
            binding?.layoutPassword?.passwordNote?.setTextColor(if (focus) Color.WHITE else Color.parseColor("#49505C"))
            if (focus) {
                viewModel.calculatePasswordLevel(viewModel.password.value
                        ?: "",
                    binding?.layoutPassword?.passwordLevel!!,
                    binding?.layoutPassword?.passwordLevelLabel!!)
            } else {
                viewModel.passwordLevelVisibility.value = GONE
            }
        }
        binding?.changePassword?.setOnClickListener {
            val request = AccountPasswordSetRequest(viewModel.password.value!!, token)
            loader(true)
            Api.signRepository.resetPasswordSet(lifecycleScope, request, {
                findNavController().navigate(ResetPasswordChangeFragmentDirections.finish())
            }, error = { _, message ->
                ErrorHandler(requireContext()).handle(message)
            }, finally = {
                loader(false)
            })
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
}
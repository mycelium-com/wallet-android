package com.mycelium.bequant.signin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.bequant.Constants.LOADER_TAG
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.common.passwordLevel
import com.mycelium.bequant.market.BequantMarketActivity
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.bequant.signup.viewmodel.SignUpViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentBequantChangePasswordBindingImpl
import kotlinx.android.synthetic.main.fragment_bequant_change_password.*
import kotlinx.android.synthetic.main.layout_password_registration.*


class ResetPasswordChangeFragment : Fragment() {

    lateinit var viewModel: SignUpViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentBequantChangePasswordBindingImpl>(inflater, R.layout.fragment_bequant_change_password, container, false)
                    .apply {
                        this.viewModel = this@ResetPasswordChangeFragment.viewModel
                        lifecycleOwner = this@ResetPasswordChangeFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_reset_password)
        val mail = arguments?.getString("email") ?: ""
        val token = arguments?.getString("token") ?: ""
        viewModel.email.value = mail
        viewModel.password.observe(this, Observer { value ->
            passwordLayout.error = null
            calculatePasswordLevel(value)
        })
        viewModel.repeatPassword.observe(this, Observer { value ->
            repeatPasswordLayout.error = null
        })
        password.setOnFocusChangeListener { _, focus ->
            passwordNote.setTextColor(if (focus) Color.WHITE else Color.parseColor("#49505C"))
            if (focus) {
                calculatePasswordLevel(viewModel.password.value ?: "")
            } else {
                viewModel.passwordLevelVisibility.value = GONE
            }
        }
        changePassword.setOnClickListener {
            val loader = LoaderFragment()
            loader.show(parentFragmentManager, LOADER_TAG)
            SignRepository.repository.resetPasswordSet(token, viewModel.password.value!!, {
                loader.dismissAllowingStateLoss()
                requireActivity().finish()
                startActivity(Intent(requireContext(), BequantMarketActivity::class.java))
            }, {
                loader.dismissAllowingStateLoss()
                ErrorHandler(requireContext()).handle(it)
            })
        }
    }

    private fun calculatePasswordLevel(password: String) {
        val level = password.passwordLevel()
        viewModel.passwordNoteVisibility.value = if (level > 0) GONE else VISIBLE
        viewModel.passwordLevelVisibility.value = if (level > 0) VISIBLE else GONE
        passwordLevel.progress = level * 30
        passwordLevel.progressDrawable = resources.getDrawable(
                when (level) {
                    1 -> R.drawable.bequant_password_red_line
                    2 -> R.drawable.bequant_password_yellow_line
                    else -> R.drawable.bequant_password_green_line
                })
        viewModel.passwordLevelText.value = when (level) {
            1 -> getString(R.string.bequant_password_weak)
            2 -> getString(R.string.bequant_password_strong)
            else -> getString(R.string.bequant_password_very_strong)
        }
        passwordLevelLabel.setTextColor(resources.getColor(when (level) {
            1 -> R.color.bequant_password_red
            2 -> R.color.bequant_password_yellow
            else -> R.color.bequant_password_green
        }))
    }
}
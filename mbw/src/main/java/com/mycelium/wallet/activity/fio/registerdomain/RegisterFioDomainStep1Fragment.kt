package com.mycelium.wallet.activity.fio.registerdomain

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registerdomain.viewmodel.RegisterFioDomainViewModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRegisterFioDomainStep1BindingImpl
import kotlinx.android.synthetic.main.fragment_register_fio_domain_step1.*

class RegisterFioDomainStep1Fragment : Fragment() {
    private val viewModel: RegisterFioDomainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRegisterFioDomainStep1BindingImpl>(inflater, R.layout.fragment_register_fio_domain_step1, container, false)
                    .apply {
                        viewModel = this@RegisterFioDomainStep1Fragment.viewModel
                        lifecycleOwner = this@RegisterFioDomainStep1Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            tvFeeInfo.text = resources.getString(R.string.fio_annual_fee_domain, it.toStringWithUnit())
        })
        btNextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
        viewModel.isFioDomainValid.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.domain.value!!)
        })
        viewModel.isFioDomainAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.domain.value!!)
        })
        viewModel.isFioServiceAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.domain.value!!)
        })
        inputEditText.doOnTextChanged { text: CharSequence?, _, _, _ ->
            doAddressCheck(text.toString())
        }
    }

    private fun doAddressCheck(fioAddress: String) {
        btNextButton.isEnabled = viewModel.isFioDomainValid.value!! && viewModel.isFioDomainAvailable.value!!
                && viewModel.isFioServiceAvailable.value!! && inputEditText.text!!.isNotEmpty()
        setDefaults()
        if (fioAddress.isNotEmpty()) {
            if (!viewModel.isFioDomainValid.value!!) {
                showErrorOrSuccess(R.string.fio_domain_is_invalid, isError = true)
            } else if (!viewModel.isFioServiceAvailable.value!!) {
                showErrorOrSuccess(R.string.fio_address_check_service_unavailable, isError = true)
            } else if (!viewModel.isFioDomainAvailable.value!!) {
                showErrorOrSuccess(R.string.fio_domain_occupied, isError = true)
            } else {
                showErrorOrSuccess(R.string.fio_domain_available, isError = false)
            }
        }
    }

    private fun setDefaults() {
        tvHint.text = resources.getString(R.string.fio_create_domain_hint)
        tvHint.setTextColor(resources.getColor(R.color.fio_white_alpha_0_6))
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
        tvHint.setCompoundDrawables(null, null, null, null)
    }

    private fun showErrorOrSuccess(messageRes: Int, isError: Boolean) {
        val drawableRes = if (isError) R.drawable.ic_fio_name_error else R.drawable.ic_fio_name_ok
        val colorRes = if (isError) R.color.fio_red else R.color.fio_green

        tvHint.text = resources.getString(messageRes)
        tvHint.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(drawableRes), null, null, null)
        tvHint.compoundDrawablePadding = 3
        tvHint.setTextColor(resources.getColor(colorRes))
        tvHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.toFloat())
    }
}
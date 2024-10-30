package com.mycelium.wallet.activity.fio.registerdomain

import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registerdomain.viewmodel.RegisterFioDomainViewModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRegisterFioDomainStep1Binding
import java.util.*

class RegisterFioDomainStep1Fragment : Fragment() {
    private val viewModel: RegisterFioDomainViewModel by activityViewModels()
    var binding: FragmentRegisterFioDomainStep1Binding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentRegisterFioDomainStep1Binding.inflate(inflater, container, false)
                    .apply {
                        binding = this
                        viewModel = this@RegisterFioDomainStep1Fragment.viewModel
                        lifecycleOwner = this@RegisterFioDomainStep1Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            binding?.tvFeeInfo?.text = resources.getString(R.string.fio_annual_fee_domain, it.toStringWithUnit())
        })
        binding?.btNextButton?.setOnClickListener {
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
        binding?.inputEditText?.doOnTextChanged { text: CharSequence?, _, _, _ ->
            doAddressCheck(text.toString())
        }
        binding?.inputEditText?.filters = arrayOf<InputFilter>(
                object : InputFilter.AllCaps() {
                    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
                        return source.toString().toLowerCase(Locale.US)
                    }
                }
        )
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun doAddressCheck(fioAddress: String) {
        binding?.btNextButton?.isEnabled = viewModel.isFioDomainValid.value!! && viewModel.isFioDomainAvailable.value!!
                && viewModel.isFioServiceAvailable.value!! && binding?.inputEditText?.text?.isNotEmpty() == true
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
        binding?.tvHint?.text = resources.getString(R.string.fio_create_domain_hint)
        binding?.tvHint?.setTextColor(resources.getColor(R.color.fio_white_alpha_0_6))
        binding?.tvHint?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
        binding?.tvHint?.setCompoundDrawables(null, null, null, null)
    }

    private fun showErrorOrSuccess(messageRes: Int, isError: Boolean) {
        val drawableRes = if (isError) R.drawable.ic_fio_name_error else R.drawable.ic_fio_name_ok
        val colorRes = if (isError) R.color.fio_red else R.color.fio_green

        binding?.tvHint?.text = resources.getString(messageRes)
        binding?.tvHint?.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(drawableRes), null, null, null)
        binding?.tvHint?.compoundDrawablePadding = 3
        binding?.tvHint?.setTextColor(resources.getColor(colorRes))
        binding?.tvHint?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.toFloat())
    }
}
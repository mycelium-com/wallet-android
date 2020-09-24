package com.mycelium.wallet.activity.fio.registername

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRegisterFioNameStep1BindingImpl
import kotlinx.android.synthetic.main.fragment_register_fio_name_confirm.btNextButton
import kotlinx.android.synthetic.main.fragment_register_fio_name_step1.*


class RegisterFioNameStep1Fragment : Fragment() {
    private val viewModel: RegisterFioNameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRegisterFioNameStep1BindingImpl>(inflater, R.layout.fragment_register_fio_name_step1, container, false)
                    .apply {
                        viewModel = this@RegisterFioNameStep1Fragment.viewModel.apply {
                            spinner?.adapter = ArrayAdapter(context,
                                    R.layout.layout_send_coin_transaction_replace, R.id.text, listOf("@mycelium", "@secondoption", "Register FIO Domain")).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {}
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                    if (spinner.selectedItem.toString() != "Register FIO Domain") {
                                        viewModel!!.domain.value = spinner.selectedItem.toString()
                                    }
                                    Log.i("asdaf", "asdaf viewModel.domain.value: ${viewModel!!.domain.value}")
                                }
                            }
                        }
                        lifecycleOwner = this@RegisterFioNameStep1Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            tvFeeInfo.text = resources.getString(R.string.fio_annual_fee, it.toStringWithUnit())
        })
        btNextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
//        tvDomain.text = "@${viewModel.domain.value}"
        viewModel.isFioAddressValid.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        viewModel.isFioAddressAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        viewModel.isFioServiceAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        inputEditText.doOnTextChanged { _, _, _, _ ->
            doAddressCheck(viewModel.address.value!!)
        }
    }

    private fun doAddressCheck(fioAddress: String) {
        btNextButton.isEnabled = viewModel.isFioAddressValid.value!! && viewModel.isFioAddressAvailable.value!!
                && viewModel.isFioServiceAvailable.value!! && inputEditText.text!!.isNotEmpty()
        tvHint.text = ""
        tvHint.setTextColor(Color.WHITE)
        if (fioAddress.isNotEmpty()) {
            if (!viewModel.isFioAddressValid.value!!) {
                tvHint.text = resources.getString(R.string.fio_address_is_invalid)
                tvHint.setTextColor(Color.RED)
            } else if (!viewModel.isFioServiceAvailable.value!!) {
                tvHint.text = resources.getString(R.string.fio_address_check_service_unavailable)
                tvHint.setTextColor(Color.RED)
            } else if (!viewModel.isFioAddressAvailable.value!!) {
                tvHint.text = resources.getString(R.string.fio_address_occupied)
                tvHint.setTextColor(Color.RED)
            } else {
                tvHint.text = resources.getString(R.string.fio_address_available)
                tvHint.setTextColor(Color.GREEN)
            }
        }
    }
}

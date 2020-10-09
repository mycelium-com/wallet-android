package com.mycelium.wallet.activity.fio.registername

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.text.Spanned
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.registerdomain.RegisterFIODomainActivity
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel.Companion.DEFAULT_DOMAIN
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRegisterFioNameStep1BindingImpl
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.FioModule
import kotlinx.android.synthetic.main.fragment_register_fio_name_confirm.btNextButton
import kotlinx.android.synthetic.main.fragment_register_fio_name_step1.*
import java.util.*


class RegisterFioNameStep1Fragment : Fragment() {
    private val viewModel: RegisterFioNameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRegisterFioNameStep1BindingImpl>(inflater, R.layout.fragment_register_fio_name_step1, container, false)
                    .apply {
                        viewModel = this@RegisterFioNameStep1Fragment.viewModel.apply {
                            val domains = getDomains()
                            val spinnerItems: MutableList<CharSequence> = mutableListOf()
                            spinnerItems.addAll(domains.map { "@${it.domain}" })
                            spinnerItems.add("Register FIO Domain")
                            spinner?.adapter = DomainsAdapter(requireContext(), spinnerItems).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {}
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                                    if (spinner.selectedItem.toString() != "Register FIO Domain") {
                                        viewModel!!.domain.value = domains[p2]
                                        Log.i("asdaf", "asdaf viewModel.domain.value: ${viewModel!!.domain.value}")
                                    } else {
                                        startActivity(Intent(requireActivity(), RegisterFIODomainActivity::class.java))
                                        // to prevent "Register FIO Domain" being set as spinner selected value
                                        spinner.setSelection((spinner.adapter as ArrayAdapter<String>).getPosition(
                                                "@${viewModel!!.domain.value!!.domain}"))
                                    }
                                }
                            }
                        }
                        lifecycleOwner = this@RegisterFioNameStep1Fragment
                    }.root

    private fun getDomains(): List<FIODomain> {
        val fioModule = (MbwManager.getInstance(context).getWalletManager(false).getModuleById(FioModule.ID) as FioModule)
        val domains: MutableList<FIODomain> = mutableListOf()
        domains.add(DEFAULT_DOMAIN)
        domains.addAll(fioModule.getAllRegisteredFioDomains())
        return domains
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinner.setSelection((spinner.adapter as ArrayAdapter<String>).getPosition("@${viewModel.domain.value!!.domain}"))
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            tvFeeInfo.text = resources.getString(R.string.fio_annual_fee, it.toStringWithUnit())
        })
        btNextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
        viewModel.isFioAddressValid.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        viewModel.isFioAddressAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        viewModel.isFioServiceAvailable.observe(viewLifecycleOwner, Observer {
            doAddressCheck(viewModel.address.value!!)
        })
        inputEditText.doOnTextChanged { text: CharSequence?, _, _, _ ->
            doAddressCheck(text.toString())
        }
        inputEditText.filters = arrayOf<InputFilter>(
                object : AllCaps() {
                    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
                        return source.toString().toLowerCase(Locale.US)
                    }
                }
        )
    }

    private fun doAddressCheck(fioAddress: String) {
        btNextButton.isEnabled = viewModel.isFioAddressValid.value!! && viewModel.isFioAddressAvailable.value!!
                && viewModel.isFioServiceAvailable.value!! && inputEditText.text!!.isNotEmpty()
        setDefaults()
        if (fioAddress.isNotEmpty()) {
            if (!viewModel.isFioAddressValid.value!!) {
                showErrorOrSuccess(R.string.fio_address_is_invalid, isError = true)
            } else if (!viewModel.isFioServiceAvailable.value!!) {
                showErrorOrSuccess(R.string.fio_address_check_service_unavailable, isError = true)
            } else if (!viewModel.isFioAddressAvailable.value!!) {
                showErrorOrSuccess(R.string.fio_address_occupied, isError = true)
            } else {
                showErrorOrSuccess(R.string.fio_address_available, isError = false)
            }
        }
    }

    private fun setDefaults() {
        tvHint.text = resources.getString(R.string.fio_create_name_hint_text)
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

class DomainsAdapter(context: Context, val items: List<CharSequence>) : ArrayAdapter<CharSequence>(context,
        R.layout.layout_fio_dropdown, R.id.text, items) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val tv = super.getDropDownView(position, convertView, parent) as TextView
        if (position == items.size - 1) {
            tv.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    context.resources.getDrawable(R.drawable.ic_fio_right_arrow), null)
            tv.compoundDrawablePadding = 7
        }
        return tv
    }
}
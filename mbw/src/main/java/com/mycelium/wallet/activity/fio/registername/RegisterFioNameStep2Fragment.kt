package com.mycelium.wallet.activity.fio.registername

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentRegisterFioNameStep2BindingImpl
import kotlinx.android.synthetic.main.fragment_register_fio_name_confirm.btNextButton
import kotlinx.android.synthetic.main.fragment_register_fio_name_step2.*


class RegisterFioNameStep2Fragment : Fragment() {
    private val viewModel: RegisterFioNameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentRegisterFioNameStep2BindingImpl>(inflater, R.layout.fragment_register_fio_name_step2, container, false)
                    .apply {
                        viewModel = this@RegisterFioNameStep2Fragment.viewModel.apply {
                            spinnerFioAccounts?.adapter = ArrayAdapter(context,
                                    R.layout.layout_send_coin_transaction_replace, R.id.text, listOf("FIO 1", "FIO 2")).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                            spinnerPayFromAccounts?.adapter = ArrayAdapter(context,
                                    R.layout.layout_send_coin_transaction_replace, R.id.text, listOf("FIO 1 29.066079 FIO")).apply {
                                this.setDropDownViewResource(R.layout.layout_send_coin_transaction_replace_dropdown)
                            }
                        }
                        lifecycleOwner = this@RegisterFioNameStep2Fragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btNextButton.setOnClickListener {
            requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container,
                            RegisterFioNameCompletedFragment.newInstance(viewModel.addressWithDomain.value!!,
                                    viewModel.account.value!!.label, ""))
                    .addToBackStack(null)
                    .commit()

        }
        viewModel.registrationFee.observe(viewLifecycleOwner, Observer {
            tvFeeInfo.text = resources.getString(R.string.fio_annual_fee, it.toStringWithUnit())
        })
        icEdit.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
    }
}

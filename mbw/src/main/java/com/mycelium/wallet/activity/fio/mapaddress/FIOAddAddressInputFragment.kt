package com.mycelium.wallet.activity.fio.mapaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentFioAddAddressInputBinding
import kotlinx.android.synthetic.main.fragment_fio_add_address_input.*


class FIOAddAddressInputFragment : Fragment() {
    private val viewModel: FIORegisterAddressViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioAddAddressInputBinding>(inflater, R.layout.fragment_fio_add_address_input, container, false)
                    .apply {
                        viewModel = this@FIOAddAddressInputFragment.viewModel
                        lifecycleOwner = this@FIOAddAddressInputFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btNextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
        tvDomain.text = "@${viewModel.domain.value}"
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
        inputEditTextLayout.error = ""
        if (fioAddress.isNotEmpty()) {
            if (!viewModel.isFioAddressValid.value!!) {
                inputEditTextLayout.error = resources.getString(R.string.fio_address_is_invalid)
            } else if (!viewModel.isFioServiceAvailable.value!!) {
                inputEditTextLayout.error = resources.getString(R.string.fio_address_check_service_unavailable)
            } else if (!viewModel.isFioAddressAvailable.value!!) {
                inputEditTextLayout.error = resources.getString(R.string.fio_address_occupied)
            }
        }
    }
}

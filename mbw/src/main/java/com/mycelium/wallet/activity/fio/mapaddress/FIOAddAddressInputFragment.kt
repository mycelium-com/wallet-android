package com.mycelium.wallet.activity.fio.mapaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentFioAddAddressInputBinding
import fiofoundation.io.fiosdk.isFioAddress
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
        nextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)

        }
        domain.text = "@${viewModel.domain.value}"

        inputEditText.doOnTextChanged { text, _, _, _ ->
            if (text!!.isNotEmpty()) {
                if (!viewModel.address.value!!.isFioAddress()) {
                    inputEditTextLayout.isErrorEnabled = true
                    inputEditTextLayout.error = "Invalid FIO address"
                } else {
                    inputEditTextLayout.isErrorEnabled = false
                    inputEditTextLayout.error = null
                }
            }
        }
    }
}

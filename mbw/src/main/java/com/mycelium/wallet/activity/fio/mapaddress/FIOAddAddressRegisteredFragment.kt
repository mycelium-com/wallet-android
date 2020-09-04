package com.mycelium.wallet.activity.fio.mapaddress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentFioAddAddressRegisteredBinding
import kotlinx.android.synthetic.main.fragment_fio_add_address_registered.*
import java.text.SimpleDateFormat
import java.util.*


class FIOAddAddressRegisteredFragment : Fragment() {
    private val viewModel: FIORegisterAddressViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            DataBindingUtil.inflate<FragmentFioAddAddressRegisteredBinding>(inflater, R.layout.fragment_fio_add_address_registered, container, false)
                    .apply {
                        viewModel = this@FIOAddAddressRegisteredFragment.viewModel
                        lifecycleOwner = this@FIOAddAddressRegisteredFragment
                    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        finishButton.setOnClickListener {
            activity?.finish()
        }
        viewModel.expirationDate.observe(viewLifecycleOwner, Observer {
            registeredExpiredIn.text = "Expires $it"
        })
    }
}
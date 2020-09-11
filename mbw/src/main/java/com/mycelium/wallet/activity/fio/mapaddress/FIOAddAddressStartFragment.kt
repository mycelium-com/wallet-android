package com.mycelium.wallet.activity.fio.mapaddress

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_fio_add_address_start.*


class FIOAddAddressStartFragment : Fragment(R.layout.fragment_fio_add_address_start) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btNextButton.setOnClickListener {
            findNavController().navigate(R.id.actionNext)
        }
    }
}

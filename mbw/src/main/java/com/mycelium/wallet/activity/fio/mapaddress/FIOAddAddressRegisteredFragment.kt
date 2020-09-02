package com.mycelium.wallet.activity.fio.mapaddress

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_fio_add_address_registered.*


class FIOAddAddressRegisteredFragment : Fragment(R.layout.fragment_fio_add_address_registered) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        finishButton.setOnClickListener {
            activity?.finish()
        }
    }
}
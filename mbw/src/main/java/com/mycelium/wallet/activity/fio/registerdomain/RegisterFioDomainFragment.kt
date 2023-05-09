package com.mycelium.wallet.activity.fio.registerdomain

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.external.partner.openLink
import kotlinx.android.synthetic.main.fragment_register_fio_domain.*

class RegisterFioDomainFragment : Fragment(R.layout.fragment_register_fio_domain) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        desc.setOnClickListener {
            openLink(getString(R.string.buy_fio_token_link))
        }
    }
}
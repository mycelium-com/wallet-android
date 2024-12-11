package com.mycelium.wallet.activity.fio.registerdomain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentRegisterFioDomainBinding
import com.mycelium.wallet.external.partner.openLink

class RegisterFioDomainFragment : Fragment() {

    var binding: FragmentRegisterFioDomainBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentRegisterFioDomainBinding.inflate(inflater, container, false).apply {
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.desc?.setOnClickListener {
            openLink(getString(R.string.buy_fio_token_link))
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
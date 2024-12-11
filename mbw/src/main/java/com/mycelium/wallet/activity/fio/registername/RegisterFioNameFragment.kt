package com.mycelium.wallet.activity.fio.registername

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentRegisterFioNameBinding

class RegisterFioNameFragment : Fragment() {
    var binding: FragmentRegisterFioNameBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        FragmentRegisterFioNameBinding.inflate(inflater, container, false).apply {
            binding = this
        }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.desc?.text = resources.getText(R.string.fio_create_name_description)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
package com.mycelium.bequant.signup

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_setup_code.*


class SetupCodeFragment : Fragment(R.layout.fragment_bequant_setup_code) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity?)?.supportActionBar?.title = getString(R.string.bequant_page_title_setup_code)
        next.setOnClickListener {
            findNavController().navigate(SetupCodeFragmentDirections.actionNext())
        }
    }
}
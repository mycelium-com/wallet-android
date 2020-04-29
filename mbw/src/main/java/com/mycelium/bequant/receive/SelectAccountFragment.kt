package com.mycelium.bequant.receive

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.receive.adapter.AccountAdapter
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_select_account.*


class SelectAccountFragment : Fragment(R.layout.fragment_bequant_select_account) {
    val adapter = AccountAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = adapter
    }
}
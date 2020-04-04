package com.mycelium.bequant.market

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_account.*


class AccountFragment : Fragment(R.layout.fragment_bequant_account) {
    var receiveListener: (() -> Unit)? = null
    var withdrawListener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deposit.setOnClickListener {
            receiveListener?.invoke()
        }
        withdraw.setOnClickListener {
            withdrawListener?.invoke()
        }
    }
}
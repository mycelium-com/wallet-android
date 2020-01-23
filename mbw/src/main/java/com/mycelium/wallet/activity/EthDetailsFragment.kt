package com.mycelium.wallet.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.GenericTransactionSummary
import kotlinx.android.synthetic.main.transaction_details_btc.*

class EthDetailsFragment(val tx: GenericTransactionSummary) : GenericDetailsFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.transaction_details_eth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}
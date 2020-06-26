package com.mycelium.bequant.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R

class ExchangeSummaryDialog: DialogFragment()  {
    companion object {
        const val accountId = "account_id"
        const val coldStorage = "isColdStorage"
        const val tx = "transaction"

        @JvmStatic
        fun create(): ExchangeSummaryDialog? {
            val dialog = ExchangeSummaryDialog()
            val bundle = Bundle()
//            bundle.putSerializable(accountId, account.id)
//            bundle.putBoolean(coldStorage, isColdStorage)
//            bundle.putSerializable(tx, transaction)
            dialog.arguments = bundle
            return dialog
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_bequant_exchange_summary, container, true)
    }
}
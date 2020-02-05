package com.mycelium.wallet.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.GenericTransactionSummary
import kotlinx.android.synthetic.main.transaction_details_eth.*
import java.math.BigInteger
import kotlin.math.round

class EthDetailsFragment : GenericDetailsFragment() {
    private val tx: EthTransactionSummary by lazy {
        arguments!!.getSerializable("tx") as EthTransactionSummary
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.transaction_details_eth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUi()
    }

    private fun updateUi() {
        alignTables(specific_table)

        val fromAddress = AddressLabel(requireContext())
        fromAddress.address = tx.sender
        llFrom.addView(fromAddress)

        val toAddress = AddressLabel(requireContext())
        toAddress.address = tx.receiver
        llTo.addView(toAddress)

        llValue.addView(getValue(tx.value, null))
        llFee.addView(getValue(tx.fee!!, null))

        tvGasLimit.text = tx.gasLimit.toString()
        tvGasUsed.text = "${tx.gasUsed} (${round(tx.gasUsed.toDouble() / tx.gasLimit.toDouble() * 10000) / 100}%)"
        val txFeeTotal = tx.fee!!.valueAsLong
        val txFeePerUnit = txFeeTotal / tx.gasLimit
        tvGasPrice.text = EthFeeFormatter().getFeePerUnit(txFeePerUnit)
        tvNonce.text = tx.nonce.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance(tx: GenericTransactionSummary): EthDetailsFragment {
            val f = EthDetailsFragment()
            val args = Bundle()

            args.putSerializable("tx", tx)
            f.arguments = args
            return f
        }
    }
}
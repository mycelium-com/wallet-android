package com.mycelium.wallet.activity

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.GenericTransactionSummary
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.transaction_details_eth.*

class EthDetailsFragment(tx: GenericTransactionSummary) : GenericDetailsFragment() {
    val tx: EthTransactionSummary = tx as EthTransactionSummary
    var mbwManager: MbwManager? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.transaction_details_eth, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mbwManager = MbwManager.getInstance(requireContext())
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
        tvGasUsed.text = tx.gasUsed.toString()
        val txFeeTotal = tx.fee!!.valueAsLong
        val txFeePerUnit = txFeeTotal / tx.rawSize
        tvGasPrice.text = EthFeeFormatter().getFeePerUnit(txFeePerUnit)
        tvNonce.text = tx.nonce.toString()
    }

    private operator fun getValue(value: Value, tag: Any?): View? {
        val tv = TextView(requireContext())
        tv.layoutParams = TransactionDetailsActivity.FPWC
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tv.text = value.toStringWithUnit(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.coinType))
        tv.setTextColor(resources.getColor(R.color.white))
        tv.tag = tag
        tv.setOnLongClickListener {
            Utils.setClipboardString(value.toString(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.coinType)), requireContext())
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            true
        }
        return tv
    }
}
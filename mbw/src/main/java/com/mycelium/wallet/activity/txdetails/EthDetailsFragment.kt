package com.mycelium.wallet.activity.txdetails

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.activity.util.EthFeeFormatter
import com.mycelium.wallet.databinding.TransactionDetailsEthBinding
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.TransactionSummary
import java.math.BigDecimal
import java.math.RoundingMode

class EthDetailsFragment : DetailsFragment() {
    private var binding: TransactionDetailsEthBinding? = null
    private val tx: EthTransactionSummary by lazy {
        arguments!!.getSerializable("tx") as EthTransactionSummary
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        TransactionDetailsEthBinding.inflate(inflater, container, false)
            .apply {
                binding = this
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUi()
    }

    private fun updateUi() {
        binding?.run {
            alignTables(specificTable)

            val fromAddress = AddressLabel(requireContext())
            fromAddress.address = tx.sender
            llFrom.addView(fromAddress)

            val toAddress = AddressLabel(requireContext())
            toAddress.address = tx.receiver
            llTo.addView(toAddress)

            llValue.addView(getValue(tx.value, null))
            if (tx.internalValue?.isZero() == false) {
                llValue.addView(TextView(requireContext()).apply {
                    layoutParams = TransactionDetailsActivity.WCWC
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    text = getString(R.string.eth_internal_transfer, tx.internalValue)
                })
            }
            llFee.addView(getValue(tx.fee!!, null))

            tvGasLimit.text = tx.gasLimit.toString()
            val percent = BigDecimal(tx.gasUsed.toDouble() / tx.gasLimit.toDouble() * 100).setScale(
                2,
                RoundingMode.UP
            ).toDouble()
            val percentString =
                if (isWholeNumber(percent)) "%.0f".format(percent) else percent.toString()
            tvGasUsed.text = "${tx.gasUsed} ($percentString%)"
            tvGasPrice.text = EthFeeFormatter().getFeePerUnit(tx.gasPrice.toLong())
            tvNonce.text = tx.nonce.toString()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tx: TransactionSummary): EthDetailsFragment {
            val f = EthDetailsFragment()
            val args = Bundle()

            args.putSerializable("tx", tx)
            f.arguments = args
            return f
        }
    }

    private fun isWholeNumber(d: Double) = d % 1.0 == 0.0
}
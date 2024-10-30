package com.mycelium.wallet.activity.txdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.databinding.TransactionDetailsFioBinding
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.fio.FioTransactionSummary

class FioDetailsFragment : DetailsFragment() {

    var binding: TransactionDetailsFioBinding? = null

    private val tx: FioTransactionSummary by lazy {
        requireArguments().getSerializable("tx") as FioTransactionSummary
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            TransactionDetailsFioBinding.inflate(inflater, container, false)
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

            llValue.addView(getValue(tx.sum, null))
            llFee.addView(getValue(tx.fee!!, null))
            if (tx.memo?.isNotEmpty() == true) {
                tvMemo?.text = tx.memo
                memoRow?.visibility = VISIBLE
            } else {
                memoRow?.visibility = GONE
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tx: TransactionSummary): FioDetailsFragment {
            val f = FioDetailsFragment()
            val args = Bundle()

            args.putSerializable("tx", tx)
            f.arguments = args
            return f
        }
    }

}
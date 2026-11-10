package com.mycelium.wallet.activity.txdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.databinding.TransactionDetailsTronBinding
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.tron.TronTransactionSummary
import com.mycelium.wapi.wallet.tron.TronTxType

class TronDetailsFragment : DetailsFragment() {
    private var binding: TransactionDetailsTronBinding? = null
    private val tx: TronTransactionSummary by lazy {
        requireArguments().getSerializable("tx") as TronTransactionSummary
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        TransactionDetailsTronBinding.inflate(inflater, container, false)
            .apply { binding = this }
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
            tx.fee?.let { llFee.addView(getValue(it, null)) }

            tvTxType.text = getString(typeName(tx.tronTxType))
            tvBandwidth.text = tx.bandwidth.toString()
            rowEnergy.isVisible = tx.energy.signum() > 0
            tvEnergy.text = tx.energy.toString()
            rowResult.isVisible = tx.confirmations > 0
            tvResult.text = getString(if (tx.success) R.string.tx_status_confirmed else R.string.tx_status_failed)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tx: TransactionSummary): TronDetailsFragment {
            val f = TronDetailsFragment()
            val args = Bundle()
            args.putSerializable("tx", tx)
            f.arguments = args
            return f
        }

        fun typeName(type: TronTxType): Int = when (type) {
            TronTxType.TRX_TRANSFER -> R.string.tron_tx_type_trx_transfer
            TronTxType.TRC20_TRANSFER -> R.string.tron_tx_type_trc20_transfer
            TronTxType.CONTRACT_CALL -> R.string.tron_tx_type_contract_call
            TronTxType.STAKE -> R.string.tron_tx_type_stake
            TronTxType.UNSTAKE -> R.string.tron_tx_type_unstake
            TronTxType.WITHDRAW_UNFROZEN -> R.string.tron_tx_type_withdraw_unfrozen
            TronTxType.CANCEL_UNSTAKE -> R.string.tron_tx_type_cancel_unstake
            TronTxType.DELEGATE_RESOURCE -> R.string.tron_tx_type_delegate
            TronTxType.UNDELEGATE_RESOURCE -> R.string.tron_tx_type_undelegate
            TronTxType.VOTE -> R.string.tron_tx_type_vote
            TronTxType.CLAIM_REWARDS -> R.string.tron_tx_type_claim_rewards
            TronTxType.ACCOUNT_ACTIVATION -> R.string.tron_tx_type_account_activation
            TronTxType.PERMISSION_UPDATE -> R.string.tron_tx_type_permission_update
            TronTxType.UNKNOWN -> R.string.tron_tx_type_unknown
        }
    }
}

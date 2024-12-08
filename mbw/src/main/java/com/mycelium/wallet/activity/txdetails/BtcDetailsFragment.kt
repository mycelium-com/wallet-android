package com.mycelium.wallet.activity.txdetails

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AddressLabel
import com.mycelium.wallet.activity.util.BtcFeeFormatter
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.TransactionDetailsBtcBinding
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.coins.Value.Companion.zeroValue
import kotlinx.coroutines.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class BtcDetailsFragment : DetailsFragment() {
    private var tx: TransactionSummary? = null
    private var job: Job? = null

    private val coluMode: Boolean by lazy {
        requireArguments().getBoolean("coluMode")
    }
    private val account: AbstractBtcAccount by lazy {
        mbwManager!!.getWalletManager(false)
            .getAccount(requireArguments().getSerializable("accountId") as UUID) as AbstractBtcAccount
    }
    private var binding: TransactionDetailsBtcBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        TransactionDetailsBtcBinding.inflate(inflater, container, false)
            .apply {
                binding = this
            }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tx = requireArguments().getSerializable("tx") as TransactionSummary
        loadAndUpdate(false)
        listOf(binding?.btFeeRetry, binding?.btInputsRetry)
            .forEach { it?.setOnClickListener { startRemoteLoading() } }
        startRemoteLoading()
    }

    private fun updateUi(isAfterRemoteUpdate: Boolean, suggestRetryIfError: Boolean) {
        binding?.run {

            alignTables(specificTable)

            btFeeRetry.visibility = View.GONE
            btInputsRetry.visibility = View.GONE
            tvFee.visibility = View.VISIBLE
            tvInputsAmount.visibility = View.VISIBLE

            // Set Inputs
            llInputs.removeAllViews()
            if (tx?.inputs != null) {
                var sum = zeroValue(tx!!.type)
                for (input in tx!!.inputs) {
                    sum = sum.plus(input.value)
                }
                if (!sum.equalZero()) {
                    tvInputsAmount.visibility = View.GONE
                    for (item in tx!!.inputs) {
                        llInputs.addView(getItemView(item))
                    }
                }
            }

            // Set Outputs
            llOutputs.removeAllViews()
            if (tx?.outputs != null) {
                for (item in tx!!.outputs) {
                    llOutputs.addView(getItemView(item))
                }
            }

            // Set Fee
            val txFeeTotal = tx!!.fee!!.valueAsLong
            if (txFeeTotal > 0 && tx!!.inputs.size != 0) {
                tvFeeLabel.visibility = View.VISIBLE
                tvInputsLabel.visibility = View.VISIBLE
                var fee =
                    tx!!.fee!!.toStringWithUnit(mbwManager!!.getDenomination(account.coinType)) + "\n"
                if (tx!!.vSize > 0) {
                    fee += BtcFeeFormatter().getFeePerUnitInBytes(txFeeTotal / tx!!.vSize)
                }
                tvFee.text = fee
                tvFee.visibility = View.VISIBLE
            } else {
                tvFee.setText(if (isAfterRemoteUpdate) R.string.no_transaction_details else R.string.no_transaction_loading)
                if (isAfterRemoteUpdate) {
                    if (suggestRetryIfError) {
                        btFeeRetry.visibility = View.VISIBLE
                        btInputsRetry.visibility = View.VISIBLE
                        tvFee.visibility = View.GONE
                        tvInputsAmount.visibility = View.GONE
                    }
                } else {
                    val length = tx!!.inputs.size
                    val amountLoading = if (length > 0) {
                        String.format(
                            "%s %s",
                            length.toString(),
                            getString(R.string.no_transaction_loading)
                        )
                    } else {
                        getString(R.string.no_transaction_loading)
                    }
                    if (tvInputsAmount.isAttachedToWindow) {
                        tvInputsAmount.text = amountLoading
                    }
                }
            }
            binding?.txSize?.text =
                "${tx?.rawSize} B" + if (tx?.vSize != 0) " ${tx?.vSize} vB" else ""
        }
    }

    /**
     * Async task to perform fetching parent transactions of current transaction from server
     */
    private fun startRemoteLoading() {
        val logger = Logger.getLogger(BtcDetailsFragment::class.java.simpleName)

        job = GlobalScope.launch(Dispatchers.Main) {
            val isResultOk = withContext(Dispatchers.IO) {
                try {
                    account.updateParentOutputs(tx!!.id)
                    true
                } catch (e: WapiException) {
                    logger.log(Level.SEVERE, "Can't load parent", e)
                    false
                }
            }
            if (isResultOk) {
                loadAndUpdate(true)
            } else {
                updateUi(isAfterRemoteUpdate = true, suggestRetryIfError = true)
            }
        }
    }

    override fun onStop() {
        job?.cancel()
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun loadAndUpdate(isAfterRemoteUpdate: Boolean) {
        // update tx
        tx = account.getTxSummary(tx!!.id)
        updateUi(isAfterRemoteUpdate, false)
    }

    private fun getItemView(item: OutputViewModel): View { // Create vertical linear layout
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = TransactionDetailsActivity.WCWC
            if (item.isCoinbase) { // Coinbase input
                addView(getValue(item.value, null))
                addView(getCoinbaseText())
            } else { // Add BTC value
                val address = item.address.toString()
                addView(getValue(item.value, address))
                val adrLabel = AddressLabel(requireContext())
                adrLabel.setColuMode(coluMode)
                adrLabel.address = item.address
                addView(adrLabel)
            }
            setPadding(10, 10, 10, 10)
        }
    }

    private fun getCoinbaseText(): View {
        return TextView(requireContext()).apply {
            layoutParams = TransactionDetailsActivity.FPWC
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setText(R.string.newly_generated_coins_from_coinbase)
            setTextColor(whiteColor)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            tx: TransactionSummary,
            coluMode: Boolean,
            accountId: UUID
        ): BtcDetailsFragment =
            BtcDetailsFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("tx", tx)
                    putBoolean("coluMode", coluMode)
                    putSerializable("accountId", accountId)
                }
            }
    }
}
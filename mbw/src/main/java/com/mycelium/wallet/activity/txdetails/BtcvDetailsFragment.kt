package com.mycelium.wallet.activity.txdetails

import android.os.AsyncTask
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
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.android.synthetic.main.transaction_details_btc.*
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


class BtcvDetailsFragment : DetailsFragment() {
    private var tx: TransactionSummary? = null

    private val account: BitcoinVaultHdAccount by lazy {
        mbwManager!!.getWalletManager(false)
                .getAccount(arguments!!.getSerializable("accountId") as UUID) as BitcoinVaultHdAccount
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.transaction_details_btc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tx = arguments!!.getSerializable("tx") as TransactionSummary
        loadAndUpdate(false)
        listOf(btFeeRetry, btInputsRetry).forEach { it.setOnClickListener { startRemoteLoading() } }
        startRemoteLoading()
    }

    private fun updateUi(isAfterRemoteUpdate: Boolean, suggestRetryIfError: Boolean) {
        if (specific_table == null) {
            return
        }
        alignTables(specific_table)

        btFeeRetry.visibility = View.GONE
        btInputsRetry.visibility = View.GONE
        tvFee.visibility = View.VISIBLE
        tvInputsAmount.visibility = View.VISIBLE

        // Set Inputs
        llInputs.removeAllViews()
        if (tx!!.inputs != null) {
            var sum = Value.zeroValue(tx!!.type)
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
        if (tx!!.outputs != null) {
            for (item in tx!!.outputs) {
                llOutputs.addView(getItemView(item))
            }
        }

        // Set Fee
        val txFeeTotal = tx!!.fee!!.valueAsLong
        if (txFeeTotal > 0 && tx!!.inputs.size != 0) {
            tvFeeLabel.visibility = View.VISIBLE
            tvInputsLabel.visibility = View.VISIBLE
            var fee = tx!!.fee!!.toStringWithUnit(mbwManager!!.getDenomination(account.coinType)) + "\n"
            if (tx!!.rawSize > 0) {
                fee += BtcFeeFormatter().getFeePerUnitInBytes(txFeeTotal / tx!!.rawSize)
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
                    String.format("%s %s", length.toString(), getString(R.string.no_transaction_loading))
                } else {
                    getString(R.string.no_transaction_loading)
                }
                if (tvInputsAmount.isAttachedToWindow) {
                    tvInputsAmount.text = amountLoading
                }
            }
        }
    }

    private fun startRemoteLoading() {
        UpdateParentTask().execute()
    }

    private fun loadAndUpdate(isAfterRemoteUpdate: Boolean) {
        // update tx
        tx = account.getTxSummary(tx!!.id)
        updateUi(isAfterRemoteUpdate, false)
    }

    private fun getItemView(item: OutputViewModel): View? { // Create vertical linear layout
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
                adrLabel.address = item.address
                addView(adrLabel)
            }
            setPadding(10, 10, 10, 10)
        }
    }

    private fun getCoinbaseText(): View? {
        return TextView(requireContext()).apply {
            layoutParams = TransactionDetailsActivity.FPWC
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setText(R.string.newly_generated_coins_from_coinbase)
            setTextColor(whiteColor)
        }
    }

    /**
     * Async task to perform fetching parent transactions of current transaction from server
     */
    inner class UpdateParentTask : AsyncTask<Void?, Void?, Boolean>() {
        private val logger = Logger.getLogger(UpdateParentTask::class.java.getSimpleName())
        override fun doInBackground(vararg params: Void?): Boolean {
            try {
                account.updateParentOutputs(tx!!.id)
            } catch (e: WapiException) {
                logger.log(Level.SEVERE, "Can't load parent", e)
                return false
            }
            return true
        }

        override fun onPostExecute(isResultOk: Boolean) {
            super.onPostExecute(isResultOk)
            if (isResultOk) {
                loadAndUpdate(true)
            } else {
                updateUi(isAfterRemoteUpdate = true, suggestRetryIfError = true)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(tx: TransactionSummary, accountId: UUID): BtcvDetailsFragment {
            val f = BtcvDetailsFragment()
            val args = Bundle()

            args.putSerializable("tx", tx)
            args.putSerializable("accountId", accountId)
            f.arguments = args
            return f
        }
    }
}
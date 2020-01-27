package com.mycelium.wallet.activity

import android.os.AsyncTask
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.*
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.GenericOutputViewModel
import com.mycelium.wapi.wallet.GenericTransactionSummary
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.zeroValue
import kotlinx.android.synthetic.main.transaction_details_btc.*


class BtcDetailsFragment(val tx: GenericTransactionSummary, private val coluMode: Boolean) : GenericDetailsFragment() {
    var mbwManager: MbwManager? = null

    private var _white_color = 0
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.transaction_details_btc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _white_color = resources.getColor(R.color.white)
        mbwManager = MbwManager.getInstance(requireContext())
        loadAndUpdate(false)
        startRemoteLoading()
    }

    private fun updateUi(isAfterRemoteUpdate: Boolean, suggestRetryIfError: Boolean) {
        alignTables(specific_table)

        btFeeRetry.visibility = View.GONE
        btInputsRetry.visibility = View.GONE
        tvFee.visibility = View.VISIBLE
        tvInputsAmount.visibility = View.VISIBLE

        // Set Inputs
        llInputs.removeAllViews()
        if (tx.inputs != null) {
            var sum = zeroValue(tx.type)
            for (input in tx.inputs) {
                sum = sum.plus(input.value)
            }
            if (!sum.equalZero()) {
                tvInputsAmount.visibility = View.GONE
                for (item in tx.inputs) {
                    llInputs.addView(getItemView(item))
                }
            }
        }

        // Set Outputs
        llOutputs.removeAllViews()
        if (tx.outputs != null) {
            for (item in tx.outputs) {
                llOutputs.addView(getItemView(item))
            }
        }

        // Set Fee
        val txFeeTotal = tx.fee!!.valueAsLong
        if (txFeeTotal > 0) {
            var fee: String?
            tvFeeLabel.visibility = View.VISIBLE
            tvInputsLabel.visibility = View.VISIBLE
            fee = tx.fee!!.toStringWithUnit(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.coinType)) + "\n"
            if (tx.rawSize > 0) {
                val txFeePerUnit = txFeeTotal / tx.rawSize
                val feeFormatter = FeeFormattingUtil().getFeeFormatter(mbwManager!!.selectedAccount.coinType)
                if (feeFormatter != null) {
                    fee += if (feeFormatter is BtcFeeFormatter) {
                        feeFormatter.getFeePerUnitInBytes(txFeePerUnit)
                    } else {
                        feeFormatter.getFeePerUnit(txFeePerUnit)
                    }
                } else {
                    fee += txFeePerUnit
                }
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
                val length = tx.inputs.size
                val amountLoading: String
                amountLoading = if (length > 0) {
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
        updateUi(isAfterRemoteUpdate, false)
    }

    private fun getItemView(item: GenericOutputViewModel): View? { // Create vertical linear layout
        val ll = LinearLayout(requireContext())
        ll.orientation = LinearLayout.VERTICAL
        ll.layoutParams = TransactionDetailsActivity.WCWC
        if (item.isCoinbase) { // Coinbase input
            ll.addView(getValue(item.value, null))
            ll.addView(getCoinbaseText())
        } else { // Add BTC value
            val address = item.address.toString()
            ll.addView(getValue(item.value, address))
            val adrLabel = AddressLabel(requireContext())
            adrLabel.setColuMode(coluMode)
            adrLabel.address = item.address
            ll.addView(adrLabel)
        }
        ll.setPadding(10, 10, 10, 10)
        return ll
    }

    private fun getCoinbaseText(): View? {
        val tv = TextView(requireContext())
        tv.layoutParams = TransactionDetailsActivity.FPWC
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tv.setText(R.string.newly_generated_coins_from_coinbase)
        tv.setTextColor(_white_color)
        return tv
    }

    private operator fun getValue(value: Value, tag: Any?): View? {
        val tv = TextView(requireContext())
        tv.layoutParams = TransactionDetailsActivity.FPWC
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        tv.text = value.toStringWithUnit(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.coinType))
        tv.setTextColor(_white_color)
        tv.tag = tag
        tv.setOnLongClickListener {
            Utils.setClipboardString(value.toString(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.coinType)), requireContext())
            Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            true
        }
        return tv
    }

    /**
     * Async task to perform fetching parent transactions of current transaction from server
     */
    inner class UpdateParentTask : AsyncTask<Void?, Void?, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            if (mbwManager!!.selectedAccount is AbstractBtcAccount) {
                val selectedAccount = mbwManager!!.selectedAccount as AbstractBtcAccount
                try {
                    selectedAccount.updateParentOutputs(tx.id)
                } catch (e: WapiException) {
                    mbwManager!!.retainingWapiLogger.logError("Can't load parent", e)
                    return false
                }
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
}
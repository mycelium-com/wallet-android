package com.mycelium.wallet.external.changelly2

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeResultBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeResultViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import java.text.DateFormat
import java.util.*


class ExchangeResultFragment : DialogFragment() {

    var binding: FragmentChangelly2ExchangeResultBinding? = null
    val viewModel: ExchangeResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2ExchangeResultBinding.inflate(inflater).apply {
                binding = this
                vm = viewModel
                lifecycleOwner = this@ExchangeResultFragment
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.toolbar?.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
        binding?.buttonOk?.setOnClickListener {
            dismissAllowingStateLoss()
        }
        val txId = arguments?.getString(KEY_TX_ID)
        update(txId)
        val accountId = arguments?.getSerializable(KEY_ACCOUNT_ID) as UUID?
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        accountId?.let {
            walletManager.getAccount(it)
        }?.getTxSummary(HexUtils.toBytes(arguments?.getString(KEY_TX)))?.let { tx ->
            updateTx(tx)
        } ?: let {
            binding?.txDetailsLayout?.visibility = View.GONE
            binding?.more?.visibility = View.GONE
        }
    }

    private fun update(txId: String?) {
        Changelly2Repository.getTransaction(lifecycleScope, txId!!,
                { response ->
                    response?.result?.first()?.let { result ->
                        binding?.toolbar?.title = when (result.status) {
                            "waiting" -> "Exchange in progress"
                            "confirming" -> "Exchange in progress"
                            "exchanging" -> "Exchange in progress"
                            "sending" -> "Exchange in progress"
                            "finished" -> "Exchange completed"
                            "failed" -> "Exchange failed"
                            "refunded" -> "Exchange failed"
                            "hold" -> "Hold"
                            "expired" -> "Exchange expired"
                            else -> "Unknown tx status"
                        }
                        viewModel.setTransaction(result)
                    } ?: let {
                        AlertDialog.Builder(requireContext())
                                .setMessage(response?.error?.message)
                                .setPositiveButton(R.string.button_ok) { _, _ ->
                                    dismissAllowingStateLoss()
                                }
                                .show()
                    }
                },
                { _, msg ->
                    AlertDialog.Builder(requireContext())
                            .setMessage(msg)
                            .setPositiveButton(R.string.button_ok) { _, _ ->
                                dismissAllowingStateLoss()
                            }
                            .show()
                })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.exchange_changelly2_result, menu)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.refresh -> {
                    update(arguments?.getString(KEY_TX_ID))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun updateTx(tx: TransactionSummary) {
        // Set Hash
        binding?.txDetails?.tvHash?.run {
            setColuMode(false)
            setTransaction(tx)
        }

        // Set Confirmed
        val confirmations = tx.confirmations
        var confirmed = if (confirmations > 0) {
            resources.getString(R.string.confirmed_in_block, tx.height)
        } else {
            resources.getString(R.string.no)
        }

        // check if tx is in outgoing queue
        if (tx.isQueuedOutgoing) {
            binding?.txDetails?.tcdConfirmations?.setNeedsBroadcast()
            binding?.txDetails?.tvConfirmations?.text = ""
            confirmed = resources.getString(R.string.transaction_not_broadcasted_info)
        } else {
            binding?.txDetails?.tcdConfirmations?.setConfirmations(confirmations)
            binding?.txDetails?.tvConfirmations?.text = confirmations.toString()
        }
        binding?.txDetails?.tvConfirmed?.text = confirmed

        // Set Date & Time
        val date = Date(tx.timestamp * 1000L)
        val locale = resources.configuration.locale
        binding?.txDetails?.tvDate?.text = DateFormat.getDateInstance(DateFormat.LONG, locale).format(date)
        binding?.txDetails?.tvTime?.text = DateFormat.getTimeInstance(DateFormat.LONG, locale).format(date)
    }

    companion object {
        const val KEY_TX_ID = "tx_id"
        const val KEY_TX = "tx"
        const val KEY_ACCOUNT_ID = "account_id"
    }
}
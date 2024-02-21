package com.mycelium.wallet.external.changelly2

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.view.forEach
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.util.HexUtils
import com.mycelium.view.RingDrawable
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2ExchangeResultBinding
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeResultViewModel
import com.mycelium.wallet.external.partner.openLink
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.TransactionSummary
import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ExchangeResultFragment : DialogFragment() {

    var binding: FragmentChangelly2ExchangeResultBinding? = null
    val viewModel: ExchangeResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_Changelly)
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
        binding?.trackLink?.setOnClickListener {
            if(viewModel.trackLink.value?.isNotEmpty() == true) {
                openLink(viewModel.trackLink.value)
            }
        }
        arguments?.getString(KEY_CHANGELLY_TX_ID)?.let{
            update(it)
        }?: run {
            Toaster(requireContext()).toast("Something went wrong! Exchange transaction id is null", false)
            dismissAllowingStateLoss()
        }
        val walletManager = viewModel.mbwManager.getWalletManager(false)
        binding?.more?.setOnClickListener {
            viewModel.more.value = !viewModel.more.value!!
        }
        viewModel.fromAddress.value = (arguments?.getSerializable(KEY_ACCOUNT_FROM_ID) as UUID?)?.let {
            walletManager.getAccount(it)
        }?.let {
            "${AddressUtils.toShortString(it.receiveAddress.toString())} ${it.label}"
        } ?: ""
        viewModel.toAddress.value = (arguments?.getSerializable(KEY_ACCOUNT_TO_ID) as UUID?)?.let {
            walletManager.getAccount(it)
        }?.let {
            "${AddressUtils.toShortString(it.receiveAddress.toString())} ${it.label}"
        } ?: ""
        startProgressAnimation()
    }

    private fun update(txId: String) {
        loader(true)
        Changelly2Repository.getTransaction(lifecycleScope, txId,
                { response ->
                    response?.result?.first()?.let { result ->
                        binding?.toolbar?.title = result.first().getReadableStatus("exchange")
                        viewModel.setTransaction(result.first())
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
                },
                {
                    loader(false)
                })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        binding?.toolbar?.menu?.clear()
        inflater.inflate(R.menu.exchange_changelly2_result, binding?.toolbar?.menu)
        binding?.toolbar?.menu?.forEach {
            it.setOnMenuItemClickListener {
                onOptionsItemSelected(it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onResume() {
        super.onResume()
        (arguments?.getSerializable(KEY_ACCOUNT_FROM_ID) as UUID?)?.let { fromAccount ->
            arguments?.getString(KEY_CHAIN_TX)?.let { chainTx ->
                viewModel.mbwManager.getWalletManager(false).getAccount(fromAccount)
                        ?.getTxSummary(HexUtils.toBytes(chainTx))?.let { tx ->
                            updateTx(tx)
                        }
            }
        } ?: let {
            binding?.txDetailsLayout?.visibility = View.GONE
            binding?.more?.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.refresh -> {
                    arguments?.getString(KEY_CHANGELLY_TX_ID)?.let {
                        update(it)
                    } ?: run {
                        Toaster(requireContext()).toast("Something went wrong! Exchange transaction id is null", false)
                        dismissAllowingStateLoss()
                    }
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

    private var animationJob: Job? = null

    private fun startProgressAnimation() {
        animationJob?.cancel()
        animationJob = startCoroutineTimer(lifecycleScope, repeatMillis = TimeUnit.SECONDS.toMillis(1)) { counter ->
            binding?.trackLinkWait?.setImageDrawable(RingDrawable(counter % 30 / 30f * 360f, Color.parseColor("#2E6699")))
        }
    }

    companion object {
        const val KEY_CHANGELLY_TX_ID = "tx_id"
        const val KEY_CHAIN_TX = "chain_tx"
        const val KEY_ACCOUNT_TO_ID = "account_to_id"
        const val KEY_ACCOUNT_FROM_ID = "account_from_id"
    }
}
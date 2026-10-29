package com.mycelium.wallet.activity.main

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.DialogCancelEthTxBinding
import com.mycelium.wallet.databinding.RowCancelEthTxBinding
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.CancelEstimate
import com.mycelium.wapi.wallet.eth.EthTxStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Confirmation dialog for cancelling a pending ETH/ERC20 outgoing tx.
 *
 * The cancel itself is a 0-value ETH self-transfer broadcast at the same
 * nonce as the original — it succeeds only if it confirms before the original.
 * Gas is paid in ETH (also for ERC20), so the dialog blocks the confirm action
 * when the parent ETH balance is short.
 */
object CancelEthTxDialog {

    fun show(
        fragment: Fragment,
        account: AbstractEthERC20Account,
        record: EthTransactionSummary,
    ) {
        if (!fragment.isAdded) return
        val ctx = fragment.requireContext()
        val binding = DialogCancelEthTxBinding.inflate(LayoutInflater.from(ctx))

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.cancel_eth_tx_title)
            .setView(binding.root)
            .setPositiveButton(R.string.cancel_eth_tx_confirm, null)
            .setNegativeButton(R.string.cancel_eth_tx_dismiss) { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val confirmBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            confirmBtn.isEnabled = false

            fragment.viewLifecycleOwner.lifecycleScope.launch {
                val estimate = withContext(Dispatchers.IO) {
                    runCatching { account.estimateCancelTx(record) }.getOrNull()
                }
                binding.progressLoading.visibility = View.GONE
                if (estimate == null) {
                    binding.tvLoadError.visibility = View.VISIBLE
                    return@launch
                }
                populate(binding, account, record, estimate)
                showQueueBlocker(fragment, dialog, binding, account, record)

                if (!estimate.hasEnoughEth) {
                    binding.tvInsufficientEth.visibility = View.VISIBLE
                    confirmBtn.isEnabled = false
                } else {
                    confirmBtn.isEnabled = true
                }

                confirmBtn.setOnClickListener {
                    confirmBtn.isEnabled = false
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                    fragment.viewLifecycleOwner.lifecycleScope.launch {
                        broadcastCancel(fragment, account, record, estimate)
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun populate(
        binding: DialogCancelEthTxBinding,
        account: AbstractEthERC20Account,
        record: EthTransactionSummary,
        estimate: CancelEstimate,
    ) {
        binding.contentContainer.visibility = View.VISIBLE

        val ctx = binding.root.context
        val mbwManager = MbwManager.getInstance(ctx)
        val txCoinDenomination = mbwManager.getDenomination(account.coinType)
        val ethDenomination = mbwManager.getDenomination(account.basedOnCoinType)

        bindRow(binding.rowRecipient, ctx.getString(R.string.cancel_eth_tx_recipient),
            record.receiver.addressString)
        bindRow(binding.rowAmount, ctx.getString(R.string.cancel_eth_tx_amount),
            record.value.toStringWithUnit(txCoinDenomination))
        bindRow(binding.rowNonce, ctx.getString(R.string.cancel_eth_tx_nonce),
            estimate.nonce.toString())

        bindRow(binding.rowOldFee, ctx.getString(R.string.cancel_eth_tx_old_fee),
            estimate.oldFee.toStringWithUnit(ethDenomination))
        bindRow(binding.rowNewFee, ctx.getString(R.string.cancel_eth_tx_new_fee),
            estimate.newFee.toStringWithUnit(ethDenomination))
        bindRow(binding.rowEthBalance, ctx.getString(R.string.cancel_eth_tx_eth_balance),
            estimate.ethSpendable.toStringWithUnit(ethDenomination))
    }

    /**
     * Txs mine in nonce order: while an earlier one is priced under the base fee, this cancel
     * waits behind it as long as it does (2026-10-26: cancels of 27–29 behind a stuck 26). Says
     * so, and offers to cancel the earlier one when its record is on this device.
     */
    private suspend fun showQueueBlocker(
        fragment: Fragment,
        dialog: AlertDialog,
        binding: DialogCancelEthTxBinding,
        account: AbstractEthERC20Account,
        record: EthTransactionSummary,
    ) {
        val nonce = record.nonce ?: return
        val blocker = account.findStuckQueueTx(beforeNonce = nonce) ?: return
        if (!fragment.isAdded || !dialog.isShowing) return
        val ctx = binding.root.context
        binding.tvQueueBlocked.text = ctx.getString(R.string.cancel_eth_tx_queue_blocked, blocker.nonce.toString())
        binding.tvQueueBlocked.visibility = View.VISIBLE

        // ERC20 cancels and sends of the same address also live in the parent ETH account.
        val txId = HexUtils.toBytes(blocker.txid.removePrefix("0x"))
        val owner = listOfNotNull(account, (account as? ERC20Account)?.ethAcc)
            .firstOrNull { (it.getTxSummary(txId) as? EthTransactionSummary)?.status == EthTxStatus.PENDING }
            ?: return
        val blockerRecord = owner.getTxSummary(txId) as EthTransactionSummary
        binding.btnCancelBlocking.text = ctx.getString(R.string.cancel_eth_tx_cancel_blocking, blocker.nonce.toString())
        binding.btnCancelBlocking.visibility = View.VISIBLE
        binding.btnCancelBlocking.setOnClickListener {
            dialog.dismiss()
            show(fragment, owner, blockerRecord)
        }
    }

    private fun bindRow(rowBinding: RowCancelEthTxBinding, label: String, value: String) {
        rowBinding.tvLabel.text = label
        rowBinding.tvValue.text = value
    }

    private suspend fun broadcastCancel(
        fragment: Fragment,
        account: AbstractEthERC20Account,
        record: EthTransactionSummary,
        estimate: CancelEstimate,
    ) {
        val ctx = fragment.requireContext()
        val toaster = Toaster(fragment)
        val result: BroadcastResult = withContext(Dispatchers.IO) {
            runCatching { account.cancelTransaction(record, estimate) }
                .getOrElse { e ->
                    BroadcastResult(
                        e.localizedMessage ?: e.javaClass.simpleName,
                        BroadcastResultType.REJECT_INVALID_TX_PARAMS,
                    )
                }
        }
        when (result.resultType) {
            BroadcastResultType.SUCCESS -> {
                toaster.toast(R.string.cancel_eth_tx_success, false)
                MbwManager.getEventBus().post(BalanceChanged(account.id))
                // Kick off a sync so reconcileLocalPending can pick up the
                // confirmed cancel as a replacement for the original. ERC20
                // cancels live in the parent ETH account's tx list, so we
                // also sync the parent — otherwise the EthAccount view of
                // the ERC20 send would stay PENDING until the next
                // periodic sync.
                val mbw = MbwManager.getInstance(ctx)
                val accounts = buildList {
                    add(account)
                    (account as? ERC20Account)?.ethAcc?.let { add(it) }
                }
                mbw.getWalletManager(false)
                    .startSynchronization(SyncMode.NORMAL_FORCED, accounts)
            }
            BroadcastResultType.NO_SERVER_CONNECTION ->
                toaster.toast(R.string.cancel_eth_tx_no_connection, false)
            else ->
                toaster.toast(
                    ctx.getString(R.string.cancel_eth_tx_failed, result.errorMessage ?: ""),
                    false,
                )
        }
    }
}

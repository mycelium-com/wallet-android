package com.mycelium.wallet.activity.send

import android.app.AlertDialog
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.event.BroadcastResultListener
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException
import java.util.*


class BroadcastDialog : DialogFragment() {
    companion object {
        const val accountId = "account_id"
        const val coldStorage = "isColdStorage"
        const val tx = "transaction"

        @JvmOverloads
        fun create(account: WalletAccount<GenericTransaction, *>, isColdStorage: Boolean = false
                   , transaction: GenericTransaction): BroadcastDialog? {
            val dialog = BroadcastDialog()
            val bundle = Bundle()
            bundle.putSerializable(accountId, account.id)
            bundle.putBoolean(coldStorage, isColdStorage)
            bundle.putSerializable(tx, transaction)
            dialog.arguments = bundle
            return dialog
        }
    }

    lateinit var account: WalletAccount<GenericTransaction, *>
    lateinit var transaction: GenericTransaction
    var isCold: Boolean = false
    private var task: BroadcastTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isCold = it.getBoolean(coldStorage, false)
            val manager = MbwManager.getInstance(activity);
            account = manager.getWalletManager(isCold).getAccount(it[accountId] as UUID) as WalletAccount<GenericTransaction, *>
            transaction = it[tx] as GenericTransaction
        }
        startBroadcastingTask()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_broadcast_transaction, container, false)
    }

    override fun onDestroy() {
        task?.cancel(true)
        super.onDestroy()
    }

    private fun startBroadcastingTask() {
        // Broadcast the transaction in the background
        if (activity != null) {
            task = BroadcastTask(account, transaction)
            task?.listener = {
                dismiss()
                handleResult(it)
            }
            if (Utils.isConnected(context)) {
                task?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            } else {
                task?.listener?.invoke(BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION))
            }
        } else {
            dismiss()
        }
    }

    private fun returnResult(it: BroadcastResult) {
        if (targetFragment is BroadcastResultListener) {
            (targetFragment as BroadcastResultListener).broadcastResult(it)
        } else if (activity is BroadcastResultListener) {
            (activity as BroadcastResultListener).broadcastResult(it)
        }
    }

    class BroadcastTask(val account: WalletAccount<GenericTransaction, *>, val transaction: GenericTransaction) : AsyncTask<Void, Int, BroadcastResult>() {
        var listener: ((BroadcastResult) -> Unit)? = null

        override fun doInBackground(vararg args: Void): BroadcastResult {
            try {
                return account.broadcastTx(transaction)
            } catch (e: TransactionBroadcastException) {
                Log.e("BroadcastDialog", "", e)
                return BroadcastResult(BroadcastResultType.REJECTED)
            }
        }

        override fun onPostExecute(result: BroadcastResult) {
            listener?.invoke(result)
        }
    }

    private fun handleResult(broadcastResult: BroadcastResult) {
        if (broadcastResult.resultType == BroadcastResultType.REJECT_DUPLICATE) {
            // Transaction rejected, display message and exit
            Utils.showSimpleMessageDialog(activity, R.string.transaction_rejected_double_spending_message) {
                returnResult(broadcastResult)
            }
        } else if (broadcastResult.resultType == BroadcastResultType.REJECTED) {
            // Transaction rejected, display message and exit
            Utils.showSimpleMessageDialog(activity, R.string.transaction_rejected_message) {
                returnResult(broadcastResult)
            }
        } else if (broadcastResult.resultType == BroadcastResultType.NO_SERVER_CONNECTION) {
            if (isCold) {
                // When doing cold storage spending we do not offer to queue the transaction
                Utils.showSimpleMessageDialog(activity, R.string.transaction_not_sent) {
                    returnResult(broadcastResult)
                }
            } else {
                // Offer the user to queue the transaction
                AlertDialog.Builder(activity)
                        .setTitle(R.string.no_server_connection)
                        .setMessage(R.string.queue_transaction_message)
                        .setPositiveButton(R.string.yes) { arg0, arg1 ->
                            // todo broadcast in queue
                            //                          ((WalletBtcAccount)_account).queueTransaction(TransactionEx.fromUnconfirmedTransaction(sendRequest.tx));
                            //                          setResultOkay();
                            //                          returnResult(broadcastResult)
                        }
                        .setNegativeButton(R.string.no) { arg0, arg1 -> returnResult(broadcastResult) }
                        .show()

            }
        } else if (broadcastResult.resultType == BroadcastResultType.SUCCESS) {
            // Toast success and finish
            Toast.makeText(activity, resources.getString(R.string.transaction_sent),
                    Toast.LENGTH_LONG).show()
            returnResult(broadcastResult)
        } else {
            throw RuntimeException()
        }
    }
}
package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import android.os.AsyncTask
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.fio.FioAccount
import java.util.*

class SendFioModel(application: Application,
                   account: WalletAccount<*>,
                   intent: Intent)
    : SendCoinsModel(application, account, intent) {

    init {
        UpdateFeeTask(account as FioAccount) { feeInSUF ->
            val coinType = account.coinType

            val feeValue = if (feeInSUF != null) {
                Value.valueOf(coinType, feeInSUF)
            } else {
                Value.valueOf(coinType, DEFAULT_FEE)
            }

            selectedFee.value = feeValue
            feeEstimation = FeeEstimationsGeneric(feeValue,
                    feeValue,
                    feeValue,
                    feeValue,
                    if (feeInSUF != null) Date().time else 0)
            showStaleWarning.value = feeInSUF == null
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        TODO("Not yet implemented")
    }

    override fun getFeeLvlItems(): List<FeeLvlItem> {
        return MinerFee.values()
                .map { fee ->
                    val blocks = when (fee) {
                        MinerFee.LOWPRIO -> 120
                        MinerFee.ECONOMIC -> 20
                        MinerFee.NORMAL -> 8
                        MinerFee.PRIORITY -> 2
                    }
                    val duration = Utils.formatBlockcountAsApproxDuration(mbwManager, blocks, EthCoin.BLOCK_TIME_IN_SECONDS)
                    FeeLvlItem(fee, "~$duration", SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM)
                }
    }

    class UpdateFeeTask(
            val account: FioAccount,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                account.getTransferTokensFee().toString()
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }

    companion object {
        const val DEFAULT_FEE = "1000000000" // 1 FIO
    }
}
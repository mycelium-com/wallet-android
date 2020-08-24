package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.fio.FioAccount
import java.lang.Exception
import java.util.*

class SendFioModel(application: Application,
                   account: WalletAccount<*>,
                   intent: Intent)
    : SendCoinsModel(application, account, intent) {
    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        TODO("Not yet implemented")
    }
    init {
        feeEstimation = getTransferTokensFee()
    }

    private fun getTransferTokensFee(): FeeEstimationsGeneric {
        val coinType = account.coinType
        return try {
            val fee = (account as FioAccount).getTransferTokensFee()
            FeeEstimationsGeneric(Value.valueOf(coinType, fee),
                    Value.valueOf(coinType, fee),
                    Value.valueOf(coinType, fee),
                    Value.valueOf(coinType, fee),
                    Date().time)
        } catch (e: Exception) {
            FeeEstimationsGeneric(Value.valueOf(coinType, 1000000000),
                    Value.valueOf(coinType, 33000000000),
                    Value.valueOf(coinType, 67000000000),
                    Value.valueOf(coinType, 100000000000),
                    0)
        }
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

}
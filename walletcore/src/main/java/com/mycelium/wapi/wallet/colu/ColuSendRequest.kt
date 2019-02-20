package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.SendRequest
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value


class ColuSendRequest(type: CryptoCurrency?, val destination: BtcAddress, val amount: Value, var _fee: Value)
    : SendRequest<ColuTransaction>(type, _fee) {
    var txHex: String? = null

    var fundingAddress: List<BtcAddress> = listOf()

    var baseTransaction: Transaction? = null

    val fundingAccounts = mutableListOf<WalletAccount<*, BtcAddress>>()

    fun setTransaction(tx: Transaction) {
        baseTransaction = tx
        this.tx = ColuTransaction(tx.id, this.type, Value.zeroValue(type)
                , 0, tx, 0, 0, false
                , listOf(), listOf())
    }

    override fun getEstimatedTransactionSize(): Int {
        if (baseTransaction != null) {
            return baseTransaction?.txRawSize!!
        } else {
            var estimatorBuilder = FeeEstimatorBuilder()
            val estimator = estimatorBuilder.setLegacyInputs(2)
                    .setLegacyOutputs(4)
                    .createFeeEstimator()
            return estimator.estimateTransactionSize()

        }
    }
}
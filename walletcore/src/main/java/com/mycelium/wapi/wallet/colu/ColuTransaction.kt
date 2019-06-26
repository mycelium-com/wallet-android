package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex


class ColuTransaction(type: CryptoCurrency?, val destination: BtcAddress, val amount: Value, val feePerKb: Value?)
    : GenericTransaction(type) {
    var txHex: String? = null

    var fundingAddress: List<BtcAddress> = listOf()

    var baseTransaction: ColuBroadcastTxHex.Json? = null

    var transaction : Transaction? = null

    val fundingAccounts = mutableListOf<WalletAccount<BtcAddress>>()

    override fun getEstimatedTransactionSize(): Int {
        if (baseTransaction != null) {
            //TODO fix length
            return baseTransaction?.txHex?.length!!
        } else {
            var estimatorBuilder = FeeEstimatorBuilder()
            val estimator = estimatorBuilder.setLegacyInputs(2)
                    .setLegacyOutputs(4)
                    .createFeeEstimator()
            return estimator.estimateTransactionSize()

        }
    }

    override fun getId(): ByteArray? {
        return transaction?.id!!.bytes
    }

    override fun txBytes(): ByteArray? {
        return transaction?.toBytes()
    }
}
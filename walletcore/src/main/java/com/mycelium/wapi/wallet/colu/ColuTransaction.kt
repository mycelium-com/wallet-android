package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.model.BitcoinTransaction
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex


class ColuTransaction(type: CryptoCurrency?, val destination: BtcAddress?, val amount: Value?, val feePerKb: Value?)
    : Transaction(type) {
    var txHex: String? = null

    var fundingAddress: List<BtcAddress> = listOf()

    var baseTransaction: ColuBroadcastTxHex.Json? = null

    var transaction : BitcoinTransaction? = null

    val fundingAccounts = mutableListOf<WalletAccount<BtcAddress>>()

    override fun getEstimatedTransactionSize(): Int {
        return if (baseTransaction != null) {
            //TODO fix length
            baseTransaction?.txHex?.length!!
        } else {
            val estimatorBuilder = FeeEstimatorBuilder()
            val estimator = estimatorBuilder.setLegacyInputs(2)
                    .setLegacyOutputs(4)
                    .createFeeEstimator()
            estimator.estimateTransactionSize()
        }
    }

    override fun getId(): ByteArray? {
        return transaction?.id!!.bytes
    }

    override fun txBytes(): ByteArray? {
        return transaction?.toBytes()
    }
}
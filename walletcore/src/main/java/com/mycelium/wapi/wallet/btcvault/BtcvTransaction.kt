package com.mycelium.wapi.wallet.btcvault

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.model.BitcoinTransaction
import com.mycelium.wapi.wallet.BitcoinBasedTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable

class BtcvTransaction(type: CryptoCurrency, val destination: BtcvAddress?, val amount: Value?, val feePerKb: Value?)
    : BitcoinBasedTransaction(type, feePerKb), Serializable {

    constructor(coinType: CryptoCurrency, tx: BitcoinTransaction): this (coinType, null, null, null) {
        setTransaction(tx)
    }

    fun setTransaction(tx: BitcoinTransaction) {
        this.tx = tx
        this.isSigned = true
    }

    override fun getEstimatedTransactionSize(): Int {
        val estimatorBuilder = FeeEstimatorBuilder()
        val estimator = if (unsignedTx != null) {
            estimatorBuilder.setArrayOfInputs(unsignedTx!!.fundingOutputs)
                    .setArrayOfOutputs(unsignedTx!!.outputs)
                    .createFeeEstimator()
        } else {
            estimatorBuilder.setLegacyInputs(1)
                    .setLegacyOutputs(2)
                    .createFeeEstimator()
        }
        return estimator.estimateTransactionSize()
    }
}
package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.BitcoinBasedGenericTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value

import java.io.Serializable

class BtcTransaction constructor(type: CryptoCurrency, val destination: BtcAddress?, val amount: Value?, val feePerKb: Value?)
    : BitcoinBasedGenericTransaction(type, feePerKb), Serializable {
    fun setTransaction(tx: Transaction) {
        this.tx = tx
        this.isSigned = true
    }

    constructor(coinType: CryptoCurrency, tx: Transaction): this (coinType, null, null, null) {
        setTransaction(tx)
    }

    constructor(coinType: CryptoCurrency, unsignedTx: UnsignedTransaction) : this(coinType, null, null, null){
        this.unsignedTx = unsignedTx
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

    companion object {
        @JvmStatic
        fun to(destination: BtcAddress, amount: Value, feePerkb: Value): BtcTransaction {
            return BtcTransaction(destination.coinType, destination, amount, feePerkb)
        }
    }
}

package com.mycelium.wapi.wallet.btc

import com.mrd.bitlib.FeeEstimator
import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.BitcoinBasedSendRequest
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value

import java.io.Serializable

class BtcSendRequest private constructor(type: CryptoCurrency, val destination: BtcAddress, val amount: Value, feePerKb: Value) : BitcoinBasedSendRequest<BtcTransaction>(type, feePerKb), Serializable {
    fun setTransaction(tx: Transaction) {
        this.tx = BtcTransaction(this.type, tx)
    }

    override fun getEstimatedTransactionSize(): Int {
        val estimatorBuilder = FeeEstimatorBuilder()
        val estimator: FeeEstimator
        if (unsignedTx != null) {
            estimator = estimatorBuilder.setArrayOfInputs(unsignedTx!!.fundingOutputs)
                    .setArrayOfOutputs(unsignedTx!!.outputs)
                    .createFeeEstimator()
        } else {
            estimator = estimatorBuilder.setLegacyInputs(1)
                    .setLegacyOutputs(2)
                    .createFeeEstimator()
        }
        return estimator.estimateTransactionSize()
    }

    override fun isSpendingUnconfirmed(account: WalletAccount<*, *>): Boolean {
        val networkParameters = if (type == BitcoinMain.get())
            NetworkParameters.productionNetwork
        else if (type == BitcoinTest.get()) NetworkParameters.testNetwork else null

        if (unsignedTx == null || networkParameters == null || account !is WalletBtcAccount) {
            return false
        }

        for (out in unsignedTx!!.fundingOutputs) {
            val address = out.script.getAddress(networkParameters)
            if (out.height == -1 && account.isOwnExternalAddress(address)) {
                // this is an unconfirmed output from an external address -> we want to warn the user
                // we allow unconfirmed spending of internal (=change addresses) without warning
                return true
            }
        }
        //no unconfirmed outputs are used as inputs, we are fine
        return false
    }

    companion object {

        fun to(destination: BtcAddress, amount: Value, feePerkb: Value): BtcSendRequest {
            return BtcSendRequest(destination.coinType, destination, amount, feePerkb)
        }
    }
}

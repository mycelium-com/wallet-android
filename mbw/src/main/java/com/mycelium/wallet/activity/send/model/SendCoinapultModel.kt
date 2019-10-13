package com.mycelium.wallet.activity.send.model

import android.app.Application
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coinapult.CoinapultAccount
import com.mycelium.wapi.wallet.coinapult.Currency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException

class SendCoinapultModel(context: Application,
                         account: WalletAccount<*>,
                         amount: Value?,
                         receivingAddress: GenericAddress?,
                         transactionLabel: String?,
                         isColdStorage: Boolean)
    : SendCoinsModel(context, account, amount, receivingAddress, transactionLabel, isColdStorage) {

    // Handles BTC payment request
    @Throws(GenericBuildTransactionException::class, GenericInsufficientFundsException::class, GenericOutputTooSmallException::class)
    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        val paymentRequestInformation = paymentRequestHandler.value!!.paymentRequestInformation
        var outputs = paymentRequestInformation.outputs

        // has the payment request an amount set?
        if (paymentRequestInformation.hasAmount()) {
            amount.postValue(Value.valueOf(Utils.getBtcCoinType(), outputs.totalAmount))
        } else {
            if (amount.value!!.isZero()) {
                return TransactionStatus.MissingArguments
            }
            // build new output list with user specified amount
            outputs = outputs.newOutputsWithTotalAmount(toSend.value)
        }

        val btcAccount = account as AbstractBtcAccount
        transaction = btcAccount.createTxFromOutputList(outputs, FeePerKbFee(selectedFee.value!!).feePerKb.value)
        spendingUnconfirmed.postValue(account.isSpendingUnconfirmed(transaction))
        receivingAddress.postValue(null)
        transactionLabel.postValue(paymentRequestInformation.paymentDetails.memo)

        return TransactionStatus.OK
    }

    override fun updateErrorMessage(transactionStatus: TransactionStatus) {
        when (transactionStatus) {
            TransactionStatus.OutputTooSmall -> {
                val coinapultAccount = account as CoinapultAccount
                errorText.postValue(context.getString(R.string.coinapult_amount_too_small,
                        (coinapultAccount.coinType as Currency).minimumConversationValue,
                        coinapultAccount.coinType.symbol)
                )
            }
            TransactionStatus.InsufficientFundsForFee -> {
                errorText.postValue(context.getString(R.string.requires_btc_amount))
            }
            else -> super.updateErrorMessage(transactionStatus)
        }
    }

    override fun getFeeLvlItems(): List<FeeLvlItem> {
        return emptyList()
    }
}
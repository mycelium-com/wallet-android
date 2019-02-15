package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.FeeEstimator;
import com.mrd.bitlib.FeeEstimatorBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public class BtcSendRequest extends SendRequest<BtcTransaction> implements Serializable {

    private Value amount;
    private BtcAddress destination;
    private UnsignedTransaction unsignedTx;

    public BtcSendRequest(CryptoCurrency type, BtcAddress destination, Value amount, Value feePerKb) {
        super(type, feePerKb);

        this.destination = destination;
        this.amount = amount;
    }

    public static BtcSendRequest to(BtcAddress destination, Value amount, Value feePerkb) {
        return new BtcSendRequest(destination.getCoinType(), destination, amount, feePerkb);
    }

    public Value getAmount() {
        return amount;
    }

    public BtcAddress getDestination() {
        return destination;
    }

    public void setUnsignedTx(UnsignedTransaction unsignedTx) {
        this.unsignedTx = unsignedTx;
    }

    public UnsignedTransaction getUnsignedTx() {
        return unsignedTx;
    }

    public void setTransaction(Transaction tx) {
        this.tx = new BtcTransaction(this.type, tx);
    }

    @Override
    public int getEstimatedTransactionSize() {
        FeeEstimatorBuilder estimatorBuilder = new FeeEstimatorBuilder();
        FeeEstimator estimator;
        if (unsignedTx != null) {
            estimator = estimatorBuilder.setArrayOfInputs(unsignedTx.getFundingOutputs())
                    .setArrayOfOutputs(unsignedTx.getOutputs())
                    .createFeeEstimator();
        } else {
            estimator = estimatorBuilder.setLegacyInputs(1)
                    .setLegacyOutputs(2)
                    .createFeeEstimator();
        }
        return estimator.estimateTransactionSize();
    }
}

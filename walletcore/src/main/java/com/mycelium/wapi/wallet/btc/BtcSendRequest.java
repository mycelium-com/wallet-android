package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.FeeEstimator;
import com.mrd.bitlib.FeeEstimatorBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mycelium.wapi.wallet.BitcoinBasedSendRequest;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public class BtcSendRequest extends BitcoinBasedSendRequest<BtcTransaction> implements Serializable {

    private Value amount;
    private BtcAddress destination;
    private UnsignedTransaction unsignedTx;

    private BtcSendRequest(CryptoCurrency type, BtcAddress destination, Value amount, Value feePerKb) {
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

    @Override
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

    @Override
    public boolean isSpendingUnconfirmed(WalletAccount account) {
        NetworkParameters networkParameters =
                type.equals(BitcoinMain.get()) ? NetworkParameters.productionNetwork :
                        type.equals(BitcoinTest.get()) ? NetworkParameters.testNetwork : null;

        if (unsignedTx == null || networkParameters == null || !(account instanceof WalletBtcAccount)) {
            return false;
        }

        for (UnspentTransactionOutput out : unsignedTx.getFundingOutputs()) {
            Address address = out.script.getAddress(networkParameters);
            if (out.height == -1 && ((WalletBtcAccount)account).isOwnExternalAddress(address)) {
                // this is an unconfirmed output from an external address -> we want to warn the user
                // we allow unconfirmed spending of internal (=change addresses) without warning
                return true;
            }
        }
        //no unconfirmed outputs are used as inputs, we are fine
        return false;
    }
}

package com.mycelium.wapi.wallet.btc;

import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public class BtcSendRequest extends SendRequest<BtcTransaction> implements Serializable {

    private Value amount;
    private BtcLegacyAddress destination;
    private UnsignedTransaction unsignedTx;

    public BtcSendRequest(CryptoCurrency type, BtcLegacyAddress destination, Value amount) {
        super(type);

        this.destination = destination;
        this.amount = amount;
    }

    public static BtcSendRequest to(BtcLegacyAddress destination, Value amount) {
        BtcSendRequest req = new BtcSendRequest(destination.getCoinType(), destination, amount);
        return req;
    }

    public Value getAmount() {
        return amount;
    }

    public BtcLegacyAddress getDestination() {
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
}

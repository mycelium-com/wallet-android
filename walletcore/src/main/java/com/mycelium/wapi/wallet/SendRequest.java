package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public abstract class SendRequest<T extends GenericTransaction> implements Serializable {

    /**
     * The blockchain network that this request is going to transact
     */
    public CryptoCurrency type;

    public T tx;

    public Value fee; //May be flat fee or fee per kb, depending on asset type

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }


    // Tracks if this has been passed to wallet.completeTransaction already: just a safety check.
    private boolean completed;

    protected SendRequest(CryptoCurrency type, Value fee) {
        this.type = type;
        this.fee = fee;
    }

    public abstract int getEstimatedTransactionSize();
}

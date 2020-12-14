package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.CryptoCurrency;

import java.io.Serializable;

public abstract class Transaction implements Serializable {

    /**
     * Type of cryptocurrency the transaction operates with
     */
    public CryptoCurrency type;

    public boolean isSigned;

    protected Transaction(CryptoCurrency type) {
        this.type = type;
        this.isSigned = false;
    }

    public abstract byte[] getId();

    public abstract byte[] txBytes();

    public abstract int getEstimatedTransactionSize();
}

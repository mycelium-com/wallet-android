package com.mycelium.wapi.wallet;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public abstract class GenericTransaction implements Serializable {

    /**
     * Type of cryptocurrency the transaction operates with
     */
    public CryptoCurrency type;

    public boolean isSigned;

    protected GenericTransaction(CryptoCurrency type) {
        this.type = type;
        this.isSigned = false;
    }

    public abstract byte[] getId();

    public abstract byte[] txBytes();

    public abstract int getEstimatedTransactionSize();
}

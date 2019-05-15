package com.mycelium.wapi.wallet;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

import java.io.Serializable;

public abstract class GenericTransaction implements Serializable {

    /**
     * The blockchain network that this request is going to transact
     */
    public CryptoCurrency type;

    public boolean isSigned;

    protected GenericTransaction(CryptoCurrency type) {
        this.type = type;
        this.isSigned = false;
    }

    public abstract Sha256Hash getId();

    public abstract byte[] txBytes();

    public abstract int getEstimatedTransactionSize();

    public boolean isSpendingUnconfirmed(WalletAccount account) {
        return false;
    }
}

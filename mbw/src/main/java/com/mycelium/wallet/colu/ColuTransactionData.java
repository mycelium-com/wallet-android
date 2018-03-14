package com.mycelium.wallet.colu;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

public class ColuTransactionData {
    private ColuAccount coluAccount;

    private Address receivingAddress;

    private ExactCurrencyValue nativeAmount;
   
    private long feePerKb;

    public ColuTransactionData(Address receivingAddress, ExactCurrencyValue nativeAmount, 
                               ColuAccount coluAccount, long feePerKb) {
        this.coluAccount = coluAccount;
        this.receivingAddress = receivingAddress;
        this.nativeAmount = nativeAmount;
        this.feePerKb = feePerKb;
    }

    public ColuAccount getColuAccount() {
        return coluAccount;
    }

    public Address getReceivingAddress() {
        return receivingAddress;
    }

    public ExactCurrencyValue getNativeAmount() {
        return nativeAmount;
    }

    public long getFeePerKb() {
        return feePerKb;
    }
}

package com.mycelium.wallet.activity.send.model;

import com.megiontechnologies.Bitcoins;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

/**
 * Created by elvis on 31.08.17.
 */

public class FeeItem {

    public long feePerKb;
    public Bitcoins btc;
    public CurrencyValue currencyValue;
    public int type;

    public FeeItem(long feePerKb, Bitcoins btc, CurrencyValue currencyValue, int type) {
        this.feePerKb = feePerKb;
        this.btc = btc;
        this.currencyValue = currencyValue;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeeItem feeItem = (FeeItem) o;

        if (feePerKb != feeItem.feePerKb) return false;
        return type == feeItem.type;

    }

    @Override
    public int hashCode() {
        int result = (int) (feePerKb ^ (feePerKb >>> 32));
        result = 31 * result + type;
        return result;
    }
}

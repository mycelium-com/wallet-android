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
}

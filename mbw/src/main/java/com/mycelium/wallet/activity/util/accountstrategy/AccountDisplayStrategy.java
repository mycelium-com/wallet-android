package com.mycelium.wallet.activity.util.accountstrategy;

import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wapi.wallet.currency.BitcoinValue;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

/**
 * Strategy to unify accounts displaying duty.
 */
public interface AccountDisplayStrategy {
    /**
     * Returns account type label. For instance "bitcoin", "bitcoincash"...
     */
    String getLabel();

    /**
     * Returns currency name. For instance "Bitcoin", "Bitcoin Cash".
     */
    String getCurrencyName();

    /**
     * Returns input hint. For instance "0.00 BTC", "0.00 MSS".
     */
    String getHint();

    /**
     * Returns formatted sum.
     */
    String getFormattedValue(CurrencyValue sum);
}

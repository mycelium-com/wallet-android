package com.mycelium.wallet.activity.util.accountstrategy;

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
}

package com.mycelium.wallet.activity.util;

public enum AccountDisplayType {
    BTC_ACCOUNT("BTC"),
    BCH_ACCOUNT("BCH"),
    DASH_ACCOUNT("DASH"),
    COLU_ACCOUNT("COLU"),
    COINAPULT_ACCOUNT("COINAPULT"),
    UNKNOWN_ACCOUNT("UNKNOWN");

    private final String accountLabel;

    AccountDisplayType(String accountLabel) {
        this.accountLabel = accountLabel;
    }

    public String getAccountLabel() {
        return accountLabel;
    }
}

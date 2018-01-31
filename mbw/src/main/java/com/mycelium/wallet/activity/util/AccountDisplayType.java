package com.mycelium.wallet.activity.util;

import com.mycelium.wapi.wallet.WalletAccount;

public enum AccountDisplayType {
    BTC_ACCOUNT("BTC"),
    BCH_ACCOUNT("BCH"),
    DASH_ACCOUNT("DASH"),
    COLU_ACCOUNT("COLU"),
    COINAPULT_ACCOUNT("COINAPULT"),
    UNKNOWN_ACCOUNT("UNKNOWN");

    private final String _accountLabel;

    AccountDisplayType(String _accountLabel) {
        this._accountLabel = _accountLabel;
    }

    public static AccountDisplayType getAccountType(WalletAccount account) {
        switch(account.getType()) {
            case BTCBIP44:
            case BTCSINGLEADDRESS:
                return BTC_ACCOUNT;
            case BCHBIP44:
            case BCHSINGLEADDRESS:
                return BCH_ACCOUNT;
            case COINAPULT:
                return COINAPULT_ACCOUNT;
            case COLU:
                return COLU_ACCOUNT;
            case DASH:
                return DASH_ACCOUNT;
            default:
                return UNKNOWN_ACCOUNT;
        }
    }

    public String getAccountLabel() {
        return _accountLabel;
    }
}

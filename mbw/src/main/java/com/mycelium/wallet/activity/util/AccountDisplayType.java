package com.mycelium.wallet.activity.util;

import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;

public enum AccountDisplayType {
    BTC_ACCOUNT,
    BCH_ACCOUNT,
    DASH_ACCOUNT,
    COLU_ACCOUNT,
    COINAPULT_ACCOUNT,
    UNKNOWN_ACCOUNT;

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
}

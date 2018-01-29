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

    AccountDisplayType() {
    }

    public static AccountDisplayType getAccountType(WalletAccount account) {
        if (account.getType() == WalletAccount.Type.BTCBIP44 ||
                account.getType() == WalletAccount.Type.BTCSINGLEADDRESS) {
            return BTC_ACCOUNT;
        }
        if (account.getType() == WalletAccount.Type.BCHBIP44 ||
                account.getType() == WalletAccount.Type.BCHSINGLEADDRESS) {
            return BCH_ACCOUNT;
        }
        if (account.getType() == WalletAccount.Type.COINAPULT) {
            return COINAPULT_ACCOUNT;
        }
        if (account.getType() == WalletAccount.Type.COLU) {
            return COINAPULT_ACCOUNT;
        }
        if (account.getType() == WalletAccount.Type.DASH) {
            return DASH_ACCOUNT;
        }
        return UNKNOWN_ACCOUNT;
    }
}

package com.mycelium.wallet.activity.util;

import android.content.Context;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wapi.wallet.WalletAccount;


public class CoinapultAccountDisplayStrategy extends BTCAccountDisplayStrategy {
    public CoinapultAccountDisplayStrategy(WalletAccount account, Context context, MbwManager mbwManager) {
        super(account, context, mbwManager);
    }
}

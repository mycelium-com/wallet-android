package com.mycelium.wallet.activity.util.accountstrategy;

import android.content.Context;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.activity.util.accountstrategy.BTCAccountDisplayStrategy;
import com.mycelium.wapi.wallet.WalletAccount;


public class CoinapultAccountDisplayStrategy extends BTCAccountDisplayStrategy {
    public CoinapultAccountDisplayStrategy(WalletAccount account, Context context, MbwManager mbwManager) {
        super(account, context, mbwManager);
    }
}

package com.mycelium.wallet.activity.util.accountstrategy;

import android.content.Context;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;


public class CoinapultAccountDisplayStrategy extends BTCAccountDisplayStrategy {
    public CoinapultAccountDisplayStrategy(WalletBtcAccount account, Context context, MbwManager mbwManager) {
        super(account, context, mbwManager);
    }
}

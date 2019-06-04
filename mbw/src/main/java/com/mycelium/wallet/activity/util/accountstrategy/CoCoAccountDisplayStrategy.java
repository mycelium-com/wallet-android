package com.mycelium.wallet.activity.util.accountstrategy;

import android.content.Context;

import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.colu.ColuAccount;

public class CoCoAccountDisplayStrategy implements AccountDisplayStrategy {
    private final ColuAccount account;
    private final Context context;

    public CoCoAccountDisplayStrategy(ColuAccount account, Context context) {
        this.account = account;
        this.context = context;
    }

    @Override
    public String getLabel() {
        return account.getCoinType().getName();
    }

    @Override
    public String getCurrencyName() {
        return account.getCoinType().getSymbol();
    }

    @Override
    public String getHint() {
        return context.getString(R.string.amount_hint_denomination, account.getCoinType().getSymbol());
    }
}

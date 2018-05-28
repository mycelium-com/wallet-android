package com.mycelium.wallet.activity.util.accountstrategy;

import android.content.Context;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

public class CoCoAccountDisplayStrategy implements AccountDisplayStrategy {
    private final ColuAccount account;
    private final Context context;

    public CoCoAccountDisplayStrategy(ColuAccount account, Context context) {
        this.account = account;
        this.context = context;
    }

    @Override
    public String getLabel() {
        return account.getColuAsset().label;
    }

    @Override
    public String getCurrencyName() {
        return account.getColuAsset().name;
    }

    @Override
    public String getHint() {
        return context.getString(R.string.amount_hint_denomination, account.getAccountDefaultCurrency());
    }

    @Override
    public String getFormattedValue(CurrencyValue sum) {
        return Utils.getColuFormattedValueWithUnit(sum);
    }
}

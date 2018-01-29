package com.mycelium.wallet.activity.util;

import android.content.Context;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

public class ColuAccountDisplayStrategy implements AccountDisplayStrategy {
    private final ColuAccount _account;
    private final Context _context;

    public ColuAccountDisplayStrategy(ColuAccount account, Context context) {
        _account = account;
        _context = context;
    }

    @Override
    public String getLabel() {
        return _account.getColuAsset().label;
    }

    @Override
    public String getCurrencyName() {
        return _account.getColuAsset().name;
    }

    @Override
    public String getHint() {
        return _context.getString(R.string.amount_hint_denomination, _account.getAccountDefaultCurrency());
    }

    @Override
    public String getFormattedValue(CurrencyValue sum) {
        return Utils.getColuFormattedValueWithUnit(sum);
    }
}

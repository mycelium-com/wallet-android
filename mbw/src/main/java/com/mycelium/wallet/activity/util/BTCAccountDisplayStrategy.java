package com.mycelium.wallet.activity.util;

import android.content.Context;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

public class BTCAccountDisplayStrategy implements AccountDisplayStrategy {
    private static final String ACCOUNT_LABEL = "bitcoin";
    protected final WalletAccount _account;
    protected final Context _context;
    protected final MbwManager _mbwManager;

    public BTCAccountDisplayStrategy(WalletAccount account, Context context, MbwManager mbwManager) {
        _account = account;
        _context = context;
        _mbwManager = mbwManager;
    }

    @Override
    public String getLabel() {
        return ACCOUNT_LABEL;
    }

    @Override
    public String getCurrencyName() {
        return _context.getString(R.string.bitcoin_name);
    }

    @Override
    public String getHint() {
        return _context.getString(R.string.amount_hint_denomination,
                _mbwManager.getBitcoinDenomination().toString());
    }

    @Override
    public String getFormattedValue(CurrencyValue sum) {
        return Utils.getFormattedValueWithUnit(sum, _mbwManager.getBitcoinDenomination());
    }
}

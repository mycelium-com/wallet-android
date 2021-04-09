package com.mycelium.wallet.activity.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.appcompat.widget.PopupMenu;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.coins.AssetInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalToggleableCurrencyButton extends ToggleableCurrencyDisplay {
    public TotalToggleableCurrencyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TotalToggleableCurrencyButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TotalToggleableCurrencyButton(Context context) {
        super(context);
    }

    @Override
    protected void updateUi() {
        super.updateUi();
        final List<AssetInfo> currencies = getCurrencySwitcher().getCurrencyList();
        // there are more than one fiat-currency
        // there is only one currency to show - don't show a triangle hinting that the user can toggle
        findViewById(R.id.ivSwitchable).setVisibility(currencies.size() > 1 ? VISIBLE : INVISIBLE);

        LinearLayout linearLayout = findViewById(R.id.llContainer);
        final PopupMenu menu = new PopupMenu(getContext(), linearLayout);
        linearLayout.setOnClickListener(v -> menu.show());

        if (currencies.size() > 0) {
            final Map<MenuItem, AssetInfo> itemMap = new HashMap<>();
            for (int i = 0; i < currencies.size(); i++) {
                String currency = currencies.get(i).getSymbol();
                MenuItem item = menu.getMenu().add(currency);
                itemMap.put(item, currencies.get(i));
            }

            menu.setOnMenuItemClickListener(item -> {
                getCurrencySwitcher().setCurrentTotalCurrency(itemMap.get(item));
                if (MbwManager.getEventBus() != null) {
                    // update UI via event bus, also inform other parts of the app about the change
                    MbwManager.getEventBus().post(new SelectedCurrencyChanged());
                } else {
                    updateUi();
                }
                return true;
            });
        }
    }
}

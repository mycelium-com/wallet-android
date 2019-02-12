/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.CurrencySwitcher;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.exchange.ValueSum;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.Value;
import com.squareup.otto.Subscribe;


public class ToggleableCurrencyDisplay extends LinearLayout {
    protected CurrencySwitcher currencySwitcher;

    protected TextView tvCurrency;
    protected TextView tvValue;

    protected Value currentValue;

    protected boolean fiatOnly = false;
    protected boolean hideOnNoExchangeRate = false;
    private int precision = -1;

    public ToggleableCurrencyDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        parseXML(context, attrs);
    }

    public ToggleableCurrencyDisplay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
        parseXML(context, attrs);
    }

    public ToggleableCurrencyDisplay(Context context) {
        super(context);
        init(context);
    }

    void parseXML(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ToggleableCurrencyButton);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.ToggleableCurrencyButton_fiatOnly:
                    setFiatOnly(a.getBoolean(attr, false));
                    break;
                case R.styleable.ToggleableCurrencyButton_textSize:
                    setTextSize(a.getDimensionPixelSize(attr, 12));
                    break;
                case R.styleable.ToggleableCurrencyButton_textColor:
                    setTextColor(a.getColor(attr, getResources().getColor(R.color.lightgrey)));
                    break;
                case R.styleable.ToggleableCurrencyButton_hideOnNoExchangeRate:
                    hideOnNoExchangeRate = a.getBoolean(attr, false);
                case R.styleable.ToggleableCurrencyButton_precision:
                    precision = a.getInteger(attr, -1);
            }
        }
        a.recycle();

    }

    protected void init(Context context) {
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = mInflater.inflate(R.layout.toggleable_currency_display, this, true);

        tvCurrency = view.findViewById(R.id.tvCurrency);
        tvValue = view.findViewById(R.id.tvDisplayValue);
    }

    private void setTextSize(int size) {
        tvCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private void setTextColor(int color) {
        tvCurrency.setTextColor(color);
        tvValue.setTextColor(color);
    }

    protected void updateUi() {
        Preconditions.checkNotNull(currencySwitcher);

        if (fiatOnly) {
            showFiat();
        } else {
            // Switch to BTC if no fiat fx rate is available
            if (!currencySwitcher.isFiatExchangeRateAvailable()
                    && currencySwitcher.isFiatCurrency(currencySwitcher.getCurrentCurrency())
                    && !currencySwitcher.isFiatCurrency(currencySwitcher.getDefaultCurrency())) {
                currencySwitcher.setCurrency(BitcoinMain.get());
            }

            setVisibility(VISIBLE);

            tvValue.setText(currentValue != null ? ValueExtensionsKt.toString(currentValue, currencySwitcher.getBitcoinDenomination()) : null);
            String currentCurrency = currencySwitcher.getCurrentCurrencyIncludingDenomination();
            tvCurrency.setText(currentCurrency);
        }
    }

    protected void showFiat() {
        if (hideOnNoExchangeRate && !currencySwitcher.isFiatExchangeRateAvailable()) {
            // hide everything
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);

            // convert to the target fiat currency, if needed
            Value value = currencySwitcher.getAsFiatValue(currentValue);

            tvCurrency.setText(currencySwitcher.getCurrentFiatCurrency().getSymbol());
            tvValue.setText(value != null ? ValueExtensionsKt.toString(value) : null);
        }
    }

    private boolean isAddedToBus = false;
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAddedToBus = true;
        MbwManager.getEventBus().register(this);
        if(currencySwitcher != null) {
            updateUi();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // unregister from the event bus
        if (isAddedToBus) {
            MbwManager.getEventBus().unregister(this);
            isAddedToBus = false;
        }
    }

    public void setCurrencySwitcher(CurrencySwitcher currencySwitcher) {
        this.currencySwitcher = currencySwitcher;
        updateUi();
    }

    public void setValue(Value value) {
        this.currentValue = value;
        updateUi();
    }

    public void setValue(ValueSum sum) {
        this.currentValue = currencySwitcher.getValue(sum);
        updateUi();
    }

    public void setFiatOnly(boolean fiatOnly) {
        this.fiatOnly = fiatOnly;
    }

    @Subscribe
    public void onExchangeRateChange(ExchangeRatesRefreshed event) {
        updateUi();
    }

    @Subscribe
    public void onSelectedCurrencyChange(SelectedCurrencyChanged event) {
        updateUi();
    }

}
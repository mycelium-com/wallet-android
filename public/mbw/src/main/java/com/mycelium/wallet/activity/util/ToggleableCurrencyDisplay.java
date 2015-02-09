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
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;


public class ToggleableCurrencyDisplay extends LinearLayout {
   private Bus eventBus = null;
   private CurrencySwitcher currencySwitcher;

   private TextView tvCurrency;
   private TextView tvValue;
   private LinearLayout llContainer;

   private long satoshis;
   private boolean fiatOnly = false;
   private boolean hideOnNoExchangeRate = false;

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

   void parseXML(Context context, AttributeSet attrs){
      TypedArray a = context.obtainStyledAttributes(attrs,
            R.styleable.ToggleableCurrencyDisplay);

      final int N = a.getIndexCount();
      for (int i = 0; i < N; ++i)
      {
         int attr = a.getIndex(i);
         switch (attr)
         {
            case R.styleable.ToggleableCurrencyDisplay_fiatOnly:
               fiatOnly = a.getBoolean(attr, false);
               break;
            case R.styleable.ToggleableCurrencyDisplay_textSize:
               setTextSize(a.getDimensionPixelSize(attr, 12));
               break;
            case R.styleable.ToggleableCurrencyDisplay_textColor:
               setTextColor(a.getColor(attr, R.color.lightgrey));
               break;
            case R.styleable.ToggleableCurrencyDisplay_hideOnNoExchangeRate:
               hideOnNoExchangeRate = a.getBoolean(attr, false);
         }
      }
      a.recycle();

   }

   void init(Context context){
      LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      View view;
      view = mInflater.inflate(R.layout.toggleable_currency_display, this, true);

      tvCurrency = (TextView) view.findViewById(R.id.tvCurrency);
      tvValue = (TextView) view.findViewById(R.id.tvValue);
      llContainer = (LinearLayout) view.findViewById(R.id.llContainer);

      llContainer.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            switchToNextCurrency();
         }
      });
   }

   private void setTextSize(int size){
      tvCurrency.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
      tvValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
   }

   private void setTextColor(int color){
      tvCurrency.setTextColor(color);
      tvValue.setTextColor(color);
   }

   private void updateUi(){
      Preconditions.checkNotNull(currencySwitcher);

      int cntCurrencies = (fiatOnly ? currencySwitcher.getFiatCurrenciesCount() : currencySwitcher.getCurrenciesCount() );
      if (cntCurrencies == 1){
         // there is only one currency to show - dont show a triangle hinting that the user can toggle
         findViewById(R.id.ivSwitchable).setVisibility(INVISIBLE);
      } else {
         // there are more than one fiat-currency
         findViewById(R.id.ivSwitchable).setVisibility(VISIBLE);
      }

      if (fiatOnly) {
         showFiat();
      } else {
         if (!currencySwitcher.hasFiatCurrencyExchangeRate()) {
            currencySwitcher.setCurrency(CurrencySwitcher.BTC);
         }

         if (currencySwitcher.getCurrentCurrency().equals(CurrencySwitcher.BTC)){
            llContainer.setVisibility(VISIBLE);
            tvValue.setText(currencySwitcher.getBtcValueString(satoshis, false));
            tvCurrency.setText(currencySwitcher.getBitcoinDenomination().getUnicodeName());
         }else {
            showFiat();
         }
      }
   }

   private void showFiat() {
      if (hideOnNoExchangeRate && !currencySwitcher.isFiatExchangeRateAvailable()){
         llContainer.setVisibility(GONE);
      } else {
         llContainer.setVisibility(VISIBLE);
         tvCurrency.setText(currencySwitcher.getCurrentFiatCurrency());
         tvValue.setText(currencySwitcher.getFormattedFiatValue(satoshis, false));
      }
   }

   public void setEventBus(Bus eventBus){
      this.eventBus = eventBus;

      //todo: unregister?
      this.eventBus.register(this);
   }

   public void setCurrencySwitcher(CurrencySwitcher currencySwitcher){
      this.currencySwitcher = currencySwitcher;
      updateUi();
   }

   public void setValue(long satoshis){
      this.satoshis = satoshis;
      updateUi();
   }

   public void switchToNextCurrency(){
      String nextCurrency = Preconditions.checkNotNull(this.currencySwitcher).getNextCurrency(!fiatOnly);
      if (eventBus != null){
         // update UI via event bus, also inform other parts of the app about the change
         eventBus.post(new SelectedCurrencyChanged());
      } else {
         updateUi();
      }
   }

   @Subscribe
   public void onExchangeRateChange(ExchangeRatesRefreshed event){
      updateUi();
   }

   @Subscribe
   public void onSelectedCurrencyChange(SelectedCurrencyChanged event){
      updateUi();
   }

}

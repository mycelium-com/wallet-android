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
import android.util.AttributeSet;
import android.view.View;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactFiatValue;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;


public class ToggleableCurrencyButton extends ToggleableCurrencyDisplay {

   public ToggleableCurrencyButton(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   public ToggleableCurrencyButton(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
   }

   public ToggleableCurrencyButton(Context context) {
      super(context);
   }

   @Override
   protected void init(Context context){
      super.init(context);

      llContainer.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            switchToNextCurrency();
         }
      });
   }


   @Override
   protected void updateUi(){
      super.updateUi();

      int cntCurrencies = (fiatOnly ? currencySwitcher.getFiatCurrenciesCount() : currencySwitcher.getCurrenciesCount() );
      if (cntCurrencies == 1){
         // there is only one currency to show - dont show a triangle hinting that the user can toggle
         findViewById(R.id.ivSwitchable).setVisibility(INVISIBLE);
      } else {
         // there are more than one fiat-currency
         findViewById(R.id.ivSwitchable).setVisibility(VISIBLE);
      }

   }

   @Override
   protected void showFiat() {
      if(currentValue != null && currentValue.getCurrency().equals(ColuAccount.ColuAssetType.RMC.toString())){
         llContainer.setVisibility(VISIBLE);
         tvValue.setText(currentValue.getValue().multiply(BigDecimal.valueOf(4000)).stripTrailingZeros().toPlainString());
         tvCurrency.setText("USD");
      }else{
         super.showFiat();
      }
   }

   @Override
   protected CurrencyValue getValueToShow() {
      if(currentValue.getCurrency().equals(ColuAccount.ColuAssetType.RMC.toString())){
         return new ExactFiatValue(currentValue.getValue().multiply(BigDecimal.valueOf(4000)), "USD");
      }else {
         return super.getValueToShow();
      }
   }

   public void switchToNextCurrency(){
      Preconditions.checkNotNull(this.currencySwitcher).getNextCurrency(!fiatOnly);
      if (eventBus != null){
         // update UI via event bus, also inform other parts of the app about the change
         eventBus.post(new SelectedCurrencyChanged());
      } else {
         updateUi();
      }
   }


   @Subscribe
   @Override
   public void onExchangeRateChange(ExchangeRatesRefreshed event){
      updateUi();
   }

   @Subscribe
   @Override
   public void onSelectedCurrencyChange(SelectedCurrencyChanged event){
      updateUi();
   }
}

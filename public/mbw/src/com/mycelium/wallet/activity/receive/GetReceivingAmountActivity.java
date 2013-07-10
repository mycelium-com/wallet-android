/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mycelium.wallet.activity.receive;

import java.math.BigDecimal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ExchangeSummary;

public class GetReceivingAmountActivity extends Activity implements NumberEntryListener {

   private AsyncTask _task;
   private NumberEntry _numberEntry;
   private MbwManager _mbwManager;
   private boolean _enterFiatAmount;
   private Double _oneBtcInFiat;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_receiving_amount_activity);

      _mbwManager = MbwManager.getInstance(getApplication());

      // Load saved state
      Long amount = null;
      if (savedInstanceState != null) {
         Long savedAmount = (Long) savedInstanceState.getSerializable("amount");
         if (savedAmount != null) {
            amount = savedAmount;
         }
      }

      _mbwManager = MbwManager.getInstance(getApplication());

      // Set amount
      String amountString;
      if (amount != null) {
         amountString = CoinUtil.valueString(amount);
         ((TextView) findViewById(R.id.tvAmount)).setText(amountString);
      } else {
         amountString = "";
      }
      ((TextView) findViewById(R.id.tvAmount)).setText(amountString);
      _numberEntry = new NumberEntry(8, this, this, amountString);

      // Make both button and entire info box at top a listener to switch
      // currency
      Button btCurrency = (Button) findViewById(R.id.btCurrency);
      btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
      btCurrency.setEnabled(false);
      btCurrency.setOnClickListener(switchCurrencyListener);
      findViewById(R.id.llInfo).setOnClickListener(switchCurrencyListener);

      findViewById(R.id.btNext).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            findViewById(R.id.btNext).setEnabled(false);
            Intent intent = new Intent(GetReceivingAmountActivity.this, ReceiveCoinsActivity.class);
            intent.putExtra("amount", getSatoshisToReceive());
            startActivity(intent);
            finish();
         }
      });

      checkEntry();

      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("amount", getSatoshisToReceive());
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   private final OnClickListener switchCurrencyListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         if (_oneBtcInFiat == null) {
            // We cannot switch to fiat as we do not know the exchange rate
            return;
         }
         switchCurrency();
      }
   };

   public void switchCurrency() {
      int newDecimalPlaces;
      BigDecimal newAmount;
      if (_enterFiatAmount) {
         // We are switching from Fiat to BTC

         // Set BTC button
         Button btCurrency = (Button) findViewById(R.id.btCurrency);
         btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());

         newDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         Long satoshis = getSatoshisToReceive();
         if (satoshis == null) {
            newAmount = null;
         } else {
            newAmount = BigDecimal.valueOf(satoshis).divide(BigDecimal.TEN.pow(newDecimalPlaces));
         }
      } else {
         // We are switching from BTC to Fiat

         // Set Fiat button
         Button btCurrency = (Button) findViewById(R.id.btCurrency);
         btCurrency.setText(_mbwManager.getFiatCurrency());

         newDecimalPlaces = 2;
         Long fiatCents = getFiatCentsToReceive();
         if (fiatCents == null) {
            newAmount = null;
         } else {
            newAmount = BigDecimal.valueOf(fiatCents).divide(BigDecimal.TEN.pow(newDecimalPlaces));
         }
      }
      // Note: Do the boolean switch before updating numberEntry, as there is
      // feedback from numberEntry back to ourselves
      _enterFiatAmount = !_enterFiatAmount;
      _numberEntry.setEntry(newAmount, newDecimalPlaces);
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   @Override
   public void onEntryChanged(String entry) {
      ((TextView) findViewById(R.id.tvAmount)).setText(entry);
      updateAmounts(entry);
      checkEntry();
   }

   private void updateAmounts(String amountText) {
      ((TextView) findViewById(R.id.tvAmount)).setText(amountText);
      TextView tvAlternateAmount = ((TextView) findViewById(R.id.tvAlternateAmount));
      Long satoshis = getSatoshisToReceive();

      // Set alternate amount if we can
      if (satoshis == null || _oneBtcInFiat == null) {
         tvAlternateAmount.setText("");
      } else {
         if (_enterFiatAmount) {
            // Show BTC as alternate amount
            tvAlternateAmount.setText(_mbwManager.getBtcValueString(satoshis));
         } else {
            // Show Fiat as alternate amount
            Double converted = Utils.getFiatValue(satoshis, _oneBtcInFiat);
            String currency = MbwManager.getInstance(getApplication()).getFiatCurrency();
            tvAlternateAmount.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
         }
      }
   }

   private void checkEntry() {
      Long satoshis = getSatoshisToReceive();
      if (satoshis == null || satoshis == 0) {
         // Nothing entered
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
         findViewById(R.id.btNext).setEnabled(false);
         return;
      }

      // Enable/disable Next button
      findViewById(R.id.btNext).setEnabled(satoshis > 0);
   }

   private Long getFiatCentsToReceive() {
      Double fiatAmount;
      BigDecimal entry = _numberEntry.getEntryAsBigDecimal();
      if (entry == null) {
         return null;
      }
      if (_enterFiatAmount) {
         fiatAmount = entry.doubleValue();
      } else {
         int decimals = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         Long satoshis = entry.movePointRight(decimals).longValue();
         fiatAmount = Utils.getFiatValue(satoshis, _oneBtcInFiat);
      }
      Double fiatCents = fiatAmount * 100;
      return fiatCents.longValue();
   }

   private Long getSatoshisToReceive() {
      BigDecimal entry = _numberEntry.getEntryAsBigDecimal();
      if (entry == null) {
         return null;
      }
      if (_enterFiatAmount) {
         return Utils.getSatoshis(entry, _oneBtcInFiat);
      } else {
         int decimals = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         Long satoshis = entry.movePointRight(decimals).longValue();
         return satoshis;
      }
   }

   class QueryExchangeSummaryHandler implements AbstractCallbackHandler<ExchangeSummary[]> {

      @Override
      public void handleCallback(ExchangeSummary[] response, ApiError exception) {
         if (exception != null) {
            Utils.toastConnectionError(GetReceivingAmountActivity.this);
            _task = null;
            _oneBtcInFiat = null;
         } else {
            _oneBtcInFiat = Utils.getLastTrade(response);
            findViewById(R.id.btCurrency).setEnabled(true);
            updateAmounts(_numberEntry.getEntry());
            _task = null;
         }
      }

   }

}
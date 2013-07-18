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

package com.mycelium.wallet.activity.send;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.SendActivityHelper.SendContext;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;

public class GetSendingAmountActivity extends Activity implements NumberEntryListener {

   private AsyncTask _task;
   private NumberEntry _numberEntry;
   private PrivateKeyRing _privateKeyRing;
   private long _balance;
   private Toast _toast;
   private boolean _enterFiatAmount;
   private MbwManager _mbwManager;
   private Double _oneBtcInFiat;
   private SendContext _context;
   private List<UnspentTransactionOutput> _outputs;
   private long _maxSendable;

   /** Called when the activity is first created. */
   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_sending_amount_activity);
      _toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _context = SendActivityHelper.getSendContext(this);

      // Get intent parameters
      Long amount = null;

      // Load saved state
      if (savedInstanceState != null) {
         amount = (Long) savedInstanceState.getSerializable("amount");
      }

      // Construct list of outputs
      _outputs = new LinkedList<UnspentTransactionOutput>();
      _outputs.addAll(_context.spendableOutputs.unspent);
      _outputs.addAll(_context.spendableOutputs.change);

      // Construct private key ring
      _privateKeyRing = _context.wallet.getPrivateKeyRing();

      // Determine and set balance
      _balance = 0;
      for (UnspentTransactionOutput out : _outputs) {
         _balance += out.value;
      }
      ((TextView) findViewById(R.id.tvMaxAmount)).setText(getBalanceString(_balance));

      // Calculate the maximum amount we can send
      _maxSendable = getMaxAmount();

      // Set amount
      String amountString;
      if (amount != null) {
         amountString = CoinUtil.valueString(amount, _mbwManager.getBitcoinDenomination());
      } else {
         amountString = "";
      }
      TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
      tvAmount.setText(amountString);

      _numberEntry = new NumberEntry(_mbwManager.getBitcoinDenomination().getDecimalPlaces(), this, this, amountString);
      checkTransaction();

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
            SendActivityHelper.startNextActivity(GetSendingAmountActivity.this, getSatoshisToSend());
         }
      });

      findViewById(R.id.btMax).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            maximizeAmount();
         }
      });

      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());
   }

   private String getBalanceString(long balance) {
      String balanceString = _mbwManager.getBtcValueString(balance);
      String balanceText = getResources().getString(R.string.max_btc, balanceString);
      return balanceText;
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

   private void switchCurrency() {
      int newDecimalPlaces;
      BigDecimal newAmount;
      if (_enterFiatAmount) {
         // We are switching from Fiat to BTC

         // Set BTC button
         Button btCurrency = (Button) findViewById(R.id.btCurrency);
         btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());

         // Set BTC balance
         ((TextView) findViewById(R.id.tvMaxAmount)).setText(getBalanceString(_balance));

         newDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         Long satoshis = getSatoshisToSend();
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

         // Set Fiat balance
         Double fiatBalance = Utils.getFiatValue(_balance, _oneBtcInFiat);
         String balanceString = getResources().getString(R.string.max_fiat, fiatBalance, _mbwManager.getFiatCurrency());
         ((TextView) findViewById(R.id.tvMaxAmount)).setText(balanceString);

         newDecimalPlaces = 2;
         Long fiatCents = getFiatCentsToSend();
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

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("amount", getSatoshisToSend());
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

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   @Override
   public void onEntryChanged(String entry) {
      updateAmounts(entry);
      checkTransaction();
   }

   private void updateAmounts(String amountText) {
      ((TextView) findViewById(R.id.tvAmount)).setText(amountText);
      TextView tvAlternateAmount = ((TextView) findViewById(R.id.tvAlternateAmount));
      Long satoshis = getSatoshisToSend();

      // enable/disable Max button
      findViewById(R.id.btMax).setEnabled(satoshis == null || _maxSendable != satoshis);
      
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

   private void maximizeAmount() {
      if (_maxSendable == 0) {
         String msg = getResources().getString(R.string.insufficient_funds);
         _toast.setText(msg);
         _toast.show();
      } else {
         if (_enterFiatAmount) {
            switchCurrency();
         }
         int newDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         BigDecimal newAmount = BigDecimal.valueOf(_maxSendable).divide(BigDecimal.TEN.pow(newDecimalPlaces));
         _numberEntry.setEntry(newAmount, newDecimalPlaces);
      }
   }

   private long getMaxAmount() {
      long satoshis = _balance;
      while (true) {
         satoshis -= StandardTransactionBuilder.MINIMUM_MINER_FEE;
         AmountValidation result = checkSendAmount(satoshis);
         if (result == AmountValidation.Ok) {
            return satoshis;
         } else if (result == AmountValidation.ValueTooSmall) {
            return 0;
         }

      }
   }

   private enum AmountValidation {
      Ok, ValueTooSmall, NotEnoughFunds
   };

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   private AmountValidation checkSendAmount(long satoshis) {
      // Create transaction builder
      StandardTransactionBuilder stb = new StandardTransactionBuilder(Constants.network);

      // Try and add the output
      try {
         stb.addOutput(_context.receivingAddress, satoshis);
      } catch (OutputTooSmallException e1) {
         return AmountValidation.ValueTooSmall;
      }

      // Try to create an unsigned transaction
      try {
         stb.createUnsignedTransaction(_outputs, _context.wallet.getReceivingAddress(), _privateKeyRing,
               Constants.network);
      } catch (InsufficientFundsException e) {
         return AmountValidation.NotEnoughFunds;
      }
      return AmountValidation.Ok;
   }

   private void checkTransaction() {
      Long satoshis = getSatoshisToSend();
      if (satoshis == null) {
         // Nothing entered
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
         findViewById(R.id.btNext).setEnabled(false);
         return;
      }

      // Check whether we have sufficient funds, and whether the output is too
      // small
      AmountValidation result = checkSendAmount(satoshis);

      if (result == AmountValidation.Ok) {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
      } else {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.red));
         if (result == AmountValidation.NotEnoughFunds) {
            // We do not have enough funds
            if (_balance < satoshis) {
               // We do not have enough funds for sending the requested amount
               String msg = getResources().getString(R.string.insufficient_funds);
               _toast.setText(msg);
            } else {
               // We do have enough funds for sending the requested amount, but
               // not for the required fee
               String msg = getResources().getString(R.string.insufficient_funds_for_fee);
               _toast.setText(msg);
            }
            _toast.show();
         } else {
            // The amount we want to send is not large enough for the network to
            // accept it. Don't Toast about it, it's just annoying
         }
      }

      // Enable/disable Next button
      findViewById(R.id.btNext).setEnabled(result != AmountValidation.NotEnoughFunds && satoshis > 0);
   }

   private Long getFiatCentsToSend() {
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

   private Long getSatoshisToSend() {
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
            Utils.toastConnectionError(GetSendingAmountActivity.this);
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
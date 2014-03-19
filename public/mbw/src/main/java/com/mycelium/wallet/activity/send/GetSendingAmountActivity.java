/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.activity.send;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.TransactionUtils;
import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;

public class GetSendingAmountActivity extends Activity implements NumberEntryListener {

   private Wallet _wallet;
   private SpendableOutputs _spendable;
   private Double _oneBtcInFiat; // May be null
   private NumberEntry _numberEntry;
   private PrivateKeyRing _privateKeyRing;
   private long _balance;
   private Toast _toast;
   private boolean _enterFiatAmount;
   private MbwManager _mbwManager;
   private List<UnspentTransactionOutput> _outputs;
   private long _maxSendable;

   public static void callMe(Activity currentActivity, int requestCode, Wallet wallet, SpendableOutputs spendable,
         Double oneBtcInFiat, Long amountToSend) {
      Intent intent = new Intent(currentActivity, GetSendingAmountActivity.class);
      intent.putExtra("wallet", wallet);
      intent.putExtra("spendable", spendable);
      intent.putExtra("oneBtcInFiat", oneBtcInFiat);
      intent.putExtra("amountToSend", amountToSend);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_sending_amount_activity);
      _toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
      _mbwManager = MbwManager.getInstance(getApplication());

      // Get intent parameters
      _wallet = Preconditions.checkNotNull((Wallet) getIntent().getSerializableExtra("wallet"));
      _spendable =  Preconditions.checkNotNull((SpendableOutputs) getIntent().getSerializableExtra("spendable"));
      // May be null
      _oneBtcInFiat =  (Double) getIntent().getSerializableExtra("oneBtcInFiat");
      Long amount = (Long) getIntent().getSerializableExtra("amountToSend");

      // Load saved state
      if (savedInstanceState != null) {
         amount = (Long) savedInstanceState.getSerializable("amountToSend");
      }

      // Construct list of outputs
      _outputs = new LinkedList<UnspentTransactionOutput>();
      _outputs.addAll(_spendable.unspent);
      _outputs.addAll(_spendable.change);

      // Construct private key ring
      _privateKeyRing = _wallet.getPrivateKeyRing();

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
         amountString = CoinUtil.valueString(amount, _mbwManager.getBitcoinDenomination(), false);
      } else {
         amountString = "";
      }
      TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
      tvAmount.setText(amountString);

      _numberEntry = new NumberEntry(_mbwManager.getBitcoinDenomination().getDecimalPlaces(), this, this, amountString);
      checkTransaction();

      // Make both currency button and invisible right button at top a listener
      // switch currency
      Button btCurrency = (Button) findViewById(R.id.btCurrency);
      btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
      btCurrency.setEnabled(_oneBtcInFiat != null);
      btCurrency.setOnClickListener(switchCurrencyListener);
      findViewById(R.id.btRight).setOnClickListener(switchCurrencyListener);

      // Make both paste button and invisible left button at top a listener to
      // paste from clipboard
      Button btPaste = (Button) findViewById(R.id.btPaste);
      btPaste.setOnClickListener(pasteListener);
      findViewById(R.id.btLeft).setOnClickListener(pasteListener);

      // Next Button
      findViewById(R.id.btOk).setOnClickListener(okClickListener);

      // Max Button
      findViewById(R.id.btMax).setOnClickListener(maxClickListener);

   }

   private OnClickListener okClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         // Return the number of satoshis to send
         Intent result = new Intent();
         result.putExtra("amountToSend", getSatoshisToSend());
         setResult(RESULT_OK, result);
         GetSendingAmountActivity.this.finish();
      }
   };

   private OnClickListener maxClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         maximizeAmount();
      }
   };

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

   private final OnClickListener pasteListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         BigDecimal clipboardValue = getAmountFromClipboard();
         if (clipboardValue == null) {
            return;
         }
         _numberEntry.setEntry(clipboardValue, _mbwManager.getBitcoinDenomination().getDecimalPlaces());
      }
   };

   private boolean enablePaste() {
      return getAmountFromClipboard() != null;
   }

   private BigDecimal getAmountFromClipboard() {
      String content = Utils.getClipboardString(GetSendingAmountActivity.this);
      if (content.length() == 0) {
         return null;
      }
      String number = content.toString().trim();
      if (_enterFiatAmount) {
         number = Utils.truncateAndConvertDecimalString(number, 2);
         if (number == null) {
            return null;
         }
         BigDecimal value = new BigDecimal(number);
         if (value.compareTo(BigDecimal.ZERO) < 1) {
            return null;
         }
         return value;
      } else {
         number = Utils
               .truncateAndConvertDecimalString(number, _mbwManager.getBitcoinDenomination().getDecimalPlaces());
         if (number == null) {
            return null;
         }
         BigDecimal value = new BigDecimal(number);
         if (value.compareTo(BigDecimal.ZERO) < 1) {
            return null;
         }
         return value;
      }
   }

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
         String fiatBalance = Utils.getFiatValueAsString(_balance, _oneBtcInFiat);
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

      // Check whether we can enable the paste button
      findViewById(R.id.btPaste).setEnabled(enablePaste());
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("amountToSend", getSatoshisToSend());
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      findViewById(R.id.btPaste).setEnabled(enablePaste());
      super.onResume();
   }

   private void cancelEverything() {
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
            String converted = Utils.getFiatValueAsString(satoshis, _oneBtcInFiat);
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
         satoshis -= TransactionUtils.DEFAULT_MINER_FEE;
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
   }

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   private AmountValidation checkSendAmount(long satoshis) {
      // Create transaction builder
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_mbwManager.getNetwork());

      // Try and add the output
      try {
         // Note, null address used here, we just use it for measuring the
         // transaction size
         stb.addOutput(Address.getNullAddress(_mbwManager.getNetwork()), satoshis);
      } catch (OutputTooSmallException e1) {
         return AmountValidation.ValueTooSmall;
      }

      // Try to create an unsigned transaction
      try {
         stb.createUnsignedTransaction(_outputs, null, _privateKeyRing, _mbwManager.getNetwork());
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
         findViewById(R.id.btOk).setEnabled(false);
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
      findViewById(R.id.btOk).setEnabled(result == AmountValidation.Ok && satoshis > 0);
   }

   // todo de-duplicate code
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

}
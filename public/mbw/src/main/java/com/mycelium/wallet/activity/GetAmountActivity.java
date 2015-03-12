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

package com.mycelium.wallet.activity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

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
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Subscribe;


public class GetAmountActivity extends Activity implements NumberEntryListener {

   public static final String AMOUNT = "amount";
   public static final String ENTEREDAMOUNT = "enteredamount";
   public static final String SELECTEDCURRENCY = "selectedcurrency";
   public static final String ENTEREDCURRENCY = "enteredcurrency";
   public static final String ONEBTCINFIAT = "onebtcinfiat";
   private boolean isSendMode;

   private WalletAccount _account;
   private Double _oneBtcInFiat;
   private NumberEntry _numberEntry;
   private String _selectedCurrency;
   private String _enteredCurrency;
   private BigDecimal _enteredAmount;
   private long _satoshis;
   private MbwManager _mbwManager;
   private long _maxSpendableAmount;
   private long _kbMinerFee;

   public static void callMe(Activity currentActivity, int requestCode, UUID account, Long amountToSend, Long kbMinerFee, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class);
      intent.putExtra("account", account);
      intent.putExtra("amount", amountToSend);
      intent.putExtra("kbMinerFee", kbMinerFee);
      intent.putExtra("isColdStorage", isColdStorage);
      intent.putExtra("sendmode", true);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Activity currentActivity, Long amount, int requestCode) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class);
      intent.putExtra("amount", amount);
      intent.putExtra("sendmode", false);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_amount_activity);
      _mbwManager = MbwManager.getInstance(getApplication());

      initNumberEntry(savedInstanceState);

      isSendMode = getIntent().getBooleanExtra("sendmode", false);
      if (isSendMode) {
         initSendMode();
      }

      initListeners();
      updateUI();
      checkEntry();
   }

   private void initSendMode() {
      //we need to have an account, fee, etc to be able to calculate sending related stuff
      boolean isColdStorage = getIntent().getBooleanExtra("isColdStorage", false);
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra("account"));
      _account = _mbwManager.getWalletManager(isColdStorage).getAccount(accountId);

      // Calculate the maximum amount that can be spent where we send everything we got to another address
      _kbMinerFee = Preconditions.checkNotNull((Long) getIntent().getSerializableExtra("kbMinerFee"));
      _maxSpendableAmount = _account.calculateMaxSpendableAmount(_kbMinerFee);
      ((TextView) findViewById(R.id.tvMaxAmount)).setText(getBalanceString(_account.getBalance()));

      // Max Button
      findViewById(R.id.btMax).setOnClickListener(maxClickListener);

      findViewById(R.id.tvMaxAmount).setVisibility(View.VISIBLE);
      findViewById(R.id.btMax).setVisibility(View.VISIBLE);
   }

   private void initListeners() {
      // Make both currency button and invisible right button at top a listener
      // switch currency
      Button btCurrency = (Button) findViewById(R.id.btCurrency);
      btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
      btCurrency.setEnabled(_mbwManager.getCurrencySwitcher().getExchangeRatePrice() != null);
      btCurrency.setOnClickListener(switchCurrencyListener);
      findViewById(R.id.btRight).setOnClickListener(switchCurrencyListener);

      // Make both paste button and invisible left button at top a listener to
      // paste from clipboard
      Button btPaste = (Button) findViewById(R.id.btPaste);
      btPaste.setOnClickListener(pasteListener);
      findViewById(R.id.btLeft).setOnClickListener(pasteListener);

      // Next Button
      findViewById(R.id.btOk).setOnClickListener(okClickListener);
   }

   private void initNumberEntry(Bundle savedInstanceState) {
      Long amount = (Long) getIntent().getSerializableExtra("amount");
      _selectedCurrency = "BTC";
      // Load saved state
      if (savedInstanceState != null) {
         amount = (Long) savedInstanceState.getSerializable(AMOUNT);
         _selectedCurrency = savedInstanceState.getString(SELECTEDCURRENCY);
         _enteredAmount = (BigDecimal) savedInstanceState.getSerializable(ENTEREDAMOUNT);
         _enteredCurrency = savedInstanceState.getString(ENTEREDCURRENCY);
         _oneBtcInFiat = (Double) savedInstanceState.getSerializable(ONEBTCINFIAT);
      }
      // Set amount
      String amountString;
      if (amount != null) {
         _satoshis = amount;
         amountString = CoinUtil.valueString(amount, _mbwManager.getBitcoinDenomination(), false);
      } else {
         _satoshis = 0;
         amountString = "";
      }
      TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
      tvAmount.setText(amountString);

      _numberEntry = new NumberEntry(_mbwManager.getBitcoinDenomination().getDecimalPlaces(), this, this, amountString);
   }

   private OnClickListener okClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         // Return the number of satoshis
         Intent result = new Intent();
         result.putExtra("amount", _satoshis);
         setResult(RESULT_OK, result);
         GetAmountActivity.this.finish();
      }
   };

   private OnClickListener maxClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         maximizeAmount();
      }
   };

   private String getBalanceString(com.mycelium.wapi.model.Balance balance) {
      String balanceString = _mbwManager.getBtcValueString(balance.getSpendableBalance());
      return getResources().getString(R.string.max_btc, balanceString);
   }

   private final OnClickListener switchCurrencyListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
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
         setEnteredAmount(clipboardValue);
         _numberEntry.setEntry(clipboardValue, _mbwManager.getBitcoinDenomination().getDecimalPlaces());
      }
   };

   private boolean enablePaste() {
      return getAmountFromClipboard() != null;
   }

   private BigDecimal getAmountFromClipboard() {
      String content = Utils.getClipboardString(GetAmountActivity.this);
      if (content.length() == 0) {
         return null;
      }
      String number = content.trim();
      if (_selectedCurrency.equals("BTC")) {
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
      } else {
         number = Utils.truncateAndConvertDecimalString(number, 2);
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

   private void switchToBtc() {
      _selectedCurrency = "BTC";
      updateUI();
   }


   private void switchCurrency() {
      _selectedCurrency = _mbwManager.getNextCurrency(true);

      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      //if the price is not available, switch on -> no point in showing it
      if (_oneBtcInFiat == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
         switchCurrency();
         return;
      }
      updateUI();
   }

   private void updateUI() {
      //update buttons and views
      if (isBtc()) {
         // Set BTC button
         Button btCurrency = (Button) findViewById(R.id.btCurrency);
         btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
         if (isSendMode) {
            // Set BTC balance
            ((TextView) findViewById(R.id.tvMaxAmount)).setText(getBalanceString(_account.getBalance()));
         }
      } else {
         // Set Fiat button
         Button btCurrency = (Button) findViewById(R.id.btCurrency);
         btCurrency.setText(_selectedCurrency);
         if (isSendMode) {
            // Set Fiat balance
            String fiatBalance = Utils.getFiatValueAsString(_account.getBalance().getSpendableBalance(), _oneBtcInFiat);
            String balanceString = getResources().getString(R.string.max_fiat, fiatBalance, _selectedCurrency);
            ((TextView) findViewById(R.id.tvMaxAmount)).setText(balanceString);
         }
      }
      //update amount
      int newDecimalPlaces;
      BigDecimal newAmount;
      if (isBtc()) {
         //just good ol bitcoins
         newDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         newAmount = BigDecimal.valueOf(_satoshis).divide(BigDecimal.TEN.pow(newDecimalPlaces));
      } else if (_selectedCurrency.equals(_enteredCurrency)) {
         //take what was typed in
         newDecimalPlaces = 2;
         newAmount = _enteredAmount;
      } else {
         //convert to that currency
         newDecimalPlaces = 2;
         newAmount = BigDecimal.valueOf(Utils.getFiatValue(_satoshis, _oneBtcInFiat));
      }
      _numberEntry.setEntry(newAmount, newDecimalPlaces);
      // Check whether we can show the paste button
      findViewById(R.id.btPaste).setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(AMOUNT, _satoshis);
      savedInstanceState.putSerializable(ENTEREDAMOUNT, _enteredAmount);
      savedInstanceState.putString(SELECTEDCURRENCY, _selectedCurrency);
      savedInstanceState.putString(ENTEREDCURRENCY, _enteredCurrency);
      savedInstanceState.putSerializable(ONEBTCINFIAT, _oneBtcInFiat);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);
      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      if (_oneBtcInFiat == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }
      findViewById(R.id.btCurrency).setEnabled(_mbwManager.hasFiatCurrency() && _oneBtcInFiat != null);
      findViewById(R.id.btPaste).setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   public void onEntryChanged(String entry, boolean wasSet) {
      if (!wasSet) {
         BigDecimal value = _numberEntry.getEntryAsBigDecimal();
         setEnteredAmount(value);
      }
      updateAmounts(entry);
      checkEntry();
   }

   private void setEnteredAmount(BigDecimal value) {
      _enteredAmount = value;
      _enteredCurrency = _selectedCurrency;
      if (isBtc()) {
         int decimals = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         _satoshis = value.movePointRight(decimals).longValue();
      } else {
         _satoshis = Utils.getSatoshis(value, _oneBtcInFiat);
      }
      if (isSendMode) {
         // enable/disable Max button
         findViewById(R.id.btMax).setEnabled(_maxSpendableAmount != _satoshis);
      }
   }

   private boolean isBtc() {
      return _selectedCurrency.equals("BTC");
   }

   private void updateAmounts(String amountText) {
      ((TextView) findViewById(R.id.tvAmount)).setText(amountText);
      TextView tvAlternateAmount = ((TextView) findViewById(R.id.tvAlternateAmount));

      // Set alternate amount if we can
      if (!_mbwManager.hasFiatCurrency() || _oneBtcInFiat == null) {
         tvAlternateAmount.setText("");
      } else {
         if (isBtc()) {
            // Show Fiat as alternate amount
            String converted = Utils.getFiatValueAsString(_satoshis, _oneBtcInFiat);
            String currency = MbwManager.getInstance(getApplication()).getFiatCurrency();
            tvAlternateAmount.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
         } else {
            // Show BTC as alternate amount
            tvAlternateAmount.setText(_mbwManager.getBtcValueString(_satoshis));
         }
      }
   }

   private void maximizeAmount() {
      if (_maxSpendableAmount == 0) {
         String msg = getResources().getString(R.string.insufficient_funds);
         Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
      } else {
         if (!isBtc()) switchToBtc();
         int newDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         BigDecimal newAmount = BigDecimal.valueOf(_maxSpendableAmount).divide(BigDecimal.TEN.pow(newDecimalPlaces));
         _numberEntry.setEntry(newAmount, newDecimalPlaces);
         _enteredAmount = BigDecimal.valueOf(_maxSpendableAmount);
         _satoshis = _maxSpendableAmount;
         _enteredCurrency = "BTC";
         updateUI();
         checkEntry();
      }
   }

   private void checkEntry() {
      if (_satoshis == 0) {
         // Nothing entered
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
         findViewById(R.id.btOk).setEnabled(false);
         return;
      }
      if (isSendMode) {
         AmountValidation result = checkTransaction();
         // Enable/disable Next button
         findViewById(R.id.btOk).setEnabled(result == AmountValidation.Ok && _satoshis > 0);
      } else {
         findViewById(R.id.btOk).setEnabled(true);
      }
   }

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   private AmountValidation checkSendAmount(long satoshis) {
      try {
         WalletAccount.Receiver receiver = new WalletAccount.Receiver(Address.getNullAddress(_mbwManager.getNetwork()), satoshis);
         _account.createUnsignedTransaction(Arrays.asList(receiver), _kbMinerFee);
      } catch (OutputTooSmallException e1) {
         return AmountValidation.ValueTooSmall;
      } catch (InsufficientFundsException e) {
         return AmountValidation.NotEnoughFunds;
      }
      return AmountValidation.Ok;
   }

   private enum AmountValidation {
      Ok, ValueTooSmall, NotEnoughFunds
   }

   private AmountValidation checkTransaction() {
      // Check whether we have sufficient funds, and whether the output is too small
      AmountValidation result = checkSendAmount(_satoshis);

      if (result == AmountValidation.Ok) {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
      } else {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.red));
         if (result == AmountValidation.NotEnoughFunds) {
            // We do not have enough funds
            if (_account.getBalance().getSpendableBalance() < _satoshis) {
               // We do not have enough funds for sending the requested amount
               String msg = getResources().getString(R.string.insufficient_funds);
               Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } else {
               // We do have enough funds for sending the requested amount, but
               // not for the required fee
               String msg = getResources().getString(R.string.insufficient_funds_for_fee);
               Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
         } else {
            // The amount we want to send is not large enough for the network to
            // accept it. Don't Toast about it, it's just annoying
         }
      }
      return result;
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event){
      updateExchangeRateDisplay();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event){
      updateExchangeRateDisplay();
   }

   private void updateExchangeRateDisplay() {
      _oneBtcInFiat = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      findViewById(R.id.btCurrency).setEnabled(_oneBtcInFiat != null);
      if (_oneBtcInFiat != null) {
         updateAmounts(_numberEntry.getEntry());
      }
   }
}
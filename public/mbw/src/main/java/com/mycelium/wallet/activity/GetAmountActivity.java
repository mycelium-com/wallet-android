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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.*;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.UUID;


public class GetAmountActivity extends Activity implements NumberEntryListener {

   public static final String AMOUNT = "amount";
   public static final String ENTEREDAMOUNT = "enteredamount";
   public static final String ACCOUNT = "account";
   public static final String KB_MINER_FEE = "kbMinerFee";
   public static final String IS_COLD_STORAGE = "isColdStorage";
   public static final String SENDMODE = "sendmode";
   public static final String AMOUNT_SATOSHI = "amountSatoshi";

   private boolean isSendMode;

   private WalletAccount _account;
   private NumberEntry _numberEntry;
   private CurrencyValue _amount;
   private MbwManager _mbwManager;
   private ExactCurrencyValue _maxSpendableAmount;
   private long _kbMinerFee;

   public static void callMe(Activity currentActivity, int requestCode, UUID account, CurrencyValue amountToSend, Long kbMinerFee, boolean isColdStorage) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class);
      intent.putExtra(ACCOUNT, account);
      intent.putExtra(ENTEREDAMOUNT, amountToSend);
      intent.putExtra(KB_MINER_FEE, kbMinerFee);
      intent.putExtra(IS_COLD_STORAGE, isColdStorage);
      intent.putExtra(SENDMODE, true);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Activity currentActivity, CurrencyValue amountToSend, int requestCode) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class);
      intent.putExtra(ENTEREDAMOUNT, amountToSend);
      intent.putExtra(SENDMODE, false);
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

      isSendMode = getIntent().getBooleanExtra(SENDMODE, false);
      if (isSendMode) {
         initSendMode();
      }

      initListeners();
      updateUI();
      checkEntry();
   }

   private void initSendMode() {
      //we need to have an account, fee, etc to be able to calculate sending related stuff
      boolean isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));
      _account = _mbwManager.getWalletManager(isColdStorage).getAccount(accountId);

      // Calculate the maximum amount that can be spent where we send everything we got to another address
      _kbMinerFee = Preconditions.checkNotNull((Long) getIntent().getSerializableExtra(KB_MINER_FEE));
      _maxSpendableAmount = _account.calculateMaxSpendableAmount(_kbMinerFee);
      showMaxAmount();

      // if no amount is set, create an null amount with the correct currency
      if (_amount == null || _amount.getValue() == null){
         _amount = ExactCurrencyValue.from(null, _maxSpendableAmount.getCurrency());
         updateUI();
      }

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

      // Ok Button
      findViewById(R.id.btOk).setOnClickListener(onClickOkButton);
   }

   private void initNumberEntry(Bundle savedInstanceState) {
      _amount = (CurrencyValue) getIntent().getSerializableExtra(ENTEREDAMOUNT);
      // Load saved state
      if (savedInstanceState != null) {
         _amount = (CurrencyValue) savedInstanceState.getSerializable(ENTEREDAMOUNT);
      }
      // Set amount
      String amountString;
      if (_amount != null) {
         amountString = Utils.getFormattedValue(_amount, _mbwManager);
      } else {
         amountString = "";
      }
      TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
      tvAmount.setText(amountString);

      _numberEntry = new NumberEntry(_mbwManager.getBitcoinDenomination().getDecimalPlaces(), this, this, amountString);
   }

   private OnClickListener onClickOkButton = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         if (_amount == null){
            return;
         }

         // Return the number of satoshis
         Intent result = new Intent();
         result.putExtra(AMOUNT, _amount);
         long longValue = _amount.getAsBitcoin(_mbwManager.getExchangeRateManager()).getLongValue();
         result.putExtra(AMOUNT_SATOSHI, longValue);
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
      String content = Utils.getClipboardString(this);
      if (content.length() == 0) {
         return null;
      }
      String number = content.trim();
      if (_amount.isBtc()) {
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

   private void switchCurrency() {
      String targetCurrency = _mbwManager.getNextCurrency(true);
      CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();

      // if we have a fiat currency selected and the price is not available, switch on -> no point in showing it
      // if there is no exchange rate at all available, we will get to BTC and stay there
      while (!targetCurrency.equals(CurrencySwitcher.BTC) && !currencySwitcher.isFiatExchangeRateAvailable()) {
         targetCurrency = _mbwManager.getNextCurrency(true);
      }

      _amount = CurrencyValue.fromValue(_amount, targetCurrency, _mbwManager.getExchangeRateManager());

      updateUI();
   }

   private void updateUI() {
      //update buttons and views
      Button btCurrency = (Button) findViewById(R.id.btCurrency);

      // Show maximum spendable amount
      if (isSendMode) {
         showMaxAmount();
      }

      if (_amount.isBtc()) {
         // Set BTC button
         btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
      } else {
         // Set Fiat button
         btCurrency.setText(_amount.getCurrency());
      }

      //update amount
      int showDecimalPlaces;
      BigDecimal newAmount = null;
      if (_amount.isBtc()) {
         //just good ol bitcoins
         showDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         if (_amount.getValue() != null) {
            int btcToTargetUnit = CoinUtil.Denomination.BTC.getDecimalPlaces() - _mbwManager.getBitcoinDenomination().getDecimalPlaces();
            newAmount = _amount.getValue().multiply(BigDecimal.TEN.pow(btcToTargetUnit));
         }
      } else {
         //take what was typed in
         showDecimalPlaces = 2;
         newAmount = _amount.getValue();
      }

      _numberEntry.setEntry(newAmount, showDecimalPlaces);

      // Check whether we can show the paste button
      findViewById(R.id.btPaste).setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
   }

   private void showMaxAmount() {
      CurrencyValue maxSpendable = CurrencyValue.fromValue(_maxSpendableAmount,
            _amount.getCurrency(), _mbwManager.getExchangeRateManager());

      String maxBalanceString = getResources().getString(R.string.max_btc,
            Utils.getFormattedValueWithUnit(maxSpendable, _mbwManager));

      ((TextView) findViewById(R.id.tvMaxAmount)).setText(maxBalanceString);
   }

   @Override
   public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(ENTEREDAMOUNT, _amount);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      _mbwManager.getExchangeRateManager().requestOptionalRefresh();

      findViewById(R.id.btCurrency).setEnabled(
            _mbwManager.hasFiatCurrency()
                  && _mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable());

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
         // if it was change by the user pressing buttons (show it unformatted)
         BigDecimal value = _numberEntry.getEntryAsBigDecimal();
         setEnteredAmount(value);
      }
      updateAmountsDisplay(entry);
      checkEntry();
   }

   private void setEnteredAmount(BigDecimal value) {
      // handle denomination
      if (_amount.isBtc()) {
         Long satoshis;
         int decimals = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         satoshis = value.movePointRight(decimals).longValue();
         _amount = ExactBitcoinValue.from(satoshis);
      } else {
         _amount = ExactCurrencyValue.from(value, _amount.getCurrency());
      }

      if (isSendMode) {
         // enable/disable Max button
         findViewById(R.id.btMax).setEnabled(_maxSpendableAmount.getExactValue() != _amount.getExactValue());
      }
   }


   private void updateAmountsDisplay(String amountText) {
      // update main-currency display
      ((TextView) findViewById(R.id.tvAmount)).setText(amountText);


      // Set alternate amount if we can
      TextView tvAlternateAmount = ((TextView) findViewById(R.id.tvAlternateAmount));
      if (!_mbwManager.hasFiatCurrency() || !_mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()) {
         tvAlternateAmount.setText("");
      } else {
         CurrencyValue convertedAmount;
         if (_amount.isBtc()) {
            // Show Fiat as alternate amount
            String currency = MbwManager.getInstance(getApplication()).getFiatCurrency();
            convertedAmount = ExchangeBasedCurrencyValue.fromValue(
                  _amount, currency, _mbwManager.getExchangeRateManager());
         } else {
            // Show BTC as alternate amount
            convertedAmount = ExchangeBasedCurrencyValue.fromValue(
                  _amount, "BTC", _mbwManager.getExchangeRateManager());
         }
         tvAlternateAmount.setText(Utils.getFormattedValueWithUnit(convertedAmount, _mbwManager));
      }
   }

   private void maximizeAmount() {
      if (_maxSpendableAmount.isZero()) {
         String msg = getResources().getString(R.string.insufficient_funds);
         Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
      } else {
         _amount = _maxSpendableAmount;
         updateUI();
         checkEntry();
      }
   }

   private void checkEntry() {
      if (_amount.getValue() == null || _amount.isZero()) {
         // Nothing entered
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
         findViewById(R.id.btOk).setEnabled(false);
         return;
      }
      if (isSendMode && _amount.getValue() != null) {
         AmountValidation result = checkTransaction();
         // Enable/disable Ok button
         findViewById(R.id.btOk).setEnabled(result == AmountValidation.Ok
               && !_amount.isZero());
      } else {
         findViewById(R.id.btOk).setEnabled(true);
      }
   }

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   private AmountValidation checkSendAmount(Bitcoins satoshis) {
      if (satoshis == null) return AmountValidation.Ok; //entering a fiat value + exchange is not availible
      try {
         WalletAccount.Receiver receiver = new WalletAccount.Receiver(Address.getNullAddress(_mbwManager.getNetwork()), satoshis);
         _account.checkAmount(receiver, _kbMinerFee, _amount);
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
      Bitcoins satoshis = _amount.getAsBitcoin(_mbwManager.getExchangeRateManager());
      // Check whether we have sufficient funds, and whether the output is too small
      AmountValidation result = checkSendAmount(satoshis);

      if (result == AmountValidation.Ok) {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.white));
      } else {
         ((TextView) findViewById(R.id.tvAmount)).setTextColor(getResources().getColor(R.color.red));
         if (result == AmountValidation.NotEnoughFunds) {
            // We do not have enough funds
            if (_account.getBalance().getSpendableBalance() < satoshis.getLongValue()) {
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
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
      updateExchangeRateDisplay();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
      updateExchangeRateDisplay();
   }

   private void updateExchangeRateDisplay() {
      Double exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      findViewById(R.id.btCurrency).setEnabled(exchangeRatePrice != null);
      if (exchangeRatePrice != null) {
         updateAmountsDisplay(_numberEntry.getEntry());
      }
   }
}
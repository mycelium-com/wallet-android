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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.CurrencySwitcher;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.AccountDisplayType;
import com.mycelium.wallet.activity.util.ValueExtentionsKt;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.fiat.FiatType;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GetAmountActivity extends Activity implements NumberEntryListener {
   public static final String AMOUNT = "amount";
   public static final String ENTERED_AMOUNT = "enteredamount";
   public static final String ACCOUNT = "account";
   public static final String KB_MINER_FEE = "kbMinerFee";
   public static final String IS_COLD_STORAGE = "isColdStorage";
   public static final String SEND_MODE = "sendmode";
   public static final String BASIC_CURRENCY = "basiccurrency";

   @BindView(R.id.btCurrency) Button btCurrency;
   @BindView(R.id.btPaste) Button btPaste;
   @BindView(R.id.btMax) Button btMax;
   @BindView(R.id.btOk) Button btOk;
   @BindView(R.id.tvMaxAmount) TextView tvMaxAmount;
   @BindView(R.id.tvHowIsItCalculated) TextView tvHowIsItCalculated;
   @BindView(R.id.tvAmount) TextView tvAmount;
   @BindView(R.id.tvAlternateAmount) TextView tvAlternateAmount;

   private boolean isSendMode;

   private WalletAccount _account;
   private NumberEntry _numberEntry;
   private Value _amount;
   private MbwManager _mbwManager;
   private Value _maxSpendableAmount;
   private long _kbMinerFee;

   private boolean isColu;
   private AccountDisplayType mainCurrencyType;

   /**
    * Get Amount for spending
    */
   public static void callMeToSend(Activity currentActivity, int requestCode, UUID account, Value amountToSend, Long kbMinerFee,
                                   AccountDisplayType currencyType, boolean isColdStorage)
   {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class)
              .putExtra(ACCOUNT, account)
              .putExtra(ENTERED_AMOUNT, amountToSend)
              .putExtra(KB_MINER_FEE, kbMinerFee)
              .putExtra(IS_COLD_STORAGE, isColdStorage)
              .putExtra(SEND_MODE, true)
              .putExtra(BASIC_CURRENCY, currencyType);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   /**
    * Get Amount for receiving
    */
   public static void callMeToReceive(Activity currentActivity, CurrencyValue amountToReceive, int requestCode, AccountDisplayType currencyType) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class)
              .putExtra(ENTERED_AMOUNT, amountToReceive)
              .putExtra(SEND_MODE, false)
              .putExtra(BASIC_CURRENCY, currencyType);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @SuppressLint("ShowToast")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_amount_activity);
      ButterKnife.bind(this);

      _mbwManager = MbwManager.getInstance(getApplication());
      isSendMode = getIntent().getBooleanExtra(SEND_MODE, false);
      if (isSendMode) {
         initSendModeAccount();
      } else {
         _account = _mbwManager.getSelectedAccount();
      }
      isColu = _account instanceof ColuAccount;
      initNumberEntry(savedInstanceState);

      mainCurrencyType = (AccountDisplayType) getIntent().getSerializableExtra(BASIC_CURRENCY);

      _mbwManager.getCurrencySwitcher().setDefaultCurrency(mainCurrencyType.getAccountLabel());

      if (isSendMode) {
         initSendMode();
      }

      initListeners();
      updateUI();
      checkEntry();
   }

   private void initSendMode() {
      // Calculate the maximum amount that can be spent where we send everything we got to another address
      _kbMinerFee = Preconditions.checkNotNull((Long) getIntent().getSerializableExtra(KB_MINER_FEE));
      // todo max
//      _maxSpendableAmount = _account.calculateMaxSpendableAmount(_kbMinerFee);
//      showMaxAmount();

      // if no amount is set, create an null amount with the correct currency
      if (_amount == null) {
         // todo get generic value (BTC/ETH) ? using _account.getAccountDefaultCurrency()
         _amount = Value.valueOf(BitcoinTest.get(), 0);
         updateUI();
      }

      // Max Button
      tvMaxAmount.setVisibility(View.VISIBLE);
      tvHowIsItCalculated.setVisibility(View.VISIBLE);
      btMax.setVisibility(View.VISIBLE);
   }

   private void initSendModeAccount() {
      //we need to have an account, fee, etc to be able to calculate sending related stuff
      boolean isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));
      _account = _mbwManager.getWalletManager(isColdStorage).getAccount(accountId);
   }

   private void initListeners() {
      // set the text for the currency button
      if(isColu) {
         ColuAccount coluAccount = (ColuAccount) _account;
         if (_amount == null) {
            // todo get generic value (BTC/ETH) ? using coluAccount.getAccountDefaultCurrency()
            _amount = Value.valueOf(BitcoinTest.get(), 0);;
         }
      } else {
//         btCurrency.setText(_mbwManager.getBitcoinDenomination().getUnicodeName());
         btCurrency.setEnabled(_mbwManager.getCurrencySwitcher().getExchangeRatePrice() != null);
      }
   }

   private void initNumberEntry(Bundle savedInstanceState) {
      _amount = (Value) getIntent().getSerializableExtra(ENTERED_AMOUNT);
      // Load saved state
      if (savedInstanceState != null) {
         _amount = (Value) savedInstanceState.getSerializable(ENTERED_AMOUNT);
      }

      // Init the number pad
      String amountString;
      if (!Value.isNullOrZero(_amount)) {
         if(isColu) {
            amountString = Utils.getColuFormattedValue(_amount);
         }else {
            amountString = Utils.getFormattedValue(_amount, _mbwManager.getBitcoinDenomination());
         }
         _mbwManager.getCurrencySwitcher().setCurrency(_amount.getCurrencySymbol());
      } else {
         if (_amount != null && _amount.getCurrencySymbol() != null) {
            _mbwManager.getCurrencySwitcher().setCurrency(_amount.getCurrencySymbol());
         } else {
            _mbwManager.getCurrencySwitcher().setCurrency(_account.getCoinType().getSymbol());
         }
         amountString = "";
      }
      if(isColu) {
         _numberEntry = new NumberEntry(4, this, this, amountString);
      } else {
         _numberEntry = new NumberEntry(_mbwManager.getBitcoinDenomination().getDecimalPlaces(), this, this, amountString);
      }
   }

   @OnClick(R.id.btOk)
   void onOkClick() {
      if (Value.isNullOrZero(_amount) && isSendMode) {
         return;
      }

      // Return the entered value and set a positive result code
      Intent result = new Intent();
      result.putExtra(AMOUNT, _amount);
      setResult(RESULT_OK, result);
      finish();
   }

   @OnClick(R.id.btMax)
   void onMaxButtonClick() {
      if (Value.isNullOrZero(_maxSpendableAmount)) {
         String msg = getResources().getString(R.string.insufficient_funds);
         Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
      } else {
         _amount = _maxSpendableAmount;
         // set the current shown currency to the amounts currency
         _mbwManager.getCurrencySwitcher().setCurrency(_amount.getCurrencySymbol());
         updateUI();
         checkEntry();
      }
   }

   @OnClick({R.id.btRight, R.id.btCurrency})
   void onSwitchCurrencyClick() {
      // if we have a fiat currency selected and the price is not available, switch on -> no point in showing it
      // if there is no exchange rate at all available, we will get to BTC and stay there
      // this does not apply to digital assets such as Colu for which we do not have a rate
      if(_amount != null && !_mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())) {
         String targetCurrency = _mbwManager.getNextCurrency(true);
         CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();
         while (!targetCurrency.equals(mainCurrencyType.getAccountLabel()) && !currencySwitcher.isFiatExchangeRateAvailable()) {
            targetCurrency = _mbwManager.getNextCurrency(true);
         }
//         _amount = CurrencyValue.fromValue(_amount, targetCurrency, _mbwManager.getExchangeRateManager());
         _amount = Value.valueOf(new FiatType(targetCurrency), _amount.getValue()); // todo use exchange rate manager?
         // todo create a FiatValue for _amount?
      }

      updateUI();
   }

   @OnClick({R.id.btLeft, R.id.btPaste})
   void onPasteButtonClick() {
      BigDecimal clipboardValue = getAmountFromClipboard();
      if (clipboardValue == null) {
         return;
      }
      setEnteredAmount(clipboardValue);

      _numberEntry.setEntry(clipboardValue, isColu ? 4 : _mbwManager.getBitcoinDenomination().getDecimalPlaces());
   }

   @OnClick(R.id.tvHowIsItCalculated)
   void howIsItCalculatedClick() {
      new AlertDialog.Builder(this)
              .setMessage(getString(R.string.how_is_it_calculated_text))
              .setPositiveButton(R.string.button_ok, null)
              .create()
              .show();
   }

   private boolean enablePaste() {
      return getAmountFromClipboard() != null;
   }

   private BigDecimal getAmountFromClipboard() {
      String content = Utils.getClipboardString(this);
      if (content.length() == 0) {
         return null;
      }
      String number = content.trim();
      if (mainCurrencyType.getAccountLabel().equals(_mbwManager.getCurrencySwitcher().getCurrentCurrency())) {
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

   private void updateUI() {
      //update buttons and views

      // Show maximum spendable amount
      if (isSendMode) {
//         showMaxAmount(); todo max
      }

      if (_amount != null) {
         if(_mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())) {
            // always set native asset currency here ?
            btCurrency.setText(_amount.getCurrencySymbol());
         } else {
            // Set current currency name button
            btCurrency.setText(_mbwManager.getCurrencySwitcher().getCurrentCurrencyIncludingDenomination());
         }
         //update amount
         int showDecimalPlaces;
         BigDecimal newAmount = null;
         if ( _mbwManager.getCurrencySwitcher().getCurrentCurrency().equals(mainCurrencyType.getAccountLabel())) {
            //just good ol bitcoins
            showDecimalPlaces = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
            if (_amount.getValueAsBigDecimal() != null) {
               int btcToTargetUnit = CoinUtil.Denomination.BTC.getDecimalPlaces() - _mbwManager.getBitcoinDenomination().getDecimalPlaces();
               newAmount = _amount.getValueAsBigDecimal().multiply(BigDecimal.TEN.pow(btcToTargetUnit));
            }
         } else if (isColu) {
            showDecimalPlaces = CoinUtil.Denomination.BTC.getDecimalPlaces();
            newAmount = _amount.getValueAsBigDecimal();
         } else {
            //take what was typed in
            showDecimalPlaces = 2;
            newAmount = _amount.getValueAsBigDecimal();
         }
         _numberEntry.setEntry(newAmount, isColu ? 4 : showDecimalPlaces);
      } else {
         tvAmount.setText("");
      }

      // Check whether we can show the paste button
      btPaste.setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
   }

   private void showMaxAmount() {
      Value maxSpendable = Value.valueOf(BitcoinTest.get(), _maxSpendableAmount.value);
      // todo was Value.fromValue(_maxSpendableAmount, _amount.getCurrencySymbol(), _mbwManager.getExchangeRateManager());

      String maxBalanceString = "";
      if (isColu) {
         maxBalanceString = getResources().getString(R.string.max_btc,
                 Utils.getColuFormattedValueWithUnit(maxSpendable));
      } else {
         maxBalanceString = getResources().getString(R.string.max_btc
                 , ValueExtentionsKt.toStringWithUnit(maxSpendable, _mbwManager.getBitcoinDenomination()));
      }
      tvMaxAmount.setText(maxBalanceString);
   }

   @Override
   public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(ENTERED_AMOUNT, _amount);
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);
      _mbwManager.getExchangeRateManager().requestOptionalRefresh();
      if(!isColu) {
         btCurrency.setEnabled(_mbwManager.hasFiatCurrency()
                 && _mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()
                 && _amount != null && !_mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())
         );
      }
      btPaste.setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();
      currencySwitcher.setCurrency(currencySwitcher.getCurrentCurrency());
      super.onPause();
   }

   @Override
   public void onEntryChanged(String entry, boolean wasSet) {
      if (!wasSet) {
         // if it was change by the user pressing buttons (show it unformatted)
         BigDecimal value = _numberEntry.getEntryAsBigDecimal();
         Log.d("mulicurrencyslplog", "onEntryChanged: " + value);
         setEnteredAmount(value);
      }
      updateAmountsDisplay(entry);
      checkEntry();
   }

   private void setEnteredAmount(BigDecimal value) {
      // handle denomination
      String currentCurrency;
      if(_amount != null && _mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())) {
         currentCurrency = _amount.getCurrencySymbol();
      } else {
         currentCurrency = _mbwManager.getCurrencySwitcher().getCurrentCurrency();
      }
      if (currentCurrency.equals(mainCurrencyType.getAccountLabel())) {
         Long satoshis;
         int decimals = _mbwManager.getBitcoinDenomination().getDecimalPlaces();
         satoshis = value.movePointRight(decimals).longValue();
         if (satoshis >= Bitcoins.MAX_VALUE) {
            // entered value is equal or larger then total amount of bitcoins ever existing
            return;
         }
         if (mainCurrencyType == AccountDisplayType.BTC_ACCOUNT) {
            _amount = Value.valueOf(BitcoinTest.get(), satoshis); // todo
         } else if (mainCurrencyType == AccountDisplayType.BCH_ACCOUNT) {
            _amount = Value.valueOf(BitcoinTest.get(), satoshis); // todo BitcoinCashValue
         }
      } else {
         _amount = Value.valueOf(BitcoinTest.get(), 567); // todo was ExactCurrencyValue.from(value, currentCurrency);
      }

      if (isSendMode) {
         // enable/disable Max button
         btMax.setEnabled(_maxSpendableAmount.getValue() != _amount.getValue());
      }
   }


   private void updateAmountsDisplay(String amountText) {
      // update main-currency display
      tvAmount.setText(amountText);
      // Set alternate amount if we can
      if (!_mbwManager.hasFiatCurrency()
              || _mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())
              || !_mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()
              || Value.isNullOrZero(_amount)) {
         tvAlternateAmount.setText("");
      } else {
         Value convertedAmount;
         if (mainCurrencyType.getAccountLabel().equals(_mbwManager.getCurrencySwitcher().getCurrentCurrency())) {
            // Show Fiat as alternate amount
            String currency = _mbwManager.getFiatCurrency();
            convertedAmount = Value.valueOf(BitcoinTest.get(), _amount.getValue()); // todo use fiat!
//                    ExchangeBasedCurrencyValue.fromValue(_amount, currency, _mbwManager.getExchangeRateManager());
         } else {
            // Show BTC as alternate amount
            try {
               convertedAmount = Value.valueOf(BitcoinTest.get(), _amount.getValue());
               // ExchangeBasedCurrencyValue.fromValue(_amount, mainCurrencyType.getAccountLabel(), _mbwManager.getExchangeRateManager());
            } catch (IllegalArgumentException ex){
               // something failed while calculating the bitcoin amount
               convertedAmount = Value.valueOf(BitcoinTest.get(), 0);// todo not bitcoin
            }
         }
         tvAlternateAmount.setText(ValueExtentionsKt.toStringWithUnit(convertedAmount, _mbwManager.getBitcoinDenomination()));
      }
   }

   private void checkEntry() {
      if (isSendMode ){
         if(Value.isNullOrZero(_amount)) {
            // Nothing entered
            tvAmount.setTextColor(getResources().getColor(R.color.white));
            btOk.setEnabled(false);
         } else {
            AmountValidation result = checkTransaction();
            // Enable/disable Ok button
            btOk.setEnabled(result == AmountValidation.Ok && !_amount.isZero());
         }
      } else {
         btOk.setEnabled(true);
      }
   }

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   private AmountValidation checkSendAmount(long satoshis) {
      if (satoshis == 0) {
         return AmountValidation.Ok; //entering a fiat value + exchange is not availible
      }
      try {
         Address address = Address.getNullAddress(_mbwManager.getNetwork());
         WalletAccount.Receiver receiver = new WalletAccount.Receiver(address.isP2SH(address.getNetwork()) ?
                 new SegwitAddress(new com.mrd.bitlib.model.SegwitAddress(address.getNetwork(),
                         0x00,address.getAllAddressBytes())) :
                 new BtcAddress(address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get(),
                         address.getAllAddressBytes()), satoshis);
         _account.checkAmount(receiver, _kbMinerFee, _amount);
      } catch (OutputTooSmallException e1) {
         return AmountValidation.ValueTooSmall;
      } catch (InsufficientFundsException e) {
         return AmountValidation.NotEnoughFunds;
      } catch (StandardTransactionBuilder.UnableToBuildTransactionException e) {
         // under certain conditions the max-miner-fee check fails - report it back to the server, so we can better
         // debug it
         _mbwManager.reportIgnoredException("MinerFeeException", e);
         return AmountValidation.Invalid;
      } catch (com.mrd.bitlib.model.SegwitAddress.SegwitAddressException e) {
         e.printStackTrace();
      }
      return AmountValidation.Ok;
   }

   private AmountValidation checkSendAmount(Value value) {
      if (value == null) {
         return AmountValidation.Ok; //entering a fiat value + exchange is not availible
      }
      if(_account.getAccountBalance().confirmed.getValueAsBigDecimal().compareTo(value.getValueAsBigDecimal()) < 0) {
         return AmountValidation.ValueTooSmall;
      } else if(!_account.getAccountBalance().confirmed.isPositive()) {
         return AmountValidation.NotEnoughFunds;
      }
      return AmountValidation.Ok;
   }

   private enum AmountValidation {
      Ok, ValueTooSmall, Invalid, NotEnoughFunds
   }

   private AmountValidation checkTransaction() {
      AmountValidation result;
      long satoshis = 0;
      if(isColu) {
         result = checkSendAmount(_amount);
      } else {
         try {
            satoshis = _amount.getValue(); // todo was _amount.getAsBitcoin(_mbwManager.getExchangeRateManager());
         } catch (IllegalArgumentException ex) {
            // something failed while calculating the bitcoin amount
            return AmountValidation.Invalid;
         }
         // Check whether we have sufficient funds, and whether the output is too small
         result = checkSendAmount(satoshis);
      }

      if (result == AmountValidation.Ok) {
         tvAmount.setTextColor(getResources().getColor(R.color.white));
      } else {
         tvAmount.setTextColor(getResources().getColor(R.color.red));
         if (result == AmountValidation.NotEnoughFunds) {
            // We do not have enough funds
            if (satoshis == 0 || _account.getAccountBalance().getSpendable().value < satoshis) {
               // We do not have enough funds for sending the requested amount
               String msg = getResources().getString(R.string.insufficient_funds);
               Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } else {
               // We do have enough funds for sending the requested amount, but
               // not for the required fee
               String msg = getResources().getString(R.string.insufficient_funds_for_fee);
               Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
         }
         // else {
         // The amount we want to send is not large enough for the network to
         // accept it. Don't Toast about it, it's just annoying
         // }
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
      if(_amount != null && ! _mbwManager.getColuManager().isColuAsset(_amount.getCurrencySymbol())) {
         Double exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
         btCurrency.setEnabled(exchangeRatePrice != null);
         if (exchangeRatePrice != null) {
            updateAmountsDisplay(_numberEntry.getEntry());
         }
      }
   }
}

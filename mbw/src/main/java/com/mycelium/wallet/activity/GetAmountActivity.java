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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Preconditions;
import com.mycelium.view.Denomination;
import com.mycelium.wallet.CurrencySwitcher;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NumberEntry;
import com.mycelium.wallet.NumberEntry.NumberEntryListener;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.ExchangeValueKt;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.FeePerKbFee;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException;
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException;
import com.mycelium.wapi.wallet.exceptions.OutputTooSmallException;
import com.mycelium.wapi.wallet.fiat.coins.FiatType;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GetAmountActivity extends AppCompatActivity implements NumberEntryListener {
   public static final String AMOUNT = "amount";
   public static final String ENTERED_AMOUNT = "enteredamount";
   public static final String ACCOUNT = "account";
   public static final String KB_MINER_FEE = "kbMinerFee";
   public static final String IS_COLD_STORAGE = "isColdStorage";
   public static final String DESTINATION_ADDRESS = "destinationAddress";
   public static final String SEND_MODE = "sendmode";

   @BindView(R.id.btCurrency) TextView btCurrency;
   @BindView(R.id.btPaste) Button btPaste;
   @BindView(R.id.btMax) Button btMax;
   @BindView(R.id.btOk) Button btOk;
   @BindView(R.id.tvMaxAmount) TextView tvMaxAmount;
   @BindView(R.id.tvHowIsItCalculated) TextView tvHowIsItCalculated;
   @BindView(R.id.tvAmount) TextView tvAmount;
   @BindView(R.id.tvAlternateAmount) TextView tvAlternateAmount;
   @BindView(R.id.currency_dropdown_image_view) View currencyDropDown;

   private boolean isSendMode;

   private WalletAccount _account;
   private NumberEntry _numberEntry;
   private Value _amount;
   private MbwManager _mbwManager;
   private Value _maxSpendableAmount;
   private Address destinationAddress;
   private Value _kbMinerFee;
   private AssetInfo mainCurrencyType;
   /**
    * Get Amount for spending
    */
   public static void callMeToSend(Activity currentActivity, int requestCode, UUID account, Value amountToSend, Value kbMinerFee,
                                   boolean isColdStorage, Address destinationAddress)
   {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class)
              .putExtra(ACCOUNT, account)
              .putExtra(ENTERED_AMOUNT, amountToSend)
              .putExtra(KB_MINER_FEE, kbMinerFee)
              .putExtra(IS_COLD_STORAGE, isColdStorage)
              .putExtra(SEND_MODE, true);
      if (destinationAddress != null) {
         intent.putExtra(DESTINATION_ADDRESS, destinationAddress);
      }
      currentActivity.startActivityForResult(intent, requestCode);
   }

   /**
    * Get Amount for receiving
    */
   public static void callMeToReceive(Activity currentActivity, Value amountToReceive, int requestCode, CryptoCurrency currencyType) {
      Intent intent = new Intent(currentActivity, GetAmountActivity.class)
              .putExtra(ENTERED_AMOUNT, amountToReceive)
              .putExtra(SEND_MODE, false);
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

      mainCurrencyType = _account.getCoinType();
      _mbwManager.getCurrencySwitcher().setDefaultCurrency(mainCurrencyType);
      _mbwManager.getCurrencySwitcher().setCurrency(_account.getCoinType(), mainCurrencyType);

      initNumberEntry(savedInstanceState);
      if (isSendMode) {
         initSendMode();
      }
      updateUI();
      checkEntry();
      setupActionBar();
   }

   private void setupActionBar() {
      getSupportActionBar().setTitle("Amount");
      getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
      getSupportActionBar().setHomeButtonEnabled(true);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
   }

   private int getMaxDecimal(AssetInfo assetInfo) {
      if (!(assetInfo instanceof FiatType)) {
         return assetInfo.getUnitExponent() - _mbwManager.getDenomination(_account.getCoinType()).getScale();
      } else {
         return assetInfo.getUnitExponent();
      }
   }

   private void initSendMode() {
      // Calculate the maximum amount that can be spent where we send everything we got to another address
      _kbMinerFee = Preconditions.checkNotNull((Value) getIntent().getSerializableExtra(KB_MINER_FEE));
      destinationAddress = (Address) getIntent().getSerializableExtra(DESTINATION_ADDRESS);

      if (destinationAddress == null) {
         destinationAddress = _account.getDummyAddress();
      }

      _maxSpendableAmount = _account.calculateMaxSpendableAmount(_kbMinerFee, destinationAddress);
      showMaxAmount();

      // if no amount is set, create an null amount with the correct currency
      if (_amount == null) {
         _amount = Value.valueOf(_account.getCoinType(), 0);
      }

      // Max Button
      tvMaxAmount.setVisibility(View.VISIBLE);
      tvHowIsItCalculated.setVisibility(_account.getCoinType().equals(Utils.getBtcCoinType()) ? View.VISIBLE : View.GONE);
      btMax.setVisibility(View.VISIBLE);
   }

   private void initSendModeAccount() {
      //we need to have an account, fee, etc to be able to calculate sending related stuff
      boolean isColdStorage = getIntent().getBooleanExtra(IS_COLD_STORAGE, false);
      UUID accountId = Preconditions.checkNotNull((UUID) getIntent().getSerializableExtra(ACCOUNT));
      _account = _mbwManager.getWalletManager(isColdStorage).getAccount(accountId);
   }

   private void initNumberEntry(Bundle savedInstanceState) {
      // Load saved state
      if (savedInstanceState != null) {
         _amount = (Value) savedInstanceState.getSerializable(ENTERED_AMOUNT);
      } else {
         _amount = (Value) getIntent().getSerializableExtra(ENTERED_AMOUNT);
      }

      // Init the number pad
      String amountString;
      AssetInfo asset;
      if (!Value.isNullOrZero(_amount)) {
         amountString = ValueExtensionsKt.toString(_amount, _mbwManager.getDenomination(_account.getCoinType()));
         asset = _amount.type;
      } else {
         asset = _amount != null && _amount.getCurrencySymbol() != null ? _amount.type : _account.getCoinType();
         amountString = "";
      }
      _mbwManager.getCurrencySwitcher().setCurrency(_account.getCoinType(), asset);
      _numberEntry = new NumberEntry(getMaxDecimal(asset), this, this, amountString);
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
         new Toaster(this).toast(R.string.insufficient_funds, true);
      } else {
         _amount = _maxSpendableAmount;
         // set the current shown currency to the amount's currency
         _mbwManager.getCurrencySwitcher().setCurrency(_account.getCoinType(), _amount.type);
         updateUI();
         checkEntry();
      }
   }

   @OnClick(R.id.btCurrency)
   void onSwitchCurrencyClick() {
      final List<AssetInfo> currencyList = getAvailableCurrencyList();
      if (currencyList.size() > 1) {
         PopupMenu currencyListMenu = new PopupMenu(this, btCurrency);
         List<String> cryptocurrencies = _mbwManager.getWalletManager(false).getCryptocurrenciesSymbols();
         for (AssetInfo asset : currencyList) {
            String itemTitle = asset.getSymbol();
            // we want to display cryptocurrency items as "Symbol (denomination if it differs from UNIT)", e.g. "BTC (bits)"
            Denomination denomination =_mbwManager.getDenomination(_account.getCoinType());
            if (cryptocurrencies.contains(asset.getSymbol()) && denomination != Denomination.UNIT) {
               itemTitle += " (" + denomination.getUnicodeString(asset.getSymbol()) + ")";
            }
            currencyListMenu.getMenu().add(Menu.NONE, asset.hashCode(), Menu.NONE, itemTitle);
         }
         currencyListMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
               for (AssetInfo assetInfo : currencyList) {
                  if (menuItem.getItemId() == assetInfo.hashCode()) {
                     _mbwManager.getCurrencySwitcher().setCurrency(_account.getCoinType(), assetInfo);
                     if (_amount != null) {
                        _amount = convert(_amount, assetInfo);
                     }
                     updateUI();
                     return true;
                  }
               }
               return false;
            }
         });
         currencyListMenu.show();
      }
   }

   private List<AssetInfo> getAvailableCurrencyList() {
      List<AssetInfo> result = new ArrayList<>();
      for (AssetInfo asset : _mbwManager.getCurrencySwitcher().getCurrencyList(mainCurrencyType)) {
         if (convert(asset.oneCoin(), mainCurrencyType) != null) {
            result.add(asset);
         }
      }
      return result;
   }

   @OnClick(R.id.btPaste)
   void onPasteButtonClick() {
      String clipboardValue = getAmountFromClipboard();
      if (clipboardValue == null) {
         return;
      }
      setEnteredAmount(clipboardValue);
      _numberEntry.setEntry(_amount.getValueAsBigDecimal(), getMaxDecimal(_mbwManager
              .getCurrencySwitcher().getCurrentCurrency(_account.getCoinType())));
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

   private String getAmountFromClipboard() {
      String content = Utils.getClipboardString(this);
      if (content.length() == 0) {
         return null;
      }
      String number = Utils.truncateAndConvertDecimalString(content.trim(), getMaxDecimal(mainCurrencyType));
      if (number == null) {
         return null;
      }
      BigDecimal value = new BigDecimal(number);
      if (value.compareTo(BigDecimal.ZERO) < 1) {
         return null;
      }
      return number;
   }

   private void updateUI() {
      //update buttons and views

      // Show maximum spendable amount
      if (isSendMode) {
         showMaxAmount();
      }
      currencyDropDown.setVisibility(getAvailableCurrencyList().size() > 1 ? View.VISIBLE : View.GONE);
      CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();
      btCurrency.setText(currencySwitcher.getCurrentCurrencyIncludingDenomination(_account.getCoinType()));
      if (_amount != null) {
         // Set current currency name button
         //update amount
         BigDecimal newAmount;
         if (currencySwitcher.getCurrentCurrency(_account.getCoinType()) instanceof FiatType) {
            newAmount = _amount.getValueAsBigDecimal();
         } else {
            int toTargetUnit = _mbwManager.getDenomination(_account.getCoinType()).getScale();
            newAmount = _amount.getValueAsBigDecimal().multiply(BigDecimal.TEN.pow(toTargetUnit));
         }
         _numberEntry.setEntry(newAmount, getMaxDecimal(_amount.type));
      } else {
         tvAmount.setText("");
      }


      // Check whether we can show the paste button
      btPaste.setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
   }

   private void showMaxAmount() {
      Value maxSpendable = convert(_maxSpendableAmount, _mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()));
      String maxBalanceString = getResources().getString(R.string.max_btc
              , ValueExtensionsKt.toStringWithUnit(maxSpendable != null ? maxSpendable : Value.zeroValue(_mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType())),
                      _mbwManager.getDenomination(_account.getCoinType())));
      tvMaxAmount.setText(maxBalanceString);
   }

   @Override
   public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable(ENTERED_AMOUNT, _amount);
   }

   @Override
   protected void onResume() {
      MbwManager.getEventBus().register(this);
      _mbwManager.getExchangeRateManager().requestOptionalRefresh();
      btPaste.setVisibility(enablePaste() ? View.VISIBLE : View.GONE);
      super.onResume();
   }

   @Override
   protected void onPause() {
      MbwManager.getEventBus().unregister(this);
      CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();
      currencySwitcher.setCurrency(_account.getCoinType(), currencySwitcher.getCurrentCurrencyMap().get(_account.getCoinType()));
      super.onPause();
   }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
   public void onEntryChanged(String entry, boolean wasSet) {
      if (!wasSet) {
         // if it was change by the user pressing buttons (show it unformatted)
         setEnteredAmount(entry);
      }
      updateAmountsDisplay(entry);
      checkEntry();
   }

   private void setEnteredAmount(String value) {
      try {
         Value val = _mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()).value(value);
         CurrencySwitcher currencySwitcher = _mbwManager.getCurrencySwitcher();
         if (currencySwitcher.getCurrentCurrency(_account.getCoinType()) instanceof FiatType) {
            _amount = val;
         } else {
            _amount = Value.valueOf(val.type, _mbwManager.getDenomination(_account.getCoinType()).getAmount(val.value));
         }
      } catch (NumberFormatException e) {
         _amount = _mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()).value(0);
      }

      if (isSendMode) {
         // enable/disable Max button
         btMax.setEnabled(_maxSpendableAmount.notEqualsTo(_amount));
      }
   }


   private void updateAmountsDisplay(String amountText) {
      // update main-currency display
      tvAmount.setText(amountText);
      // Set alternate amount if we can
      if (!_mbwManager.hasFiatCurrency()
              || !_mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable(_account.getCoinType())) {
         tvAlternateAmount.setText("");
      } else {
         Value convertedAmount;
         if (mainCurrencyType.equals(_mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()))) {
            // Show Fiat as alternate amount
            AssetInfo currency = _mbwManager.getFiatCurrency(_account.getCoinType());
            convertedAmount = convert(_amount, currency);
         } else {
            try {
               convertedAmount = convert(_amount, mainCurrencyType);
            } catch (IllegalArgumentException ex){
               // something failed while calculating the amount
               convertedAmount = null;
            }
         }
         if(convertedAmount != null) {
            tvAlternateAmount.setText(ValueExtensionsKt.toStringWithUnit(convertedAmount, _mbwManager.getDenomination(_account.getCoinType())));
         } else {
            tvAlternateAmount.setText(null);
         }
      }
   }

   private void checkEntry() {
      if (isSendMode) {
         if (Value.isNullOrZero(_amount)) {
            // Nothing entered
            tvAmount.setTextColor(getResources().getColor(R.color.white));
            btOk.setEnabled(false);
         } else {
            new Handler(Looper.myLooper()).post(new Runnable() {
               @Override
               public void run() {
                  checkTransaction();
               }
            });
         }
      } else {
         btOk.setEnabled(true);
      }
   }

   /**
    * Check that the amount is large enough for the network to accept it, and
    * that we have enough funds to send it.
    */
   @SuppressLint("StaticFieldLeak")
   private void checkSendAmount(final Value value, final CheckListener listener) {
      new AsyncTask<Void, Void, AmountValidation>() {
         @Override
         protected AmountValidation doInBackground(Void... voids) {
            if(value == null) {
               return AmountValidation.ExchangeRateNotAvailable;
            }else if (value.equalZero()) {
               return AmountValidation.Ok; //entering a fiat value + exchange is not availible
            }
            try {
               _account.createTx(_account.getDummyAddress(destinationAddress.getSubType()), value, new FeePerKbFee(_kbMinerFee), null);
            } catch (OutputTooSmallException e) {
               return AmountValidation.ValueTooSmall;
            } catch (InsufficientFundsException e) {
               return AmountValidation.NotEnoughFunds;
            } catch (BuildTransactionException e) {
               // under certain conditions the max-miner-fee check fails - report it back to the server, so we can better
               // debug it
               _mbwManager.reportIgnoredException("MinerFeeException", e);
               return AmountValidation.Invalid;
            } catch (Exception e) {
               e.printStackTrace();
               return AmountValidation.Invalid;
            }
            return AmountValidation.Ok;
         }

         @Override
         protected void onPostExecute(AmountValidation amountValidation) {
            super.onPostExecute(amountValidation);
            if(listener != null) {
               listener.onResult(amountValidation);
            }
         }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
   }

   interface CheckListener {
      void onResult(AmountValidation result);
   }

   private enum AmountValidation {
      Ok, ValueTooSmall, Invalid, NotEnoughFunds, ExchangeRateNotAvailable
   }

   private void checkTransaction() {
      // Check whether we have sufficient funds, and whether the output is too small
      Value amount = _amount;
      // if _amount is not in account's currency then convert to account's currency before checking amount
      if (!mainCurrencyType.equals(_mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()))) {
         amount = convert(_amount, mainCurrencyType);
      }
      checkSendAmount(amount, new CheckListener() {
         @Override
         public void onResult(AmountValidation result) {
            if (result == AmountValidation.Ok) {
               tvAmount.setTextColor(getResources().getColor(R.color.white));
            } else {
               tvAmount.setTextColor(getResources().getColor(R.color.red));
               Value amount = _amount;
               // if _amount is not in account's currency then convert to account's currency before checking amount
               if (!mainCurrencyType.equals(_mbwManager.getCurrencySwitcher().getCurrentCurrency(_account.getCoinType()))) {
                  amount = convert(_amount, mainCurrencyType);
               }
               if (result == AmountValidation.NotEnoughFunds) {
                  // We do not have enough funds
                  if (amount.equalZero() || _account.getAccountBalance().getSpendable().lessThan(amount)) {
                     // We do not have enough funds for sending the requested amount
                     new Toaster(GetAmountActivity.this).toast(R.string.insufficient_funds, true);
                  } else {
                     // We do have enough funds for sending the requested amount, but
                     // not for the required fee
                     new Toaster(GetAmountActivity.this).toast(R.string.insufficient_funds_for_fee, true);
                  }
               } else if(result == AmountValidation.ExchangeRateNotAvailable) {
                  new Toaster(GetAmountActivity.this).toast(R.string.exchange_rate_unavailable, true);
               }
               // else {
               // The amount we want to send is not large enough for the network to
               // accept it. Don't Toast about it, it's just annoying
               // }
            }
             // Enable/disable Ok button
             btOk.setEnabled(result == AmountValidation.Ok && !_amount.isZero());
         }
      });
   }

   private Value convert(Value value, AssetInfo assetInfo) {
      return ExchangeValueKt.get(_mbwManager.getExchangeRateManager(),
              _mbwManager.getWalletManager(false), value, assetInfo);
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
      if(_amount != null) {
         Double exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice(_account.getCoinType());
         if (exchangeRatePrice != null) {
            updateAmountsDisplay(_numberEntry.getEntry());
         }
      }
   }
}

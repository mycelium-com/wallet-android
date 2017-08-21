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

package com.mycelium.wallet.activity.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.StringHandleConfig;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.RefreshingExchangeRatesFailed;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BalanceFragment extends Fragment {
   private MbwManager _mbwManager;
   private View _root;
   private Double _exchangeRatePrice;
   private Toaster _toaster;
   @BindView(R.id.tcdFiatDisplay) ToggleableCurrencyButton _tcdFiatDisplay;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_balance_view, container, false));
      final View balanceArea = Preconditions.checkNotNull(_root.findViewById(R.id.llBalance));
      balanceArea.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            _mbwManager.getWalletManager(false).startSynchronization();
         }
      });
      ButterKnife.bind(this, _root);
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
//      setRetainInstance(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(getActivity());
      _toaster = new Toaster(activity);
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      if (_exchangeRatePrice == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }

      _tcdFiatDisplay.setCurrencySwitcher(_mbwManager.getCurrencySwitcher());
      _tcdFiatDisplay.setEventBus(_mbwManager.getEventBus());

      updateUi();
      super.onResume();
   }

   @OnClick(R.id.btSend) void onClickSend() {
      WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
      if (account instanceof ColuAccount && ((ColuAccount) account).getSatoshiAmount() == 0) {
         new AlertDialog.Builder(getActivity())
                 .setMessage(getString(R.string.rmc_send_warning, ((ColuAccount) account).getColuAsset().label))
                 .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                       SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _mbwManager.getSelectedAccount().getId(), false);
                    }
                 })
                 .create()
                 .show();
      } else {
         SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _mbwManager.getSelectedAccount().getId(), false);
      }
   }

   @OnClick(R.id.btReceive) void onClickReceive() {
      Optional<Address> receivingAddress = _mbwManager.getSelectedAccount().getReceivingAddress();
      if (receivingAddress.isPresent()) {
         ReceiveCoinsActivity.callMe(getActivity(), receivingAddress.get(),
               _mbwManager.getSelectedAccount().canSpend(), true);
      }
   }

   @OnClick(R.id.btScan) void onClickScan() {
      //perform a generic scan, act based upon what we find in the QR code
      StringHandleConfig config = StringHandleConfig.genericScanRequest();
      WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
      if(account instanceof ColuAccount) {
         config.bitcoinUriAction = StringHandleConfig.BitcoinUriAction.SEND_RMC;
         config.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.SEND_RMC;
      }
      ScanActivity.callMe(BalanceFragment.this.getActivity(), ModernMain.GENERIC_SCAN_REQUEST, config);
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      if (_mbwManager.getSelectedAccount().isArchived()) {
         return;
      }
      WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
      CurrencyBasedBalance balance;
      try {
         balance = Preconditions.checkNotNull(account.getCurrencyBasedBalance());
      } catch (IllegalArgumentException ex){
         _mbwManager.reportIgnoredException(ex);
         balance = CurrencyBasedBalance.ZERO_BITCOIN_BALANCE;
      }

      // Hide spend button if not canSpend()
      int visibility = account.canSpend() ? View.VISIBLE : View.GONE;
      _root.findViewById(R.id.btSend).setVisibility(visibility);

      updateUiKnownBalance(balance);

      TextView tvBtcRate = (TextView) _root.findViewById(R.id.tvBtcRate);
      // show / hide components depending on account type
//      View coluSatoshiBalanceLayout = _root.findViewById(R.id.llColuSatoshiBalance);
      View tcdFiatDisplay = _root.findViewById(R.id.tcdFiatDisplay);
      if(account instanceof ColuAccount) {
//          coluSatoshiBalanceLayout.setVisibility(View.VISIBLE);
         tvBtcRate.setVisibility(View.VISIBLE);
         if(((ColuAccount) account).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC){
            tcdFiatDisplay.setVisibility(View.VISIBLE);
            CurrencyValue rmcValue = ExactCurrencyValue.from(BigDecimal.ONE, "RMC");
            CurrencyValue usdValue = CurrencyValue.fromValue(rmcValue, "USD", _mbwManager.getExchangeRateManager());
            if(usdValue != null && usdValue.getValue() != null) {
               tvBtcRate.setText("1 RMC = " + usdValue.getValue().setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString() + " " + usdValue.getCurrency());
            }
         }else {
            tcdFiatDisplay.setVisibility(View.INVISIBLE);
            tvBtcRate.setText(getString(R.string.exchange_source_not_available, ((ColuAccount) account).getColuAsset().name));
         }
//          TextView tvColuSatoshiBalance = (TextView) _root.findViewById(R.id.tvColuSatoshiBalance);
//          ColuAccount coluAccount = (ColuAccount) account;
//          tvColuSatoshiBalance.setText(String.valueOf(coluAccount.getSatoshiAmount()) + " sat");
      } else {
          // restore default settings if account is standard
//          coluSatoshiBalanceLayout.setVisibility(View.GONE);
          tvBtcRate.setVisibility(View.VISIBLE);
          tcdFiatDisplay.setVisibility(View.VISIBLE);

          // Set BTC rate
          if (!_mbwManager.hasFiatCurrency()) {
             // No fiat currency selected by user
             tvBtcRate.setVisibility(View.INVISIBLE);
          } else if (_exchangeRatePrice == null) {
             // We have no price, exchange not available
             tvBtcRate.setVisibility(View.VISIBLE);
             tvBtcRate.setText(getResources().getString(R.string.exchange_source_not_available, _mbwManager.getExchangeRateManager().getCurrentExchangeSourceName() ));
          } else {
             tvBtcRate.setVisibility(View.VISIBLE);
             String currency = _mbwManager.getFiatCurrency();
             String converted = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, _exchangeRatePrice);
             tvBtcRate.setText(getResources().getString(R.string.btc_rate, currency, converted, _mbwManager.getExchangeRateManager().getCurrentExchangeSourceName()));
          }
      }
   }

   private void updateUiKnownBalance(CurrencyBasedBalance balance) {
      // Set Balance
      String valueString = Utils.getFormattedValueWithUnit(balance.confirmed, _mbwManager.getBitcoinDenomination());
      WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
      if(account instanceof ColuAccount) {
          valueString =  Utils.getColuFormattedValueWithUnit(account.getCurrencyBasedBalance().confirmed);
//         Utils.getFormattedValueWithUnit(balance.confirmed, _mbwManager.getBitcoinDenomination(), 5);
      }
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(valueString);

      _root.findViewById(R.id.pbProgress).setVisibility(balance.isSynchronizing ? View.VISIBLE : View.GONE);

      // Show alternative values
      _tcdFiatDisplay.setFiatOnly(balance.confirmed.isBtc() || (account instanceof ColuAccount && ((ColuAccount) account).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC));
      _tcdFiatDisplay.setValue(balance.confirmed);

      // Show/Hide Receiving
      if (balance.receiving.getValue().compareTo(BigDecimal.ZERO) > 0) {
         String receivingString;
         if (account instanceof ColuAccount) {
            receivingString = Utils.getColuFormattedValueWithUnit(balance.receiving);
         } else {
            receivingString = Utils.getFormattedValueWithUnit(balance.receiving, _mbwManager.getBitcoinDenomination());
         }
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) _root.findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }
      // show fiat value (if balance is in btc)
      setFiatValue(R.id.tvReceivingFiat, balance.receiving, true);

      // Show/Hide Sending
      if (balance.sending.getValue().compareTo(BigDecimal.ZERO) > 0) {
         String sendingString;
         if (account instanceof ColuAccount) {
            sendingString = Utils.getColuFormattedValueWithUnit(balance.sending);
         } else {
            sendingString = Utils.getFormattedValueWithUnit(balance.sending, _mbwManager.getBitcoinDenomination());
         }
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = (TextView) _root.findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvSending).setVisibility(View.GONE);
      }
      // show fiat value (if balance is in btc)
      setFiatValue(R.id.tvSendingFiat, balance.sending, true);
   }

   private void setFiatValue(int textViewResourceId, CurrencyValue value, boolean hideOnZeroBalance) {
      TextView tv = (TextView) _root.findViewById(textViewResourceId);
      if (!_mbwManager.hasFiatCurrency()
            || _exchangeRatePrice == null
            || (hideOnZeroBalance && value.isZero())
            || value.isFiat()
            ) {
         tv.setVisibility(View.GONE);
      } else {
         try {
            long satoshis = value.getAsBitcoin(_mbwManager.getExchangeRateManager()).getLongValue();
            tv.setVisibility(View.VISIBLE);
            String converted = Utils.getFiatValueAsString(satoshis, _exchangeRatePrice);
            String currency = _mbwManager.getFiatCurrency();
            tv.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
         } catch (IllegalArgumentException ex) {
            // something failed while calculating the bitcoin amount
            tv.setVisibility(View.GONE);
         }
      }
   }

   @Subscribe
   public void refreshingExchangeRatesFailed(RefreshingExchangeRatesFailed event){
      _toaster.toastConnectionError();
      _exchangeRatePrice = null;
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event){
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event){
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
   }

   /**
    * The selected Account changed, update UI to reflect other Balance
    */
   @Subscribe
   public void selectedAccountChanged(SelectedAccountChanged event) {
      updateUi();
   }

   /**
    * balance has changed, update UI
    */
   @Subscribe
   public void balanceChanged(BalanceChanged event) {
      updateUi();
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      updateUi();
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      updateUi();
   }

}

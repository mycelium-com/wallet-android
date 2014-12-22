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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.common.base.Preconditions;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.event.*;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Subscribe;

public class BalanceFragment extends Fragment {

   private MbwManager _mbwManager;
   private View _root;
   private ExchangeRate _exchangeRate;
   private Toaster _toaster;

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
      _exchangeRate = _mbwManager.getExchangeRateManager().getExchangeRate();
      if (_exchangeRate == null || _exchangeRate.price == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }
      _root.findViewById(R.id.btSend).setOnClickListener(sendClickListener);
      _root.findViewById(R.id.btReceive).setOnClickListener(receiveClickListener);
      _root.findViewById(R.id.btScan).setOnClickListener(scanClickListener);
      updateUi();
      super.onResume();
   }

   OnClickListener sendClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _mbwManager.getSelectedAccount().getId(), false);
      }
   };

   OnClickListener receiveClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         ReceiveCoinsActivity.callMe(getActivity(), _mbwManager.getSelectedAccount().getReceivingAddress(),
               _mbwManager.getSelectedAccount().canSpend());
      }
   };

   OnClickListener scanClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         //perform a generic scan, act based upon what we find in the QR code
         ScanActivity.callMe(BalanceFragment.this.getActivity(), ModernMain.GENERIC_SCAN_REQUEST, ScanRequest.genericScanRequest());
      }
   };

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
      Balance balance = Preconditions.checkNotNull(account.getBalance());

      if (account.canSpend()) {
         // Show spend button
         _root.findViewById(R.id.btSend).setVisibility(View.VISIBLE);
      } else {
         // Hide spend button
         _root.findViewById(R.id.btSend).setVisibility(View.GONE);
      }

      updateUiKnownBalance(balance);

      // Set BTC rate
      if (_exchangeRate == null) {
         // No rate, will probably get one soon
         _root.findViewById(R.id.tvBtcRate).setVisibility(View.INVISIBLE);
      } else if (_exchangeRate.price == null) {
         // We have no price, exchange not available
         TextView tvBtcRate = (TextView) _root.findViewById(R.id.tvBtcRate);
         tvBtcRate.setVisibility(View.VISIBLE);
         tvBtcRate.setText(getResources().getString(R.string.exchange_source_not_available, _exchangeRate.name));
      } else {
         TextView tvBtcRate = (TextView) _root.findViewById(R.id.tvBtcRate);
         tvBtcRate.setVisibility(View.VISIBLE);
         String currency = _mbwManager.getFiatCurrency();
         String converted = Utils.getFiatValueAsString(Constants.ONE_BTC_IN_SATOSHIS, _exchangeRate.price);
         tvBtcRate.setText(getResources().getString(R.string.btc_rate, currency, converted, _exchangeRate.name));
      }
   }

   private void updateUiKnownBalance(Balance balance) {

      // Set Balance
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(balance.getSpendableBalance()));

      // Set balance fiat value
      setFiatValue(R.id.tvFiat, balance.getSpendableBalance(), false);

      // Show/Hide Receiving
      // todo de-duplicate code
      if (balance.getReceivingBalance() > 0) {
         String receivingString = _mbwManager.getBtcValueString(balance.getReceivingBalance());
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) _root.findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }
      setFiatValue(R.id.tvReceivingFiat, balance.getReceivingBalance(), true);

      // Show/Hide Sending
      // todo de-duplicate code
      if (balance.getSendingBalance() != 0) {
         String sendingString = _mbwManager.getBtcValueString(balance.getSendingBalance());
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = (TextView) _root.findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvSending).setVisibility(View.GONE);
      }
      setFiatValue(R.id.tvSendingFiat, balance.getSendingBalance(), true);

   }

   private void setFiatValue(int textViewResourceId, long satoshis, boolean hideOnZeroBalance) {
      TextView tv = (TextView) _root.findViewById(textViewResourceId);
      if (_exchangeRate == null || _exchangeRate.price == null || (hideOnZeroBalance && satoshis == 0)) {
         tv.setVisibility(View.GONE);
      } else {
         tv.setVisibility(View.VISIBLE);

         String converted = Utils.getFiatValueAsString(satoshis, _exchangeRate.price);
         String currency = _mbwManager.getFiatCurrency();
         tv.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
      }

   }

   @Subscribe
   public void refreshingExchangeRatesFailed(RefreshingExchangeRatesFailed event){
      _toaster.toastConnectionError();
      _exchangeRate = null;
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event){
      _exchangeRate = _mbwManager.getExchangeRateManager().getExchangeRate();
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

}

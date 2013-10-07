package com.mycelium.wallet.activity.main;

import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;
import com.mycelium.wallet.activity.receive.WithAmountActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;

public class BalanceFragment extends Fragment implements WalletFragmentObserver {

   public interface BalanceFragmentContainer {

      public MbwManager getMbwManager();

      public void requestBalanceRefresh();

      public void addObserver(WalletFragmentObserver observer);

      public void removeObserver(WalletFragmentObserver observer);

   }

   private BalanceFragmentContainer _container;
   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private View _root;
   // private BalanceInfo _balance;
   private Double _oneBtcInFiat;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = (ViewGroup) inflater.inflate(R.layout.main_balance_view, container, false);
      return _root;
   }

   @Override
   public void onResume() {
      _container = (BalanceFragmentContainer) this.getActivity();
      _mbwManager = _container.getMbwManager();
      _recordManager = _mbwManager.getRecordManager();
      _root.findViewById(R.id.llBalance).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            _container.requestBalanceRefresh();
         }
      });

      _root.findViewById(R.id.btSend).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), getWallet(), false);
         }
      });

      _root.findViewById(R.id.btReceive).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            WithAmountActivity.callMe(BalanceFragment.this.getActivity(), _recordManager.getSelectedRecord());
         }
      });
      // _balance =
      // _container.getWallet().getLocalBalance(_mbwManager.getBlockChainAddressTracker());
      updateUi();
      _container.addObserver(this);
      super.onResume();
   }

   private Wallet getWallet() {
      return _recordManager.getWallet(_mbwManager.getWalletMode());
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public void onPause() {
      _container.removeObserver(this);
      super.onPause();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      Wallet wallet = getWallet();
      BalanceInfo balance = wallet.getLocalBalance(_mbwManager.getBlockChainAddressTracker());

      if (wallet.canSpend()) {
         // Show spend button
         _root.findViewById(R.id.btSend).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.vSendGap).setVisibility(View.VISIBLE);
      } else {
         // Hide spend button
         _root.findViewById(R.id.btSend).setVisibility(View.GONE);
         _root.findViewById(R.id.vSendGap).setVisibility(View.GONE);
      }

      if (balance == null) {
         return;
      }

      if (balance.isKnown()) {
         updateUiKnownBalance(balance);
      } else {
         updateUiUnknownBalance();
      }

      // Set BTC rate
      if (_oneBtcInFiat == null) {
         _root.findViewById(R.id.tvBtcRate).setVisibility(View.INVISIBLE);
      } else {
         TextView tvBtcRate = (TextView) _root.findViewById(R.id.tvBtcRate);
         tvBtcRate.setVisibility(View.VISIBLE);

         String currency = _mbwManager.getFiatCurrency();
         tvBtcRate.setText(getResources().getString(R.string.btc_rate, currency, _oneBtcInFiat,
               _mbwManager.getExchangeRateCalculationMode().getShortName()));

      }

   }

   private void updateUiKnownBalance(BalanceInfo balance) {

      // Set Balance
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(balance.unspent
            + balance.pendingChange));

      // Show/Hide Receiving
      if (balance.pendingReceiving > 0) {
         String receivingString = _mbwManager.getBtcValueString(balance.pendingReceiving);
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) _root.findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         ((TextView) _root.findViewById(R.id.tvReceiving)).setVisibility(View.GONE);
      }

      // Show/Hide Sending
      if (balance.pendingSending > 0) {
         String sendingString = _mbwManager.getBtcValueString(balance.pendingSending);
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = (TextView) _root.findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         ((TextView) _root.findViewById(R.id.tvSending)).setVisibility(View.GONE);
      }

      // Set Fiat value
      if (_oneBtcInFiat == null) {
         _root.findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
      } else {
         TextView tvFiat = (TextView) _root.findViewById(R.id.tvFiat);
         tvFiat.setVisibility(View.VISIBLE);

         Double converted = Utils.getFiatValue(balance.unspent + balance.pendingChange, _oneBtcInFiat);
         String currency = _mbwManager.getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));

      }
   }

   private void updateUiUnknownBalance() {

      // Show "Tap to Refresh" instead of balance
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(R.string.tap_to_refresh);

      // Hide Receiving
      ((TextView) _root.findViewById(R.id.tvSending)).setVisibility(View.GONE);

      // Hide Sending
      ((TextView) _root.findViewById(R.id.tvSending)).setVisibility(View.GONE);

      // Set Fiat value
      _root.findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
   }

   @Override
   public void balanceUpdateStarted() {
      // Show progress spinner and hide refresh icon
      _root.findViewById(R.id.pbBalance).setVisibility(View.VISIBLE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.GONE);
   }

   @Override
   public void balanceUpdateStopped() {
      // Hide progress spinner and show refresh icon
      _root.findViewById(R.id.pbBalance).setVisibility(View.GONE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.VISIBLE);
   }

   @Override
   public void walletChanged(Wallet wallet) {
      updateUi();
   }

   @Override
   public void balanceChanged(BalanceInfo info) {
      // _balance = info;
      updateUi();
   }

   @Override
   public void transactionHistoryUpdateStarted() {
   }

   @Override
   public void transactionHistoryUpdateStopped() {
   }

   @Override
   public void transactionHistoryChanged() {
   }

   @Override
   public void invoiceMapChanged(Map<String, String> invoiceMap) {
   }
   
   @Override
   public void newExchangeRate(Double oneBtcInFiat) {
      _oneBtcInFiat = oneBtcInFiat;
      updateUi();
   }

}

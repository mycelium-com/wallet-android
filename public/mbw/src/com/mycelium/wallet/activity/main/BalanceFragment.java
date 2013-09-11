package com.mycelium.wallet.activity.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NetworkConnectionWatcher.ConnectionObserver;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;
import com.mycelium.wallet.activity.receive.WithAmountActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;

public class BalanceFragment extends Fragment implements ConnectionObserver {

   public interface BalanceFragmentContainer {
      public Wallet getWallet();

      public MbwManager getMbwManager();

      public void balanceChanged(BalanceInfo balance);
   }

   private BalanceFragmentContainer _container;
   private MbwManager _mbwManager;
   private View _root;
   private AsyncTask _task;
   private BalanceInfo _balance;
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

      Wallet wallet = _container.getWallet();
      
      if (wallet.canSpend()) {
         _root.findViewById(R.id.btSend).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.vSendGap).setVisibility(View.VISIBLE);
      } else{
         _root.findViewById(R.id.btSend).setVisibility(View.GONE);
         _root.findViewById(R.id.vSendGap).setVisibility(View.GONE);
      }

      if (!Utils.isConnected(this.getActivity())) {
         Utils.toastConnectionError(this.getActivity());
      }

      _root.findViewById(R.id.llBalance).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            refresh();
         }
      });

      _root.findViewById(R.id.btSend).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _container.getWallet(), false);
         }
      });

      _root.findViewById(R.id.btReceive).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            Intent intent = new Intent(BalanceFragment.this.getActivity(), WithAmountActivity.class);
            intent.putExtra("wallet", _container.getWallet());
            startActivity(intent);
         }
      });

      refresh();
      // Register for network going up/down callbacks
      _mbwManager.getNetworkConnectionWatcher().addObserver(this);

      super.onResume();
   }

   @Override
   public void onDestroy() {
      if (_task != null) {
         _task.cancel();
      }
      super.onDestroy();
   }

   @Override
   public void onPause() {
      _mbwManager.getNetworkConnectionWatcher().removeObserver(this);
      super.onPause();
   }

   private void refresh() {
      Wallet wallet = _container.getWallet();
      
      if (_task != null) {
         return;
      }

      // Show cached balance and progress spinner
      _root.findViewById(R.id.pbBalance).setVisibility(View.VISIBLE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.GONE);
      _balance = wallet.getLocalBalance(_mbwManager.getBlockChainAddressTracker());
      updateBalance();

      // Create a task for getting the current balance
      // AndroidAsyncApi api = _mbwManager.getAsyncApi();
      // _task = api.getBalance(_record.address, new QueryBalanceHandler());
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());

   }

   private void updateBalance() {
      View me = this.getView();
      if (_balance == null) {
         return;
      }

      if (_balance.isKnown()) {
         updateKnownBalance();
      } else {
         updateUnknownBalance();
      }

      // Set BTC rate
      if (_oneBtcInFiat == null) {
         me.findViewById(R.id.tvBtcRate).setVisibility(View.INVISIBLE);
      } else {
         TextView tvBtcRate = (TextView) me.findViewById(R.id.tvBtcRate);
         tvBtcRate.setVisibility(View.VISIBLE);

         String currency = _mbwManager.getFiatCurrency();
         tvBtcRate.setText(getResources().getString(R.string.btc_rate, currency, _oneBtcInFiat, _mbwManager.getExchangeRateCalculationMode().getShortName()));

      }

   }

   private void updateKnownBalance() {

      // Set Balance
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(_mbwManager.getBtcValueString(_balance.unspent
            + _balance.pendingChange));

      // Show/Hide Receiving
      if (_balance.pendingReceiving > 0) {
         String receivingString = _mbwManager.getBtcValueString(_balance.pendingReceiving);
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = (TextView) _root.findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         ((TextView) _root.findViewById(R.id.tvReceiving)).setVisibility(View.GONE);
      }

      // Show/Hide Sending
      if (_balance.pendingSending > 0) {
         String sendingString = _mbwManager.getBtcValueString(_balance.pendingSending);
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

         Double converted = Utils.getFiatValue(_balance.unspent + _balance.pendingChange, _oneBtcInFiat);
         String currency = _mbwManager.getFiatCurrency();
         tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));

      }
   }

   private void updateUnknownBalance() {
      String questionMark = getResources().getString(R.string.question_mark);

      // Set Balance
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(questionMark);

      // Set Receiving
      String receivingText = getResources().getString(R.string.receiving, questionMark);
      ((TextView) _root.findViewById(R.id.tvReceiving)).setText(receivingText);

      // Set Sending
      String sendingText = getResources().getString(R.string.sending, questionMark);
      ((TextView) _root.findViewById(R.id.tvSending)).setText(sendingText);

      // Set Fiat value
      _root.findViewById(R.id.tvFiat).setVisibility(View.INVISIBLE);
   }

   class WalletUpdateHandler implements Wallet.WalletUpdateHandler {

      @Override
      public void walletUpdatedCallback(Wallet wallet, boolean success) {
         _root.findViewById(R.id.pbBalance).setVisibility(View.GONE);
         _root.findViewById(R.id.ivRefresh).setVisibility(View.VISIBLE);
         if (!success) {
            Utils.toastConnectionError(BalanceFragment.this.getActivity());
            _task = null;
            return;
         }
         BalanceInfo balance = wallet.getLocalBalance(_mbwManager.getBlockChainAddressTracker());
         if (!_balance.equals(balance)) {
            _balance = balance;
            updateBalance();
            // Tell our parent that the balance changed
            _container.balanceChanged(_balance);
         }
         _task = null;
      }

   }

   class QueryExchangeSummaryHandler implements AbstractCallbackHandler<ExchangeSummary[]> {

      @Override
      public void handleCallback(ExchangeSummary[] response, ApiError exception) {
         if (exception != null) {
            Utils.toastConnectionError(BalanceFragment.this.getActivity());
            _task = _container.getWallet().requestUpdate(_mbwManager.getBlockChainAddressTracker(),
                  new WalletUpdateHandler());
            _oneBtcInFiat = null;
         } else {
            _oneBtcInFiat = Utils.getLastTrade(response, _mbwManager.getExchangeRateCalculationMode());
            updateBalance();
            _task = _container.getWallet().requestUpdate(_mbwManager.getBlockChainAddressTracker(),
                  new WalletUpdateHandler());
         }
      }

   }

   @Override
   public void OnNetworkConnected() {
      if (this.getActivity().isFinishing()) {
         return;
      }
      new Handler().post(new Runnable() {
         @Override
         public void run() {
            refresh();
         }
      });
   }

   @Override
   public void OnNetworkDisconnected() {

   }

}

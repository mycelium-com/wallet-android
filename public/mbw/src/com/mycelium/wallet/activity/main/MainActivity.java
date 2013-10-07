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

package com.mycelium.wallet.activity.main;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.util.TransactionSummaryUtils;
import com.mrd.mbwapi.util.TransactionType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NetworkConnectionWatcher.ConnectionObserver;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.activity.KeyVulnerabilityDialog;
import com.mycelium.wallet.activity.RecordsActivity;
import com.mycelium.wallet.activity.addressbook.AddressBookActivity;
import com.mycelium.wallet.activity.main.AddressFragment.AddressFragmentContainer;
import com.mycelium.wallet.activity.main.BalanceFragment.BalanceFragmentContainer;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment.TransactionHistoryFragmentContainer;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;

public class MainActivity extends FragmentActivity implements BalanceFragmentContainer,
      TransactionHistoryFragmentContainer, AddressFragmentContainer, ConnectionObserver {

   /**
    * The pager widget, which handles swiping horizontally between balance view
    * and transaction history view.
    */
   private ViewPager _topPager;
   private TopPagerAdapter _topPagerAdapter;
   private ViewPager _bottomPager;
   private BottomPagerAdapter _bottomPagerAdapter;
   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private Wallet _aggregatedWallet;
   private Dialog _weakKeyDialog;
   private Handler _hintHandler;
   private AlertDialog _hintDialog;
   private BalanceInfo _oldBalance;
   private AsyncTask _task;
   List<WalletFragmentObserver> _observers;
   private Map<String, String> _invoiceMap;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main_activity);

      _observers = new LinkedList<WalletFragmentObserver>();

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();

      // Set beta build
      PackageInfo pInfo;
      try {
         pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
         ((TextView) findViewById(R.id.tvBetaBuild)).setText(getResources().getString(R.string.beta_build,
               pInfo.versionName));
      } catch (NameNotFoundException e) {
         // Ignore
      }

   }

   @Override
   protected void onResume() {
      initializeWallet();

      // Bottom view pages
      _bottomPager = (ViewPager) findViewById(R.id.pgBottom);
      _bottomPager.setOnPageChangeListener(bottomPageChanged);
      _bottomPagerAdapter = new BottomPagerAdapter(getSupportFragmentManager());
      _bottomPager.setAdapter(_bottomPagerAdapter);
      _bottomPager.setCurrentItem(_mbwManager.getMainViewFragmentIndex());
      updateBottomDots(_mbwManager.getMainViewFragmentIndex());

      _weakKeyDialog = weakKeyCheck();

      if (_weakKeyDialog == null) {
         // Delay hints by a few seconds
         _hintHandler = new Handler();
         _hintHandler.postDelayed(delayedHint, 5000);
      }

      if (!Utils.isConnected(this)) {
         Utils.toastConnectionError(this);
      }

      // Register for network going up/down callbacks
      _mbwManager.getNetworkConnectionWatcher().addObserver(this);

      // Request a slightly delayed balance refresh. This allows all fragments
      // to be hooked up properly first
      new Handler().postDelayed(new Runnable() {

         @Override
         public void run() {
            requestBalanceRefresh();
         }
      }, 50);

      super.onResume();
   }

   @Override
   protected void onPause() {
      if (_hintHandler != null) {
         _hintHandler.removeCallbacks(delayedHint);
      }
      _mbwManager.getNetworkConnectionWatcher().removeObserver(this);
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      if (_hintDialog != null && _hintDialog.isShowing()) {
         _hintDialog.dismiss();
      }
      if (_weakKeyDialog != null && _weakKeyDialog.isShowing()) {
         _weakKeyDialog.dismiss();
      }
      if (_task != null) {
         _task.cancel();
      }
      super.onDestroy();
   }

   private void initializeWallet() {
      Wallet wallet = _mbwManager.getRecordManager().getWallet(WalletMode.Aggregated);
      // Clear the old balance if we have a new wallet
      if (!wallet.equals(_aggregatedWallet)) {
         _oldBalance = null;
      }
      _aggregatedWallet = wallet;

      // Determine whether we are working with the archive
      boolean isArchivedKey = _mbwManager.getRecordManager().getSelectedRecord().tag == Tag.ARCHIVE;

      // Top view pages showing addresses
      _topPager = (ViewPager) findViewById(R.id.pgTop);
      _topPager.setOnPageChangeListener(topPageChanged);
      _topPagerAdapter = new TopPagerAdapter(getSupportFragmentManager(), _aggregatedWallet);
      _topPager.setAdapter(_topPagerAdapter);

      // Show the fragment that has the receiving address
      // Find the address index of the currently selected
      List<Address> addresses = _aggregatedWallet.getAddresses();
      int addressIndex = _aggregatedWallet.getIndexOfReceivingAddress();
      _topPager.setCurrentItem(addressIndex);

      // Add a textual notice if we are working on a key in the archive
      if (isArchivedKey) {
         // Show notice
         TextView notice = (TextView) findViewById(R.id.tvKeyInfo);
         notice.setText(R.string.managing_archive_key);
         notice.setTextColor(getResources().getColor(R.color.darkyellow));
         notice.setVisibility(View.VISIBLE);
         findViewById(R.id.llSwipeDots).setVisibility(View.GONE);
         findViewById(R.id.gap).setVisibility(View.GONE);
      } else if (addresses.size() > 1) {
         // Show dots
         updateSwipeDots(addresses.size(), addressIndex);
         findViewById(R.id.tvKeyInfo).setVisibility(View.GONE);
         findViewById(R.id.llSwipeDots).setVisibility(View.VISIBLE);
         findViewById(R.id.gap).setVisibility(View.GONE);
      } else {
         // Show a gap
         findViewById(R.id.tvKeyInfo).setVisibility(View.GONE);
         findViewById(R.id.llSwipeDots).setVisibility(View.GONE);
         findViewById(R.id.gap).setVisibility(View.VISIBLE);
      }

   }

   private void updateSwipeDots(int dots, int selectedIndex) {
      LinearLayout llSwipeDots = (LinearLayout) findViewById(R.id.llSwipeDots);
      llSwipeDots.removeAllViews();
      Utils.addHorizontalSwipeDotView(this, llSwipeDots, dots, selectedIndex);
      llSwipeDots.refreshDrawableState();
   }

   OnPageChangeListener topPageChanged = new OnPageChangeListener() {

      @Override
      public void onPageSelected(int position) {
         List<Address> addresses = _aggregatedWallet.getAddresses();
         Address address = addresses.get(position);
         if (address.equals(_aggregatedWallet.getReceivingAddress())) {
            // Already the selected address
            return;
         }
         _aggregatedWallet.changeReceivingAddress(address);
         _recordManager.setSelectedRecord(_aggregatedWallet.getReceivingAddress());
         updateSwipeDots(addresses.size(), position);
         notifyWalletChanged(_aggregatedWallet);
      }

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }
   };

   OnPageChangeListener bottomPageChanged = new OnPageChangeListener() {

      @Override
      public void onPageSelected(int position) {
         _mbwManager.setMainViewFragmentIndex(position);
         updateBottomDots(position);
      }

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }
   };

   private void updateBottomDots(int position) {
      Drawable full = getResources().getDrawable(R.drawable.circle_full_white);
      Drawable line = getResources().getDrawable(R.drawable.circle_line_white);

      if (position == 0) {
         ((ImageView) findViewById(R.id.ivDotOne)).setImageDrawable(full);
         ((ImageView) findViewById(R.id.ivDotTwo)).setImageDrawable(line);
      } else {
         ((ImageView) findViewById(R.id.ivDotOne)).setImageDrawable(line);
         ((ImageView) findViewById(R.id.ivDotTwo)).setImageDrawable(full);
      }
   }

   @Override
   public void onBackPressed() {
      if (_bottomPager.getCurrentItem() == 1) {
         // The user is currently looking at the transaction history fragment,
         // go to balance view
         _bottomPager.setCurrentItem(0);
      } else {
         // The suer is looking at the balance view allow the system
         // to handle the Back button. This calls finish() on this activity and
         // pops the back stack.
         super.onBackPressed();

      }
   }

   private class TopPagerAdapter extends FragmentStatePagerAdapter {

      private Wallet _myWallet;
      private int _numAddresses;

      public TopPagerAdapter(FragmentManager fm, Wallet wallet) {
         super(fm);
         _myWallet = wallet;
         _numAddresses = _myWallet.getAddresses().size();
      }

      @Override
      public Fragment getItem(int position) {
         Address address = _myWallet.getAddresses().get(position);
         Record record = _recordManager.getRecord(address);
         return AddressFragment.newInstance(record, position, _numAddresses);
      }

      @Override
      public int getCount() {
         return _numAddresses;
      }
   }

   private class BottomPagerAdapter extends FragmentStatePagerAdapter {

      private BalanceFragment _balanceFragment;
      private TransactionHistoryFragment _transactionHistoryFragment;

      public BottomPagerAdapter(FragmentManager fm) {
         super(fm);
         _balanceFragment = new BalanceFragment();
         _transactionHistoryFragment = new TransactionHistoryFragment();
      }

      @Override
      public Fragment getItem(int position) {
         if (position == 0) {
            return _balanceFragment;
         } else if (position == 1) {
            return _transactionHistoryFragment;
         } else {
            throw new RuntimeException("Invalid fragment position");
         }
      }

      @Override
      public int getCount() {
         return 2;
      }
   }

   private Dialog weakKeyCheck() {
      List<Record> weakKeys = _mbwManager.getRecordManager().getWeakActiveKeys();
      if (weakKeys.isEmpty()) {
         return null;
      }

      KeyVulnerabilityDialog dialog = new KeyVulnerabilityDialog(this, weakKeys);
      dialog.show();
      return dialog;
   }

   private Runnable delayedHint = new Runnable() {

      @Override
      public void run() {
         if (_mbwManager.getHintManager().timeForAHint()) {
            _hintDialog = _mbwManager.getHintManager().showHint(MainActivity.this);
         }
      }
   };

   @Override
   public MbwManager getMbwManager() {
      return _mbwManager;
   }

   /**
    * Called when menu button is pressed.
    */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.main_activity_options_menu, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      boolean expertMode = _mbwManager.getExpertMode();
      menu.findItem(R.id.miKeyManagement).setVisible(expertMode);
      menu.findItem(R.id.miExport).setVisible(!expertMode);
      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miSettings) {
         Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miAddressBook) {
         AddressBookActivity.callMe(this);
         return true;
      } else if (item.getItemId() == R.id.miKeyManagement) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedKeyManagement);
         return true;
      } else if (item.getItemId() == R.id.miExport) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedExport);
         return true;
      } else if (item.getItemId() == R.id.miColdStorage) {
         InstantWalletActivity.callMe(this);
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   final Runnable pinProtectedKeyManagement = new Runnable() {

      @Override
      public void run() {
         Intent intent = new Intent(MainActivity.this, RecordsActivity.class);
         startActivity(intent);
      }
   };

   final Runnable pinProtectedExport = new Runnable() {

      @Override
      public void run() {

         Record record = _mbwManager.getRecordManager().getSelectedRecord();
         if (record == null) {
            return;
         }
         Utils.exportPrivateKey(record, MainActivity.this);
      }
   };

   private void refreshExchangeRateAndBalance() {
      if (_task != null) {
         _task.cancel();
      }
      notifyBalanceUpdateRequestStarted();
      // Query exchange rate followed by a refresh of the wallet
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getExchangeSummary(_mbwManager.getFiatCurrency(), new QueryExchangeSummaryHandler());
   }

   class QueryExchangeSummaryHandler implements AbstractCallbackHandler<ExchangeSummary[]> {

      @Override
      public void handleCallback(ExchangeSummary[] response, ApiError exception) {
         if (exception != null) {
            Utils.toastConnectionError(MainActivity.this);
            _task = _aggregatedWallet.requestUpdate(_mbwManager.getBlockChainAddressTracker(),
                  new WalletUpdateHandler());
            notifyNewExchangeRate(null);
         } else {
            Double oneBtcInFiat = Utils.getLastTrade(response, _mbwManager.getExchangeRateCalculationMode());
            notifyNewExchangeRate(oneBtcInFiat);
            _task = _aggregatedWallet.requestUpdate(_mbwManager.getBlockChainAddressTracker(),
                  new WalletUpdateHandler());
         }
      }

   }

   class WalletUpdateHandler implements Wallet.WalletUpdateHandler {

      @Override
      public void walletUpdatedCallback(Wallet wallet, boolean success) {
         notifyBalanceUpdateRequestStopped();
         if (!success) {
            Utils.toastConnectionError(MainActivity.this);
            _task = null;
            return;
         }
         BalanceInfo balance = wallet.getLocalBalance(_mbwManager.getBlockChainAddressTracker());
         if (!balance.equals(_oldBalance)) {
            notifyBalanceChanged(balance);
            _oldBalance = balance;
            refreshTransactionHistoryAndInvoices();
         } else {
            _task = null;
         }
      }

   }

   private void refreshTransactionHistoryAndInvoices() {
      if (_task != null) {
         _task.cancel();
      }
      notifyTransactionHistoryUpdateRequestStarted();
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getTransactionSummary(_aggregatedWallet.getAddressSet(), new QueryTransactionSummaryHandler());
   }

   class QueryTransactionSummaryHandler implements AbstractCallbackHandler<QueryTransactionSummaryResponse> {

      @Override
      public void handleCallback(QueryTransactionSummaryResponse response, ApiError exception) {
         notifyTransactionHistoryUpdateRequestStopped();
         if (exception != null) {
            _task = null;
            Utils.toastConnectionError(MainActivity.this);
         } else {
            notifyTransactionHistoryChanged();
            _task = fetchInvoices(response);
         }
      }

   }

   private AsyncTask fetchInvoices(QueryTransactionSummaryResponse response) {
      Set<Address> addressSet = _aggregatedWallet.getAddressSet();
      List<String> addresses = new LinkedList<String>();
      for (TransactionSummary t : response.transactions) {
         TransactionType type = TransactionSummaryUtils.getTransactionType(t, addressSet);
         if (type == TransactionType.SentToOthers) {
            String[] candidates = TransactionSummaryUtils.getReceiversNotMe(t, addressSet);
            if (candidates.length == 1) {
               addresses.add(candidates[0]);
            }
         }
      }
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      return api.lookupInvoices(addresses, new LookupInvoicesHandler());
   }

   class LookupInvoicesHandler implements AbstractCallbackHandler<Map<String, String>> {

      @Override
      public void handleCallback(Map<String, String> response, ApiError exception) {
         if (exception != null) {
            // Ignore
            _task = null;
         } else {
            _invoiceMap = response;
            notifyInvoiceMapChanged(_invoiceMap);
         }
         _task = null;
      }
   }

   @Override
   public void OnNetworkConnected() {
      if (this.isFinishing()) {
         return;
      }
      new Handler().post(new Runnable() {
         @Override
         public void run() {
            refreshExchangeRateAndBalance();
         }
      });
   }

   @Override
   public void OnNetworkDisconnected() {
   }

   private void notifyWalletChanged(Wallet wallet) {
      for (WalletFragmentObserver o : _observers) {
         o.walletChanged(wallet);
      }
   }

   private void notifyBalanceUpdateRequestStarted() {
      for (WalletFragmentObserver o : _observers) {
         o.balanceUpdateStarted();
      }
   }

   private void notifyBalanceUpdateRequestStopped() {
      for (WalletFragmentObserver o : _observers) {
         o.balanceUpdateStopped();
      }
   }

   private void notifyBalanceChanged(BalanceInfo info) {
      for (WalletFragmentObserver o : _observers) {
         o.balanceChanged(info);
      }
   }

   private void notifyTransactionHistoryUpdateRequestStarted() {
      for (WalletFragmentObserver o : _observers) {
         o.transactionHistoryUpdateStarted();
      }
   }

   private void notifyTransactionHistoryUpdateRequestStopped() {
      for (WalletFragmentObserver o : _observers) {
         o.transactionHistoryUpdateStopped();
      }
   }

   private void notifyTransactionHistoryChanged() {
      for (WalletFragmentObserver o : _observers) {
         o.transactionHistoryChanged();
      }
   }

   private void notifyInvoiceMapChanged(Map<String, String> invoiceMap) {
      for (WalletFragmentObserver o : _observers) {
         o.invoiceMapChanged(invoiceMap);
      }
   }

   private void notifyNewExchangeRate(Double oneBtcInFiat) {
      for (WalletFragmentObserver o : _observers) {
         o.newExchangeRate(oneBtcInFiat);
      }
   }

   public void addObserver(WalletFragmentObserver observer) {
      _observers.add(observer);
   }

   public void removeObserver(WalletFragmentObserver observer) {
      _observers.remove(observer);
   }

   @Override
   public void requestBalanceRefresh() {
      refreshExchangeRateAndBalance();
   }

   @Override
   public void requestTransactionHistoryRefresh() {
      refreshTransactionHistoryAndInvoices();
   }

}

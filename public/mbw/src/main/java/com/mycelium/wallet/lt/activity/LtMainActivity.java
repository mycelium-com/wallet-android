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

package com.mycelium.wallet.lt.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Preconditions;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.activity.buy.SellOrderSearchFragment;
import com.mycelium.wallet.lt.activity.sell.SellOrdersFragment;
import com.mycelium.wallet.lt.api.GetTraderInfo;

public class LtMainActivity extends ActionBarActivity {

   public enum TAB_TYPE {
      DEFAULT, ACTIVE_TRADES, TRADE_HISTORY
   };

   public static void callMe(Context context, TAB_TYPE tabToSelect) {
      Intent intent = createIntent(context, tabToSelect);
      context.startActivity(intent);
   }

   public static Intent createIntent(Context context, TAB_TYPE tabToSelect) {
      Intent intent = new Intent(context, LtMainActivity.class);
      intent.putExtra("tabToSelect", tabToSelect.ordinal());
      return intent;
   }

   @SuppressWarnings("unused")
   private static final String TAG = "LtMainActivity";

   private ViewPager _viewPager;
   private TabsAdapter _tabsAdapter;
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   ActionBar.Tab _myNearestTradesTab;
   ActionBar.Tab _myActiveTradesTab;
   ActionBar.Tab _myTradeHistoryTab;
   ActionBar.Tab _mySellOrdersTab;
   ActionBar.Tab _myTraderInfoTab;
   ActionBar _actionBar;
   private TAB_TYPE _tabToSelect;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _viewPager = new ViewPager(this);
      _viewPager.setId(R.id.pager);

      setContentView(_viewPager);

      _actionBar = getSupportActionBar();
      _actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

      _tabsAdapter = new TabsAdapter(this, _viewPager);

      // Add Sell Orders tab
      _myNearestTradesTab = _actionBar.newTab();
      _myNearestTradesTab.setText(getResources().getString(R.string.lt_buy_bitcoin_tab));
      _tabsAdapter.addTab(_myNearestTradesTab, SellOrderSearchFragment.class, null);

      // Add Active Trades tab
      _myActiveTradesTab = _actionBar.newTab();
      _myActiveTradesTab.setText(getResources().getString(R.string.lt_active_trades_tab));
      _tabsAdapter.addTab(_myActiveTradesTab, ActiveTradesFragment.class, null);

      // Add Historic Trades tab
      _myTradeHistoryTab = _actionBar.newTab();
      _myTradeHistoryTab.setText(getResources().getString(R.string.lt_trade_history_tab));
      _tabsAdapter.addTab(_myTradeHistoryTab, TradeHistoryFragment.class, null);

      // Add Sell Orders tab
      _mySellOrdersTab = _actionBar.newTab();
      _mySellOrdersTab.setText(getResources().getString(R.string.lt_my_sell_orders_tab));
      _mySellOrdersTab.setTag(_tabsAdapter.getCount());
      _tabsAdapter.addTab(_mySellOrdersTab, SellOrdersFragment.class, null);

      // Add Trader Info tab
      _myTraderInfoTab = _actionBar.newTab();
      _myTraderInfoTab.setText(getResources().getString(R.string.lt_my_trader_info_tab));
      _myTraderInfoTab.setTag(_tabsAdapter.getCount());
      _tabsAdapter.addTab(_myTraderInfoTab, TraderInfoFragment.class, null);

      // Load the tab to select from intent
      _tabToSelect = TAB_TYPE.values()[getIntent().getIntExtra("tabToSelect", TAB_TYPE.DEFAULT.ordinal())];
      _actionBar.selectTab(enumToTab(_tabToSelect));

   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.lt_sell_orders_options_global, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      final int tabIdx = _viewPager.getCurrentItem();

      // Add Sell Order menu
      final boolean isSellOrders = tabIdx == ((TabsAdapter.TabInfo) _mySellOrdersTab.getTag()).index;
      Preconditions.checkNotNull(menu.findItem(R.id.miAddSellOrder)).setVisible(isSellOrders);

      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      checkGooglePlayServices();
//      _ltManager.enableNotifications(false);
      _ltManager.subscribe(ltSubscriber);
      _ltManager.startMonitoringTrader();
      if (_ltManager.hasLocalTraderAccount()) {
         _ltManager.makeRequest(new GetTraderInfo());
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      _ltManager.stopMonitoringTrader();
      _ltManager.unsubscribe(ltSubscriber);
//      _ltManager.enableNotifications(true);
      super.onPause();
   }

   /**
    * figure out whether Google Play Services are available and act accordingly
    */
   private boolean checkGooglePlayServices() {
      int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
      if (result == ConnectionResult.SERVICE_MISSING || result == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
            || result == ConnectionResult.SERVICE_DISABLED) {
         GooglePlayServicesUtil.getErrorDialog(result, this, 0).show();
         return false;
      } else if (result != ConnectionResult.SUCCESS) {
         // Warn about degraded notifications
         Utils.showOptionalMessage(this, R.string.lt_google_play_services_not_available);
         return false;
      } else {
         _ltManager.initializeGooglePlayServices();
         return true;
      }
   }

   private Tab enumToTab(TAB_TYPE tabType) {
      switch (tabType) {
      case ACTIVE_TRADES:
         return _myActiveTradesTab;
      case TRADE_HISTORY:
         return _myTradeHistoryTab;
      default:
         return _myNearestTradesTab;
      }
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         if (errorCode == LtApi.ERROR_CODE_INCOMPATIBLE_API_VERSION) {
            Toast.makeText(LtMainActivity.this, R.string.lt_error_incompatible_version, Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(LtMainActivity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         }
      }

      @Override
      public boolean onLtNoTraderAccount() {
         return true;
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(LtMainActivity.this);
         return true;
      };

      @Override
      public void onLtTraderActicityNotification() {
         _ltManager.makeRequest(new GetTraderInfo());
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         // Mark that we have seen this version of the trader
         _ltManager.setLastTraderSynchronization(info.lastChange);
      }

   };

}

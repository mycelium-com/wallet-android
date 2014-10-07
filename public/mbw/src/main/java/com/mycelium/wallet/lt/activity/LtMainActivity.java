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

package com.mycelium.wallet.lt.activity;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.common.base.Preconditions;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.model.TraderInfo;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.activity.buy.AdSearchFragment;
import com.mycelium.wallet.lt.activity.sell.AdsFragment;
import com.mycelium.wallet.lt.api.GetTraderInfo;

public class LtMainActivity extends ActionBarActivity {

   public enum TAB_TYPE {
      DEFAULT, ACTIVE_TRADES, TRADE_HISTORY, MY_ADS
   }

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
   ActionBar.Tab _myBuyBitcoinTab;
   ActionBar.Tab _mySellBitcoinTab;
   ActionBar.Tab _myActiveTradesTab;
   ActionBar.Tab _myTradeHistoryTab;
   ActionBar.Tab _myAdsTab;
   ActionBar.Tab _myTraderInfoTab;
   ActionBar _actionBar;
   private TAB_TYPE _tabToSelect;
   private boolean _hasWelcomed;
   private Ringtone _updateSound;

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
      // to provide up navigation from actionbar, in case the modern main
      // activity is not on the stack
      _actionBar.setDisplayHomeAsUpEnabled(true);

      _tabsAdapter = new TabsAdapter(this, _viewPager);

      // Add Buy Bitcoin tab
      _myBuyBitcoinTab = _actionBar.newTab();
      _myBuyBitcoinTab.setText(getResources().getString(R.string.lt_buy_bitcoin_tab));
      _tabsAdapter.addTab(_myBuyBitcoinTab, AdSearchFragment.class, AdSearchFragment.createArgs(true));

      // Add Sell Bitcoin tab
      _mySellBitcoinTab = _actionBar.newTab();
      _mySellBitcoinTab.setText(getResources().getString(R.string.lt_sell_bitcoin_tab));
      _tabsAdapter.addTab(_mySellBitcoinTab, AdSearchFragment.class, AdSearchFragment.createArgs(false));

      // Add Active Trades tab
      _myActiveTradesTab = _actionBar.newTab();
      _myActiveTradesTab.setText(getResources().getString(R.string.lt_active_trades_tab));
      _tabsAdapter.addTab(_myActiveTradesTab, ActiveTradesFragment.class, null);

      // Add Historic Trades tab
      _myTradeHistoryTab = _actionBar.newTab();
      _myTradeHistoryTab.setText(getResources().getString(R.string.lt_trade_history_tab));
      _tabsAdapter.addTab(_myTradeHistoryTab, TradeHistoryFragment.class, null);

      // Add Ads tab
      _myAdsTab = _actionBar.newTab();
      _myAdsTab.setText(getResources().getString(R.string.lt_my_ads_tab));
      _myAdsTab.setTag(_tabsAdapter.getCount());
      _tabsAdapter.addTab(_myAdsTab, AdsFragment.class, null);

      // Add Trader Info tab
      _myTraderInfoTab = _actionBar.newTab();
      _myTraderInfoTab.setText(getResources().getString(R.string.lt_my_trader_info_tab));
      _myTraderInfoTab.setTag(_tabsAdapter.getCount());
      _tabsAdapter.addTab(_myTraderInfoTab, MyInfoFragment.class, null);

      // Load the tab to select from intent
      _tabToSelect = TAB_TYPE.values()[getIntent().getIntExtra("tabToSelect", TAB_TYPE.DEFAULT.ordinal())];
      _actionBar.selectTab(enumToTab(_tabToSelect));

      _updateSound = RingtoneManager
            .getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

      // Load saved state
      if (savedInstanceState != null) {
         _hasWelcomed = savedInstanceState.getBoolean("hasWelcomed", false);
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.lt_ads_options_global, menu);
      inflater.inflate(R.menu.lt_activity_options_menu, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      final int tabIdx = _viewPager.getCurrentItem();

      // Add Ad menu
      final boolean isAdsTab = tabIdx == ((TabsAdapter.TabInfo) _myAdsTab.getTag()).index;
      Preconditions.checkNotNull(menu.findItem(R.id.miAddAd)).setVisible(isAdsTab);

      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putBoolean("hasWelcomed", _hasWelcomed);
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      checkGooglePlayServices();
      //showWelcomeMessage();
      // _ltManager.enableNotifications(false);
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
      // _ltManager.enableNotifications(true);
      super.onPause();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      final int itemId = item.getItemId();
      if (itemId == R.id.miHowTo) {
         openLocalTraderHelp();
      }
      if (itemId == android.R.id.home) {
        // Respond to the action bar's home button, navigates to parent activity
        Intent intent = new Intent(this, ModernMain.class);
        //This flag causes the back stack to be cleared until modern main is on top
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        finish();
        return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private void openLocalTraderHelp() {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(Constants.LOCAL_TRADER_HELP_URL));
      startActivity(intent);
      Toast.makeText(this, R.string.going_to_mycelium_com_help, Toast.LENGTH_LONG).show();
   }

//  Welcome message disabled for now as the text is outdated. Kept the code so we can change the text later on if we wish
//   /**
//    * Show welcome message
//    */
//   private void showWelcomeMessage() {
//      if (!_hasWelcomed) {
//         // Only show welcome message per activity instance
//         _hasWelcomed = true;
//         Utils.showOptionalMessage(this, R.string.lt_welcome_message);
//      }
//   }

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
      case MY_ADS:
         return _myAdsTab;
      default:
         return _myBuyBitcoinTab;
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
      }

      @Override
      public void onLtTraderActicityNotification(long timestamp) {
         if (_ltManager.getLastNotificationSoundTimestamp() < timestamp) {
            _ltManager.setLastNotificationSoundTimestamp(timestamp);
            // Vibrate
            _mbwManager.vibrate();
            // Make a sound if applicable
            if (_ltManager.getPlaySoundOnTradeNotification()) {
               if (_updateSound != null) {
                  _updateSound.play();
               }
            }
         }
         _ltManager.makeRequest(new GetTraderInfo());
      }

      @Override
      public void onLtTraderInfoFetched(TraderInfo info, GetTraderInfo request) {
         // Mark that we have seen this version of the trader
         _ltManager.setLastTraderSynchronization(info.lastChange);
      }

   };

}

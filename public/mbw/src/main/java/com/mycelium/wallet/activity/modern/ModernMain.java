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

package com.mycelium.wallet.activity.modern;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AboutActivity;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.main.BalanceMasterFragment;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;
import com.mycelium.wallet.event.RefreshStatus;
import com.squareup.otto.Subscribe;

public class ModernMain extends ActionBarActivity {
   
   private MbwManager _mbwManager;

   ViewPager mViewPager;
   TabsAdapter mTabsAdapter;
   TextView tabCenter;
   TextView tabText;
   ActionBar.Tab mBalanceTab;
   private MenuItem refreshItem;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);

      mViewPager = new ViewPager(this);
      mViewPager.setId(R.id.pager);

      setContentView(mViewPager);

      ActionBar bar = getSupportActionBar();

      /*
       * bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.
       * actionbar_background)); bar.setIcon(new
       * ColorDrawable(getResources().getColor(android.R.color.transparent)));
       */

      bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      bar.setDisplayOptions(1, ActionBar.DISPLAY_SHOW_TITLE);
      mTabsAdapter = new TabsAdapter(this, mViewPager, _mbwManager);

      mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tab_keys)), RecordsFragment.class, null);
      mBalanceTab = bar.newTab();
      mTabsAdapter.addTab(mBalanceTab.setText(getString(R.string.tab_balance)), BalanceMasterFragment.class, null);
      mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tab_transactions)), TransactionHistoryFragment.class,
            null);
      final Bundle addressBookConfig = new Bundle();
      addressBookConfig.putBoolean(AddressBookFragment.SELECT_ONLY, false);
      mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tab_addresses)), AddressBookFragment.class,
            addressBookConfig);
      // mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.explore)),
      // ExploreFragment.class,null);
      bar.selectTab(mBalanceTab);
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);
      _mbwManager.getSyncManager().triggerUpdate();
      supportInvalidateOptionsMenu();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   public void onBackPressed() {
      ActionBar bar = getSupportActionBar();
      if (bar.getSelectedTab() == mBalanceTab) {
         super.onBackPressed();
      } else {
         bar.selectTab(mBalanceTab);
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.main_activity_options_menu, menu);
      inflater.inflate(R.menu.refresh, menu);
      inflater.inflate(R.menu.record_options_menu_global, menu);
      inflater.inflate(R.menu.addressbook_options_global, menu);
      return true;
   }

   // controlling the behavior here is the safe but slightly slower responding
   // way of doing this.
   // controlling the visibility from the individual fragments is a bug-ridden
   // nightmare.
   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      final int tabIdx = mViewPager.getCurrentItem();

      // Add Record menu
      final boolean isRecords = tabIdx == 0;
      final boolean expertMode = _mbwManager.getExpertMode();
      final boolean locked = _mbwManager.isKeyManagementLocked();
      Preconditions.checkNotNull(menu.findItem(R.id.miAddRecord)).setVisible(isRecords && expertMode && !locked);

      // Lock menu
      final boolean hasPin = _mbwManager.isPinProtected();
      Preconditions.checkNotNull(menu.findItem(R.id.miLockKeys)).setVisible(
            isRecords && expertMode && !locked && hasPin);

      // Refresh menu
      final boolean isBalance = tabIdx == 1;
      final boolean isHistory = tabIdx == 2;
      refreshItem = Preconditions.checkNotNull(menu.findItem(R.id.miRefresh));
      refreshItem.setVisible(isBalance | isHistory);
      setRefreshAnimation(_mbwManager.getSyncManager().currentStatus());
      final boolean isAddressBook = tabIdx == 3;
      Preconditions.checkNotNull(menu.findItem(R.id.miAddAddress)).setVisible(isAddressBook);
      // only enable explore item if the permission is given
      // Disabled functionality
      // Preconditions.checkNotNull(menu.findItem(R.id.miExplore)).setVisible(canObtainLocation());

      return super.onPrepareOptionsMenu(menu);
   }

   @SuppressWarnings("unused")
   private boolean canObtainLocation() {
      final boolean hasFeature = getPackageManager().hasSystemFeature("android.hardware.location.network");
      if (!hasFeature)
         return false;
      if (!hasFeature)
         return false;

      String permission = "android.permission.ACCESS_COARSE_LOCATION";
      int res = checkCallingOrSelfPermission(permission);
      return (res == PackageManager.PERMISSION_GRANTED);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      final int itemId = item.getItemId();
      if (itemId == R.id.miColdStorage) {
         InstantWalletActivity.callMe(this);
         return true;
      } else if (itemId == R.id.miSettings) {
         Intent intent = new Intent(this, SettingsActivity.class);
         startActivity(intent);
         return true;
      } else if (itemId == R.id.miBackup) {
         Utils.pinProtectedBackup(this);
         return true;
      } else if (itemId == R.id.miVerifyBackup) {
         VerifyBackupActivity.callMe(this);
         return true;
      } else if (itemId == R.id.miRefresh) {
         _mbwManager.getSyncManager().triggerUpdate();
      } else if (itemId == R.id.miExplore) {
         _mbwManager.getExploreHelper().redirectToCoinmap(this);
      } else if (itemId == R.id.miHelp) {
         openMyceliumHelp();
      } else if (itemId == R.id.miAbout) {
         Intent intent = new Intent(this, AboutActivity.class);
         startActivity(intent);
      }
      return super.onOptionsItemSelected(item);
   }

   private void openMyceliumHelp() {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(Constants.MYCELIUM_WALLET_HELP_URL));
      startActivity(intent);
      Toast.makeText(this, R.string.going_to_mycelium_com_help, Toast.LENGTH_LONG).show();
   }
   
   @Subscribe
   public void setRefreshAnimation(RefreshStatus refreshAnimation) {
      if (refreshItem != null) {
         if (refreshAnimation.isRunning) {
            MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
         } else {
            MenuItemCompat.setActionView(refreshItem, null);
         }
      } else {
         Log.i(Constants.TAG, "unable to set refresh animation since the item is not there..");
      }
   }

}

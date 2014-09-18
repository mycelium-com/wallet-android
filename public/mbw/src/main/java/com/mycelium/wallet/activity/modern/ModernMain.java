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
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.event.*;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.squareup.otto.Subscribe;

import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AboutActivity;
import com.mycelium.wallet.activity.main.BalanceMasterFragment;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;

import java.util.UUID;

public class ModernMain extends ActionBarActivity {

   public static final int GENERIC_SCAN_REQUEST = 4;
   private static final int REQUEST_SETTING_CHANGED = 5;
   private MbwManager _mbwManager;

   ViewPager mViewPager;
   TabsAdapter mTabsAdapter;
   ActionBar.Tab mBalanceTab;
   ActionBar.Tab mAccountsTab;
   private MenuItem refreshItem;
   private Toaster _toaster;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);
      mViewPager = new ViewPager(this);
      mViewPager.setId(R.id.pager);
      setContentView(mViewPager);
      ActionBar bar = getSupportActionBar();
      bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
      bar.setDisplayOptions(1, ActionBar.DISPLAY_SHOW_TITLE);

      mTabsAdapter = new TabsAdapter(this, mViewPager, _mbwManager);
      mAccountsTab = bar.newTab();
      mTabsAdapter.addTab(mAccountsTab.setText(getString(R.string.tab_accounts)), AccountsFragment.class, null);
      mBalanceTab = bar.newTab();
      mTabsAdapter.addTab(mBalanceTab.setText(getString(R.string.tab_balance)), BalanceMasterFragment.class, null);
      mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tab_transactions)), TransactionHistoryFragment.class, null);
      final Bundle addressBookConfig = new Bundle();
      addressBookConfig.putBoolean(AddressBookFragment.SELECT_ONLY, false);
      mTabsAdapter.addTab(bar.newTab().setText(getString(R.string.tab_addresses)), AddressBookFragment.class, addressBookConfig);

      bar.selectTab(mBalanceTab);
      _toaster = new Toaster(this);

   }

   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      // Start WAPI & classic synchronization as a delayed action. This way we don't immediately block the account
      // while synchronizing
      Handler h = new Handler();
      h.postDelayed(new Runnable() {
         @Override
         public void run() {
            _mbwManager.getVersionManager().checkForUpdate();
            _mbwManager.getWalletManager(false).startSynchronization();
         }
      }, 5);

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
      addEnglishSetting(menu.findItem(R.id.miSettings));
      inflater.inflate(R.menu.refresh, menu);
      inflater.inflate(R.menu.record_options_menu_global, menu);
      inflater.inflate(R.menu.addressbook_options_global, menu);
      return true;
   }

   private void addEnglishSetting(MenuItem settingsItem) {
      String displayed = getResources().getString(R.string.settings);
      String settingsEn = Utils.loadEnglish(R.string.settings);
      if (!settingsEn.equals(displayed)) {
         settingsItem.setTitle(settingsItem.getTitle() + " (" + settingsEn + ")");
      }
   }

   // controlling the behavior here is the safe but slightly slower responding
   // way of doing this.
   // controlling the visibility from the individual fragments is a bug-ridden
   // nightmare.
   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      final int tabIdx = mViewPager.getCurrentItem();

      // Only show backup option if we haven't done a backup yet
      Preconditions.checkNotNull(menu.findItem(R.id.miBackup)).setVisible(_mbwManager.getMetadataStorage().getMasterSeedBackupState() != MetadataStorage.BackupState.VERIFIED);

      // Add Record menu
      final boolean isRecords = tabIdx == 0;
      final boolean locked = _mbwManager.isKeyManagementLocked();
      Preconditions.checkNotNull(menu.findItem(R.id.miAddRecord)).setVisible(isRecords && !locked);

      // Lock menu
      final boolean hasPin = _mbwManager.isPinProtected();
      Preconditions.checkNotNull(menu.findItem(R.id.miLockKeys)).setVisible(isRecords && !locked && hasPin);

      // Refresh menu
      final boolean isBalance = tabIdx == 1;
      final boolean isHistory = tabIdx == 2;
      refreshItem = Preconditions.checkNotNull(menu.findItem(R.id.miRefresh));
      refreshItem.setVisible(isBalance | isHistory);
      setRefreshAnimation();
      final boolean isAddressBook = tabIdx == 3;
      Preconditions.checkNotNull(menu.findItem(R.id.miAddAddress)).setVisible(isAddressBook);

      return super.onPrepareOptionsMenu(menu);
   }

   @SuppressWarnings("unused")
   private boolean canObtainLocation() {
      final boolean hasFeature = getPackageManager().hasSystemFeature("android.hardware.location.network");
      if (!hasFeature) {
         return false;
      }
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
         startActivityForResult(intent, REQUEST_SETTING_CHANGED);
         return true;
      } else if (itemId == R.id.miBackup) {
         Utils.pinProtectedWordlistBackup(this);
         return true;
      //with wordlists, we just need to backup and verify in one step
      //} else if (itemId == R.id.miVerifyBackup) {
      //   VerifyBackupActivity.callMe(this);
      //   return true;
      } else if (itemId == R.id.miRefresh) {
         _mbwManager.getWalletManager(false).startSynchronization();
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

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_SETTING_CHANGED) {
         // restart activity if language changes
         // or anything else in settings. this makes some of the listeners
         // obsolete
         Intent running = getIntent();
         finish();
         startActivity(running);
      } else if (requestCode == GENERIC_SCAN_REQUEST) {
         if (resultCode == RESULT_OK) {
            ScanActivity.ResultType type = (ScanActivity.ResultType) data.getSerializableExtra(ScanActivity.RESULT_TYPE_KEY);
            if (type == ScanActivity.ResultType.ACCOUNT) {
               UUID accountid = ScanActivity.getAccount(data);
               WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountid);
               //we are returning from seed import, so this has to be the first hd account
               Preconditions.checkState(account instanceof Bip44Account);
               String defaultName = getString(R.string.account) + " " + (((Bip44Account) account).getAccountIndex() + 1);
               //store the default name for the account
               _mbwManager.getMetadataStorage().storeAccountLabel(accountid, defaultName);
               getSupportActionBar().selectTab(mAccountsTab);
            }
         } else {
            //report to user in case of error
            //if no scan handlers match successfully, this is the last resort to display an error msg
            ScanActivity.toastScanError(resultCode, data, this);
         }
      } else {
         super.onActivityResult(requestCode, resultCode, data);
      }
   }

   private void openMyceliumHelp() {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(Constants.MYCELIUM_WALLET_HELP_URL));
      startActivity(intent);
      Toast.makeText(this, R.string.going_to_mycelium_com_help, Toast.LENGTH_LONG).show();
   }

   public void setRefreshAnimation() {
      if (refreshItem != null) {
         if (_mbwManager.getWalletManager(false).getState() == WalletManager.State.SYNCHRONIZING) {
            MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
         } else {
            MenuItemCompat.setActionView(refreshItem, null);
         }
      }
   }

   @Subscribe
   public void syncStarted(SyncStarted event) {
      setRefreshAnimation();
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      setRefreshAnimation();
   }

   @Subscribe
   public void synchronizationFailed(SyncFailed event) {
      _toaster.toastConnectionError();
   }

   @Subscribe
   public void transactionBroadcasted(TransactionBroadcasted event) {
      _toaster.toast(R.string.transaction_sent, false);
   }

   @Subscribe
   public void onNewVersion(final WalletVersionEvent event) {
      if (!event.response.isPresent()) {
         return;
      }
      _mbwManager.getVersionManager().showIfRelevant(event.response.get(), this);
   }

}

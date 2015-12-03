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

package com.mycelium.wallet.activity.modern;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.AboutActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.main.BalanceMasterFragment;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.event.*;
import com.mycelium.wallet.external.cashila.activity.CashilaPaymentsActivity;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Subscribe;
import de.cketti.library.changelog.ChangeLog;
import info.guardianproject.onionkit.ui.OrbotHelper;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ModernMain extends ActionBarActivity {

   public static final int GENERIC_SCAN_REQUEST = 4;
   private static final int REQUEST_SETTING_CHANGED = 5;
   public static final int MIN_AUTOSYNC_INTERVAL = 1 * 60 * 1000;
   public static final String LAST_SYNC = "LAST_SYNC";
   private MbwManager _mbwManager;

   ViewPager mViewPager;
   TabsAdapter mTabsAdapter;
   ActionBar.Tab mBalanceTab;
   ActionBar.Tab mAccountsTab;
   private MenuItem refreshItem;
   private Toaster _toaster;
   private long _lastSync = 0;

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

      // Load the theme-background (usually happens in styles.xml) but use a lower
      // pixel format, this saves around 10MB of allocated memory
      // persist the loaded Bitmap in the context of mbw-manager and reuse it every time this activity gets created
      try {
         BitmapDrawable background = (BitmapDrawable) _mbwManager.getBackgroundObjectsCache().get("mainBackground", new Callable<BitmapDrawable>() {
            @Override
            public BitmapDrawable call() throws Exception {
               BitmapFactory.Options options = new BitmapFactory.Options();
               options.inPreferredConfig = Bitmap.Config.RGB_565;
               Bitmap background = BitmapFactory.decodeResource(getResources(), R.drawable.background_witherrors_dimmed, options);
               BitmapDrawable drawable = new BitmapDrawable(getResources(), background);
               drawable.setGravity(Gravity.CENTER);
               return drawable;
            }
         });
         getWindow().setBackgroundDrawable(background);
      } catch (ExecutionException ignore) {
      }


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

      ChangeLog cl = new DarkThemeChangeLog(this);
      if (cl.isFirstRun() && cl.getChangeLog(false).size() > 0) {
         cl.getLogDialog().show();
      }

      checkTorState();

      _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.APP_START);

      if (savedInstanceState != null) {
         _lastSync = savedInstanceState.getLong(LAST_SYNC, 0);
      }

   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putLong(LAST_SYNC, _lastSync);
   }

   private void checkTorState() {
      if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR) {
         OrbotHelper obh = new OrbotHelper(this);
         if (!obh.isOrbotRunning()) {
            obh.requestOrbotStart(this);
         }
      }
   }


   @Override
   protected void onResume() {
      _mbwManager.getEventBus().register(this);

      // Start WAPI as a delayed action. This way we don't immediately block the account
      // while synchronizing
      Handler h = new Handler();
      if (_lastSync == 0 || new Date().getTime() - _lastSync > MIN_AUTOSYNC_INTERVAL) {
         h.postDelayed(new Runnable() {
            @Override
            public void run() {
               _mbwManager.getVersionManager().checkForUpdate();
               _mbwManager.getWalletManager(false).startSynchronization();
               _mbwManager.getExchangeRateManager().requestRefresh();
            }
         }, 70);
         _lastSync = new Date().getTime();
      }


      supportInvalidateOptionsMenu();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _mbwManager.getEventBus().unregister(this);
      _mbwManager.getVersionManager().closeDialog();
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
      inflater.inflate(R.menu.transaction_history_options_global, menu);
      inflater.inflate(R.menu.main_activity_options_menu, menu);
      addEnglishSetting(menu.findItem(R.id.miSettings));
      inflater.inflate(R.menu.refresh, menu);
      inflater.inflate(R.menu.export_history, menu);
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

      // at the moment, we allow to make backups multiple times
      Preconditions.checkNotNull(menu.findItem(R.id.miBackup)).setVisible(true);

      // Add Record menu
      final boolean isRecords = tabIdx == 0;
      final boolean locked = _mbwManager.isKeyManagementLocked();
      Preconditions.checkNotNull(menu.findItem(R.id.miAddRecord)).setVisible(isRecords && !locked);
      Preconditions.checkNotNull(menu.findItem(R.id.miAddFiatAccount)).setVisible(isRecords);

      // Lock menu
      final boolean hasPin = _mbwManager.isPinProtected();
      Preconditions.checkNotNull(menu.findItem(R.id.miLockKeys)).setVisible(isRecords && !locked && hasPin);

      // Refresh menu
      final boolean isBalance = tabIdx == 1;
      final boolean isHistory = tabIdx == 2;
      refreshItem = Preconditions.checkNotNull(menu.findItem(R.id.miRefresh));
      refreshItem.setVisible(isBalance || isHistory);
      setRefreshAnimation();

      //export tx history
      Preconditions.checkNotNull(menu.findItem(R.id.miExportHistory)).setVisible(isHistory);

      final boolean showSepaEntry = isBalance && _mbwManager.isWalletPaired(ExternalService.CASHILA);
      Preconditions.checkNotNull(menu.findItem(R.id.miSepaSend).setVisible(showSepaEntry));

      Preconditions.checkNotNull(menu.findItem(R.id.miRescanTransactions)).setVisible(isHistory);

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
         //switch server every third time the refresh button gets hit
         if (new Random().nextInt(3) == 0) {
            _mbwManager.switchServer();
         }
         _mbwManager.getWalletManager(false).startSynchronization();
      } else if (itemId == R.id.miExplore) {
         _mbwManager.getExploreHelper().redirectToCoinmap(this);
      } else if (itemId == R.id.miHelp) {
         openMyceliumHelp();
      } else if (itemId == R.id.miAbout) {
         Intent intent = new Intent(this, AboutActivity.class);
         startActivity(intent);
      } else if (itemId == R.id.miRescanTransactions) {
         _mbwManager.getSelectedAccount().dropCachedData();
         _mbwManager.getWalletManager(false).startSynchronization();
      } else if (itemId == R.id.miSepaSend) {
         _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.CASHILA, true, new Runnable() {
            @Override
            public void run() {
               startActivity(CashilaPaymentsActivity.getIntent(ModernMain.this));
            }
         });
      } else if (itemId == R.id.miExportHistory) {
         shareTransactionHistory();
      }
      return super.onOptionsItemSelected(item);
   }

   private void shareTransactionHistory() {
      String historyData = DataExport.getTxHistoryCsv(_mbwManager.getSelectedAccount(), _mbwManager.getMetadataStorage());
      Intent s = new Intent(android.content.Intent.ACTION_SEND);
      s.setType("text/plain");
      s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.transaction_history_title));
      s.putExtra(Intent.EXTRA_TEXT, historyData);
      startActivity(Intent.createChooser(s, getResources().getString(R.string.share_transaction_history)));
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
         if (resultCode != RESULT_OK) {
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
            MenuItem menuItem = MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
            ImageView ivTorIcon = (ImageView) menuItem.getActionView().findViewById(R.id.ivTorIcon);

            if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR && _mbwManager.getTorManager() != null) {
               ivTorIcon.setVisibility(View.VISIBLE);
               if (_mbwManager.getTorManager().getInitState() == 100) {
                  ivTorIcon.setImageResource(R.drawable.tor);
               } else {
                  ivTorIcon.setImageResource(R.drawable.tor_gray);
               }
            } else {
               ivTorIcon.setVisibility(View.GONE);
            }

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
   public void torState(TorStateChanged event) {
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
   public void onNewFeatureWarnings(final FeatureWarningsAvailable event) {
      _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.MAIN_SCREEN);

      if (_mbwManager.getSelectedAccount() instanceof CoinapultAccount) {
         _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.COINAPULT);
      }
   }

   @Subscribe
   public void onNewVersion(final NewWalletVersionAvailable event) {
      _mbwManager.getVersionManager().showIfRelevant(event.versionInfo, this);
   }

}

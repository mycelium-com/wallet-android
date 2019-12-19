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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.AboutActivity;
import com.mycelium.wallet.activity.MessageVerifyActivity;
import com.mycelium.wallet.activity.main.BalanceMasterFragment;
import com.mycelium.wallet.activity.main.RecommendationsFragment;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment;
import com.mycelium.wallet.activity.modern.adapter.TabsAdapter;
import com.mycelium.wallet.activity.news.NewsActivity;
import com.mycelium.wallet.activity.send.InstantWalletActivity;
import com.mycelium.wallet.activity.settings.SettingsActivity;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.event.FeatureWarningsAvailable;
import com.mycelium.wallet.event.MalformedOutgoingTransactionsFound;
import com.mycelium.wallet.event.NewWalletVersionAvailable;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.event.TorStateChanged;
import com.mycelium.wallet.event.TransactionBroadcasted;
import com.mycelium.wallet.external.mediaflow.NewsConstants;
import com.mycelium.wallet.modularisation.ModularisationVersionHelper;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.manager.State;
import com.squareup.otto.Subscribe;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.cketti.library.changelog.ChangeLog;
import info.guardianproject.onionkit.ui.OrbotHelper;

import static com.google.common.base.Preconditions.checkNotNull;

public class ModernMain extends AppCompatActivity {
    private static final int TAB_ID_NEWS = 0;
    private static final int TAB_ID_ACCOUNTS = 1;
    private static final int TAB_ID_BALANCE = 2;
    private static final int TAB_ID_HISTORY = 3;

    private static final int REQUEST_SETTING_CHANGED = 5;
    public static final int MIN_AUTOSYNC_INTERVAL = (int) Constants.MS_PR_MINUTE;
    public static final int MIN_FULLSYNC_INTERVAL = (int) (5 * Constants.MS_PR_HOUR);
    public static final String LAST_SYNC = "LAST_SYNC";
    private static final String APP_START = "APP_START";
    private MbwManager _mbwManager;

    private int addressBookTabIndex;

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    TabLayout.Tab mBalanceTab;
    TabLayout.Tab mNewsTab;
    TabLayout.Tab mAccountsTab;
    TabLayout.Tab mRecommendationsTab;
    private MenuItem refreshItem;
    private Toaster _toaster;
    private volatile long _lastSync = 0;
    private boolean _isAppStart = true;
    private int counter = 0;

    private Timer balanceRefreshTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _mbwManager = MbwManager.getInstance(this);
        WalletApplication.applyLanguageChange(getBaseContext(), _mbwManager.getLanguage());
        setContentView(R.layout.modern_main);
        TabLayout tabLayout = findViewById(R.id.pager_tabs);
        mViewPager = findViewById(R.id.pager);
        tabLayout.setupWithViewPager(mViewPager);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(false);
        bar.setDisplayShowHomeEnabled(true);
        bar.setIcon(R.drawable.action_bar_logo);

        getWindow().setBackgroundDrawableResource(R.drawable.background_main);

        mViewPager.setOffscreenPageLimit(5);
        mTabsAdapter = new TabsAdapter(this, mViewPager, _mbwManager);
        if (SettingsPreference.getMediaFlowEnabled()) {
            mNewsTab = tabLayout.newTab().setText(getString(R.string.media_flow));
            mTabsAdapter.addTab(mNewsTab, NewsFragment.class, null);
        }
        mAccountsTab = tabLayout.newTab().setText(getString(R.string.tab_accounts));
        mTabsAdapter.addTab(mAccountsTab, AccountsFragment.class, null);
        mBalanceTab = tabLayout.newTab().setText(getString(R.string.tab_balance));
        mTabsAdapter.addTab(mBalanceTab, BalanceMasterFragment.class, null);
        mTabsAdapter.addTab(tabLayout.newTab().setText(getString(R.string.tab_transactions)), TransactionHistoryFragment.class, null);
        mRecommendationsTab = tabLayout.newTab().setText(getString(R.string.tab_partners));
        mTabsAdapter.addTab(mRecommendationsTab,
                RecommendationsFragment.class, null);
        final Bundle addressBookConfig = new Bundle();
        addressBookConfig.putBoolean(AddressBookFragment.OWN, false);
        addressBookConfig.putBoolean(AddressBookFragment.SELECT_ONLY, false);
        addressBookConfig.putBoolean(AddressBookFragment.AVAILABLE_FOR_SENDING, false);
        mTabsAdapter.addTab(tabLayout.newTab().setText(getString(R.string.tab_addresses)), AddressBookFragment.class,
                addressBookConfig);
        addressBookTabIndex = mTabsAdapter.getCount() - 1; // save address book tab id to show/hide add contact
        if (SettingsPreference.getMediaFlowEnabled() &&
                Objects.equals(getIntent().getAction(), "media_flow")) {
            mNewsTab.select();
            mViewPager.setCurrentItem(TAB_ID_NEWS);
            if (getIntent().hasExtra(NewsConstants.NEWS)) {
                Intent intent = new Intent(this, NewsActivity.class);
                intent.putExtras(getIntent().getExtras());
                startActivity(intent);
            }
        } else {
            mViewPager.setCurrentItem(TAB_ID_BALANCE);
        }
        _toaster = new Toaster(this);

        ChangeLog cl = new DarkThemeChangeLog(this);
        if (cl.isFirstRun() && cl.getChangeLog(false).size() > 0 && !cl.isFirstRunEver()) {
            cl.getLogDialog().show();
        }

        checkTorState();

        if (savedInstanceState != null) {
            _lastSync = savedInstanceState.getLong(LAST_SYNC, 0);
            _isAppStart = savedInstanceState.getBoolean(APP_START, true);
        }

        if (_isAppStart) {
            _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.APP_START);
            checkGapBug();
            _isAppStart = false;
        }

        ModularisationVersionHelper.notifyWrongModuleVersion(this);
    }

    private void checkGapBug() {
        final BitcoinHDModule module = (BitcoinHDModule) _mbwManager.getWalletManager(false).getModuleById(BitcoinHDModule.ID);
        final Set<Integer> gaps = module != null ? module.getGapsBug() : null;
        if (gaps != null && !gaps.isEmpty()) {
            checkNotNull(module);
            final List<Address> gapAddresses = module.getGapAddresses(AesKeyCipher.defaultKeyCipher());
            final String gapsString = Joiner.on(", ").join(gapAddresses);
            Log.d("Gaps", gapsString);

            final SpannableString s = new SpannableString(getResources().getString(R.string.check_gap_bug_spannable_string));
            Linkify.addLinks(s, Linkify.ALL);

            final AlertDialog d = new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.account_gap)).setMessage(s)
                    .setPositiveButton(getResources().getString(R.string.gaps_button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            createPlaceHolderAccounts(gaps);
                            _mbwManager.reportIgnoredException(new RuntimeException(getResources().getString(R.string.address_gaps) + gapsString));
                        }
                    }).setNegativeButton(getResources().getString(R.string.gaps_button_ignore), null).show();

            // Make the textview clickable. Must be called after show()
            ((TextView) Objects.requireNonNull(d.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void createPlaceHolderAccounts(Set<Integer> gapIndex) {
        final BitcoinHDModule module = (BitcoinHDModule) _mbwManager.getWalletManager(false).getModuleById(BitcoinHDModule.ID);
        for (Integer index : gapIndex) {
            final UUID newAccount = module.createArchivedGapFiller(AesKeyCipher.defaultKeyCipher(), index);
            _mbwManager.getMetadataStorage().storeAccountLabel(newAccount, "Gap Account " + (index + 1));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(LAST_SYNC, _lastSync);
        outState.putBoolean(APP_START, _isAppStart);
    }

    private void checkTorState() {
        if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR) {
            OrbotHelper obh = new OrbotHelper(this);
            // only check for Orbot if the OS is older than AndroidN (SDK_INT==24),
            // because the current check does not work any more
            // see: https://github.com/mycelium-com/wallet/issues/288#issuecomment-257261708
            if (!obh.isOrbotRunning(this) && android.os.Build.VERSION.SDK_INT < 24) {
                obh.requestOrbotStart(this);
            }
        }
    }

    protected void stopBalanceRefreshTimer() {
        if (balanceRefreshTimer != null) {
            balanceRefreshTimer.cancel();
        }
    }

    @Subscribe
    public void malformedOutgoingTransactionFound(MalformedOutgoingTransactionsFound event) {
        final MalformedOutgoingTransactionsFound ev = event;
        if (_mbwManager.isShowQueuedTransactionsRemovalAlert()) {
            // Whatever option the user choose, the confirmation dialog will not be shown
            // until the next application start
            _mbwManager.setShowQueuedTransactionsRemovalAlert(false);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.failed_transaction_removal_title)
                    .setMessage(R.string.failed_transaction_removal_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            WalletAccount account = _mbwManager.getWalletManager(false).getAccount(ev.getAccount());
                            account.removeAllQueuedTransactions();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        MbwManager.getEventBus().register(this);

        long curTime = new Date().getTime();
        if (_lastSync == 0 || curTime - _lastSync > MIN_AUTOSYNC_INTERVAL) {
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _mbwManager.getVersionManager().checkForUpdate();
                }
            }, 50);
        }

        stopBalanceRefreshTimer();
        balanceRefreshTimer = new Timer();
        balanceRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (Utils.isConnected(getApplicationContext())) {
                    _mbwManager.getExchangeRateManager().requestRefresh();

                    // if the last full sync is too old (or not known), start a full sync for _all_
                    // accounts
                    // otherwise just run a normal sync for the current account
                    final Optional<Long> lastFullSync = _mbwManager.getMetadataStorage().getLastFullSync();
                    if (lastFullSync.isPresent()
                            && (new Date().getTime() - lastFullSync.get() < MIN_FULLSYNC_INTERVAL)) {
                        _mbwManager.getWalletManager(false).startSynchronization();
                    } else {
                        _mbwManager.getWalletManager(false).startSynchronization(SyncMode.FULL_SYNC_ALL_ACCOUNTS);
                        _mbwManager.getMetadataStorage().setLastFullSync(new Date().getTime());
                    }

                    _lastSync = new Date().getTime();
                }
            }
        }, 100, MIN_AUTOSYNC_INTERVAL);

        supportInvalidateOptionsMenu();
        super.onStart();
    }

    @Override
    protected void onStop() {
        stopBalanceRefreshTimer();
        MbwManager.getEventBus().unregister(this);
        _mbwManager.getVersionManager().closeDialog();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (mBalanceTab.isSelected()) {
            // this is not finishing on Android 6 LG G4, so the pin on startup is not
            // requested.
            // commented out code above doesn't do the trick, neither.
            _mbwManager.setStartUpPinUnlocked(false);
            super.onBackPressed();
        } else {
            mBalanceTab.select();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.transaction_history_options_global, menu);
        inflater.inflate(R.menu.main_activity_options_menu, menu);
        addEnglishSetting(menu.findItem(R.id.miSettings));
        inflater.inflate(R.menu.refresh, menu);
        inflater.inflate(R.menu.record_options_menu_global, menu);
        inflater.inflate(R.menu.addressbook_options_global, menu);
        inflater.inflate(R.menu.verify_message, menu);
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
        checkNotNull(menu.findItem(R.id.miBackup)).setVisible(true);

        // Add Record menu
        final boolean isAccountTab = tabIdx == TAB_ID_ACCOUNTS;
        final boolean locked = _mbwManager.isKeyManagementLocked();
        checkNotNull(menu.findItem(R.id.miAddRecord)).setVisible(isAccountTab && !locked);

        // Lock menu
        final boolean hasPin = _mbwManager.isPinProtected();
        checkNotNull(menu.findItem(R.id.miLockKeys)).setVisible(isAccountTab && !locked && hasPin);

        // Refresh menu
        final boolean isBalanceTab = tabIdx == TAB_ID_BALANCE;
        final boolean isHistoryTab = tabIdx == TAB_ID_HISTORY;
        refreshItem = checkNotNull(menu.findItem(R.id.miRefresh));
        refreshItem.setVisible(isBalanceTab || isHistoryTab || isAccountTab);
        setRefreshAnimation();

        checkNotNull(menu.findItem(R.id.miRescanTransactions)).setVisible(isHistoryTab);

        final boolean isAddressBook = tabIdx == addressBookTabIndex;
        checkNotNull(menu.findItem(R.id.miAddAddress)).setVisible(isAddressBook);

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
        switch (itemId) {
            case R.id.miColdStorage:
                InstantWalletActivity.callMe(this);
                return true;
            case R.id.miSettings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, REQUEST_SETTING_CHANGED);
                return true;
            case R.id.miBackup:
                Utils.pinProtectedWordlistBackup(this);
                return true;
            //with wordlists, we just need to backup and verify in one step
            //} else if (itemId == R.id.miVerifyBackup) {
            //   VerifyBackupActivity.callMe(this);
            //   return true;
            case R.id.miRefresh:
                // default only sync the current account
                SyncMode syncMode = SyncMode.NORMAL_FORCED;
                // every 5th manual refresh make a full scan
                if (counter == 4) {
                    syncMode = SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED;
                    counter = 0;
                } else if (mViewPager.getCurrentItem() == TAB_ID_ACCOUNTS) {
                    // if we are in the accounts tab, sync all accounts if the users forces a sync
                    syncMode = SyncMode.NORMAL_ALL_ACCOUNTS_FORCED;
                    counter++;
                }

                if (!startSynchronization(syncMode)) {
                    mViewPager.postDelayed(this::setRefreshAnimation, TimeUnit.SECONDS.toMillis(1));
                }

                // also fetch a new exchange rate, if necessary
                _mbwManager.getExchangeRateManager().requestOptionalRefresh();

                showRefresh(); // without this call sometime user not see click feedback
                return true;
            case R.id.miHelp:
                openMyceliumHelp();
                break;
            case R.id.miAbout:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            case R.id.miRescanTransactions:
                _mbwManager.getSelectedAccount().dropCachedData();
                startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
                break;
            case R.id.miVerifyMessage:
                startActivity(new Intent(this, MessageVerifyActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean startSynchronization(SyncMode syncMode) {
        boolean started = _mbwManager.getWalletManager(false).startSynchronization(syncMode);
        if (!started) {
            MbwManager.getEventBus().post(new SyncFailed());
        }
        return started;
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
            if (_mbwManager.getWalletManager(false).getState() == State.SYNCHRONIZING) {
                showRefresh();
            } else {
                hideRefresh();
            }
        }
    }

    private void hideRefresh() {
        refreshItem.setActionView(null);
    }

    private void showRefresh() {
        MenuItem menuItem = refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        ImageView ivTorIcon = menuItem.getActionView().findViewById(R.id.ivTorIcon);

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
        hideRefresh();
        _toaster.toastConnectionError();
    }

    @Subscribe
    public void transactionBroadcasted(TransactionBroadcasted event) {
        _toaster.toast(R.string.transaction_sent, false);
    }

    @Subscribe
    public void onNewFeatureWarnings(final FeatureWarningsAvailable event) {
        _mbwManager.getVersionManager().showFeatureWarningIfNeeded(this, Feature.MAIN_SCREEN);
    }

    @Subscribe
    public void onNewVersion(final NewWalletVersionAvailable event) {
        _mbwManager.getVersionManager().showIfRelevant(event.versionInfo, this);
    }
}

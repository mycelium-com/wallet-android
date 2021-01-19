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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.bequant.intro.BequantIntroActivity;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.MessageVerifyActivity;
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity;
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity;
import com.mycelium.wallet.activity.main.BalanceMasterFragment;
import com.mycelium.wallet.activity.main.FioRequestsHistoryFragment;
import com.mycelium.wallet.activity.main.RecommendationsFragment;
import com.mycelium.wallet.activity.main.TransactionHistoryFragment;
import com.mycelium.wallet.activity.modern.adapter.TabsAdapter;
import com.mycelium.wallet.activity.news.NewsActivity;
import com.mycelium.wallet.activity.news.NewsUtils;
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
import com.mycelium.wallet.external.partner.model.MainMenuContent;
import com.mycelium.wallet.external.partner.model.MainMenuPage;
import com.mycelium.wallet.fio.FioRequestNotificator;
import com.mycelium.wallet.modularisation.ModularisationVersionHelper;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.fio.FioModule;
import com.mycelium.wapi.wallet.manager.State;
import com.squareup.otto.Subscribe;

import java.util.Collections;
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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.common.base.Preconditions.checkNotNull;

public class ModernMain extends AppCompatActivity {
    private static final String TAB_NEWS = "tab_news";
    private static final String TAB_ACCOUNTS = "tab_accounts";
    private static final String TAB_BALANCE = "tab_balance";
    private static final String TAB_HISTORY = "tab_history";
    private static final String TAB_FIO_REQUESTS = "tab_fio_requests";
    private static final String TAB_ADS = "tab_ads";
    private static final String TAB_RECOMMENDATIONS = "tab_recommendations";
    private static final String TAB_ADDRESS_BOOK = "tab_address_book";

    private static final int REQUEST_SETTING_CHANGED = 5;
    public static final int MIN_AUTOSYNC_INTERVAL = (int) Constants.MS_PR_MINUTE;
    public static final int MIN_FULLSYNC_INTERVAL = (int) (5 * Constants.MS_PR_HOUR);
    public static final String LAST_SYNC = "LAST_SYNC";
    private static final String APP_START = "APP_START";
    private MbwManager _mbwManager;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private TabLayout.Tab mBalanceTab;
    private TabLayout.Tab mNewsTab;
    private TabLayout.Tab mAccountsTab;
    private TabLayout.Tab mRecommendationsTab;
    private TabLayout.Tab mFioRequestsTab;
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
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        findViewById(R.id.logoButton).setOnClickListener(new LogoMenuClick());
        findViewById(R.id.logoMenu).setOnClickListener(new LogoMenuClick());
        View investmentWallet = findViewById(R.id.investmentWallet);
        investmentWallet.setVisibility(SettingsPreference.isContentEnabled(com.mycelium.bequant.BequantConstants.PARTNER_ID) ?
                VISIBLE : GONE);
        investmentWallet.setOnClickListener(view -> {
            findViewById(R.id.logoMenu).performClick(); // to hide menu
            startActivity(new Intent(view.getContext(), BequantIntroActivity.class));
        });

        getWindow().setBackgroundDrawableResource(R.drawable.background_main);

        mViewPager.setOffscreenPageLimit(5);
        mTabsAdapter = new TabsAdapter(this, mViewPager, _mbwManager);
        if (SettingsPreference.getMediaFlowEnabled()) {
            mNewsTab = tabLayout.newTab().setText(getString(R.string.media_flow));
            mTabsAdapter.addTab(mNewsTab, NewsFragment.class, null, TAB_NEWS);
        }
        mAccountsTab = tabLayout.newTab().setText(getString(R.string.tab_accounts));
        mTabsAdapter.addTab(mAccountsTab, AccountsFragment.class, null, TAB_ACCOUNTS);
        mBalanceTab = tabLayout.newTab().setText(getString(R.string.tab_balance));
        mTabsAdapter.addTab(mBalanceTab, BalanceMasterFragment.class, null, TAB_BALANCE);
        mTabsAdapter.addTab(tabLayout.newTab().setText(getString(R.string.tab_transactions)), TransactionHistoryFragment.class, null, TAB_HISTORY);
        mRecommendationsTab = tabLayout.newTab().setText(getString(R.string.tab_partners));
        mTabsAdapter.addTab(mRecommendationsTab,
                RecommendationsFragment.class, null, TAB_RECOMMENDATIONS);
        mFioRequestsTab = tabLayout.newTab().setText(getString(R.string.tab_fio_requests));
        mTabsAdapter.addTab(mFioRequestsTab, FioRequestsHistoryFragment.class, null, TAB_FIO_REQUESTS);
        final Bundle addressBookConfig = new Bundle();
        addressBookConfig.putBoolean(AddressBookFragment.OWN, false);
        addressBookConfig.putBoolean(AddressBookFragment.SELECT_ONLY, false);
        addressBookConfig.putBoolean(AddressBookFragment.AVAILABLE_FOR_SENDING, false);
        mTabsAdapter.addTab(tabLayout.newTab().setText(getString(R.string.tab_addresses)), AddressBookFragment.class,
                addressBookConfig, TAB_ADDRESS_BOOK);
        addAdsTabs(tabLayout);
        mBalanceTab.select();
        mViewPager.setCurrentItem(mTabsAdapter.indexOf(TAB_BALANCE));
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
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (SettingsPreference.getMediaFlowEnabled() &&
                Objects.equals(getIntent().getAction(), NewsUtils.MEDIA_FLOW_ACTION)) {
            mNewsTab.select();
            mViewPager.setCurrentItem(mTabsAdapter.indexOf(TAB_NEWS));
            if (getIntent().hasExtra(NewsConstants.NEWS)) {
                startActivity(new Intent(this, NewsActivity.class)
                        .putExtras(getIntent().getExtras()));
            }
        } else if (Objects.equals(intent.getAction(), FioRequestNotificator.FIO_REQUEST_ACTION)) {
            mFioRequestsTab.select();
            mViewPager.setCurrentItem(mTabsAdapter.indexOf(TAB_FIO_REQUESTS));
            startActivity(new Intent(this, ApproveFioRequestActivity.class)
                    .putExtras(getIntent().getExtras()));
        }
    }

    private void addAdsTabs(TabLayout tabLayout) {
        MainMenuContent content = SettingsPreference.getMainMenuContent();
        if (content != null) {
            Collections.sort(content.getPages(), (a1, a2) -> a1.getTabIndex() - a2.getTabIndex());
            for (MainMenuPage page : content.getPages()) {
                if (page.isActive() && SettingsPreference.isContentEnabled(page.getParentId())) {
                    Bundle adsBundle = new Bundle();
                    adsBundle.putSerializable("page", page);
                    int tabIndex = page.getTabIndex();
                    TabLayout.Tab newTab = tabLayout.newTab().setText(page.getTabName());
                    if (0 <= tabIndex && tabIndex < mTabsAdapter.getCount()) {
                        mTabsAdapter.addTab(tabIndex, newTab,
                                AdsFragment.class, adsBundle, TAB_ADS + tabIndex);
                    } else {
                        mTabsAdapter.addTab(newTab,
                                AdsFragment.class, adsBundle, TAB_ADS + tabIndex);
                    }
                }
            }
        }
    }

    private void checkGapBug() {
        final BitcoinHDModule module = (BitcoinHDModule) _mbwManager.getWalletManager(false).getModuleById(BitcoinHDModule.ID);
        final Set<Integer> gaps = module != null ? module.getGapsBug() : null;
        if (gaps != null && !gaps.isEmpty()) {
            checkNotNull(module);
            final List<BitcoinAddress> gapAddresses = module.getGapAddresses(AesKeyCipher.defaultKeyCipher());
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
                    // otherwise just run a normal sync for all accounts
                    final Optional<Long> lastFullSync = _mbwManager.getMetadataStorage().getLastFullSync();
                    if (lastFullSync.isPresent()
                            && (new Date().getTime() - lastFullSync.get() < MIN_FULLSYNC_INTERVAL)) {
                        WalletAccount<?> account = _mbwManager.getSelectedAccount();
                        _mbwManager.getWalletManager(false)
                                .startSynchronization(SyncMode.NORMAL_ALL_ACCOUNTS_FORCED);
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
        inflater.inflate(R.menu.record_options_menu_global, menu);
        inflater.inflate(R.menu.transaction_history_options_global, menu);
        inflater.inflate(R.menu.main_activity_options_menu, menu);
        addEnglishSetting(menu.findItem(R.id.miSettings));
        inflater.inflate(R.menu.refresh, menu);
        inflater.inflate(R.menu.addressbook_options_global, menu);
        inflater.inflate(R.menu.verify_message, menu);
        if (!((FioModule) _mbwManager.getWalletManager(false).getModuleById(FioModule.ID)).getAllRegisteredFioNames().isEmpty()) {
            inflater.inflate(R.menu.record_fio_options, menu);
        }
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
        String tabTag = mTabsAdapter.getPageTag(mViewPager.getCurrentItem());
        // at the moment, we allow to make backups multiple times
        checkNotNull(menu.findItem(R.id.miBackup)).setVisible(true);

        // Add Record menu
        final boolean isAccountTab = TAB_ACCOUNTS.equals(tabTag);
        final boolean locked = _mbwManager.isKeyManagementLocked();
        checkNotNull(menu.findItem(R.id.miAddRecord)).setVisible(isAccountTab && !locked);

        // Lock menu
        final boolean hasPin = _mbwManager.isPinProtected();
        checkNotNull(menu.findItem(R.id.miLockKeys)).setVisible(isAccountTab && !locked && hasPin);

        // Refresh menu
        final boolean isBalanceTab = TAB_BALANCE.equals(tabTag);
        final boolean isHistoryTab = TAB_HISTORY.equals(tabTag);
        final boolean isRequestsTab = TAB_FIO_REQUESTS.equals(tabTag);
        refreshItem = checkNotNull(menu.findItem(R.id.miRefresh));
        refreshItem.setVisible(isBalanceTab || isHistoryTab || isRequestsTab || isAccountTab);
        setRefreshAnimation();

        checkNotNull(menu.findItem(R.id.miRescanTransactions)).setVisible(isHistoryTab);

        final boolean isAddressBook = TAB_ADDRESS_BOOK.equals(tabTag);
        checkNotNull(menu.findItem(R.id.miAddAddress)).setVisible(isAddressBook);

        return super.onPrepareOptionsMenu(menu);
    }

    public void selectRequestTab(){
        int item = mTabsAdapter.indexOf(TAB_FIO_REQUESTS);
        mViewPager.setCurrentItem(item);
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
                } else if (TAB_ACCOUNTS.equals(mTabsAdapter.getPageTag(mViewPager.getCurrentItem()))) {
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
            case R.id.miRescanTransactions:
                _mbwManager.getSelectedAccount().dropCachedData();
                startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
                break;
            case R.id.miVerifyMessage:
                startActivity(new Intent(this, MessageVerifyActivity.class));
                break;
            case R.id.miMyFIONames:
                startActivity(new Intent(this, AccountMappingActivity.class));
                break;
            case R.id.miFIORequests:
                selectRequestTab();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean startSynchronization(SyncMode syncMode) {
        WalletAccount<?> account = _mbwManager.getSelectedAccount();
        boolean started = _mbwManager.getWalletManager(false)
                .startSynchronization(syncMode, Collections.singletonList(account));
        if (!started) {
            MbwManager.getEventBus().post(new SyncFailed(account.getId()));
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
        if (refreshItem != null) {
            refreshItem.setActionView(null);
        }
    }

    private void showRefresh() {
        MenuItem menuItem = refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        ImageView ivTorIcon = menuItem.getActionView().findViewById(R.id.ivTorIcon);

        if (_mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR && _mbwManager.getTorManager() != null) {
            ivTorIcon.setVisibility(VISIBLE);
            if (_mbwManager.getTorManager().getInitState() == 100) {
                ivTorIcon.setImageResource(R.drawable.tor);
            } else {
                ivTorIcon.setImageResource(R.drawable.tor_gray);
            }
        } else {
            ivTorIcon.setVisibility(GONE);
        }
    }

    private static class LogoMenuClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Activity host = (Activity) view.getContext();
            View logoMenu = host.findViewById(R.id.logoMenu);
            boolean isOpened = logoMenu.getVisibility() == VISIBLE;
            logoMenu.setVisibility(isOpened ? GONE : VISIBLE);
            ImageView logoArrow = host.findViewById(R.id.logoArrow);
            logoArrow.setImageDrawable(logoArrow.getResources().getDrawable(isOpened ?
                    R.drawable.ic_arrow_drop_down : R.drawable.ic_arrow_drop_down_active));
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
        String type = "";
        WalletAccount account = _mbwManager.getWalletManager(false).getAccount(event.accountId);
        if(account != null) {
            type = account.getCoinType().getName();
        }
        _toaster.toastConnectionError(type);
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

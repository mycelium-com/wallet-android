package com.mycelium.wallet.activity.modern

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.common.base.Preconditions
import com.mycelium.giftbox.GiftBoxRootActivity
import com.mycelium.giftbox.client.GiftboxConstants
import com.mycelium.net.ServerEndpointType
import com.mycelium.wallet.*
import com.mycelium.wallet.activity.ActionActivity
import com.mycelium.wallet.activity.MessageVerifyActivity
import com.mycelium.wallet.activity.changelog.ChangeLog
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity
import com.mycelium.wallet.activity.main.BalanceMasterFragment
import com.mycelium.wallet.activity.main.FioRequestsHistoryFragment
import com.mycelium.wallet.activity.main.RecommendationsFragment
import com.mycelium.wallet.activity.main.TransactionHistoryFragment
import com.mycelium.wallet.activity.modern.adapter.TabsAdapter
import com.mycelium.wallet.activity.modern.event.BackHandler
import com.mycelium.wallet.activity.modern.event.BackListener
import com.mycelium.wallet.activity.modern.event.SelectTab
import com.mycelium.wallet.activity.modern.helper.MainActions
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.send.InstantWalletActivity
import com.mycelium.wallet.activity.settings.SettingsActivity
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.activity.settings.SettingsPreference.getMainMenuContent
import com.mycelium.wallet.activity.settings.SettingsPreference.isContentEnabled
import com.mycelium.wallet.activity.settings.SettingsPreference.mediaFlowEnabled
import com.mycelium.wallet.activity.util.collapse
import com.mycelium.wallet.activity.util.expand
import com.mycelium.wallet.databinding.ModernMainBinding
import com.mycelium.wallet.event.*
import com.mycelium.wallet.external.changelly.ChangellyConstants
import com.mycelium.wallet.external.changelly2.ExchangeFragment
import com.mycelium.wallet.external.changelly2.HistoryFragment
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.fio.FioRequestNotificator
import com.mycelium.wallet.modularisation.ModularisationVersionHelper
import com.mycelium.wapi.api.response.Feature
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule
import com.mycelium.wapi.wallet.fio.FioModule
import com.mycelium.wapi.wallet.manager.State
import com.squareup.otto.Subscribe
import info.guardianproject.netcipher.proxy.OrbotHelper
import java.util.*
import java.util.concurrent.TimeUnit

class ModernMain : AppCompatActivity(), BackHandler {
    private lateinit var mbwManager: MbwManager
    private var mTabsAdapter: TabsAdapter? = null
    private var mExchangeTab: TabLayout.Tab? = null
    private var mBalanceTab: TabLayout.Tab? = null
    private var mNewsTab: TabLayout.Tab? = null
    private var mAccountsTab: TabLayout.Tab? = null
    private var mTransactionsTab: TabLayout.Tab? = null
    private var mRecommendationsTab: TabLayout.Tab? = null
    private var mFioRequestsTab: TabLayout.Tab? = null
    private var refreshItem: MenuItem? = null
    private var _toaster: Toaster? = null

    @Volatile
    private var _lastSync: Long = 0
    private var _isAppStart = true
    private var counter = 0
    private var balanceRefreshTimer: Timer? = null

    val backListeners = mutableListOf<BackListener>()

    lateinit var binding: ModernMainBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mbwManager = MbwManager.getInstance(this)
        WalletApplication.applyLanguageChange(baseContext, mbwManager.language)
        setContentView(ModernMainBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)
        binding.pagerTabs.setupWithViewPager(binding.pager)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayShowHomeEnabled(true)
            it.setIcon(R.drawable.action_bar_logo)
        }
        window.setBackgroundDrawableResource(R.drawable.background_main)
        binding.pager.offscreenPageLimit = 5
        mTabsAdapter = TabsAdapter(this, binding.pager, mbwManager)
        if (mediaFlowEnabled) {
            mNewsTab = binding.pagerTabs.newTab().setText(getString(R.string.media_flow)).setCustomView(R.layout.layout_exchange_tab)
            mTabsAdapter!!.addTab(mNewsTab, NewsFragment::class.java, null, TAB_NEWS)
        }
        if(SettingsPreference.isContentEnabled(ChangellyConstants.PARTNER_ID_CHANGELLY)) {
            mExchangeTab = binding.pagerTabs.newTab().setText(R.string.tab_exchange_title)
            mTabsAdapter!!.addTab(mExchangeTab, ExchangeFragment::class.java, null, TAB_EXCHANGE)
        }
        mAccountsTab = binding.pagerTabs.newTab().setText(getString(R.string.tab_accounts))
        mTabsAdapter!!.addTab(mAccountsTab, AccountsFragment::class.java, null, TAB_ACCOUNTS)
        mBalanceTab = binding.pagerTabs.newTab().setText(getString(R.string.tab_balance))
        mTabsAdapter!!.addTab(mBalanceTab, BalanceMasterFragment::class.java, null, TAB_BALANCE)
        mTransactionsTab = binding.pagerTabs.newTab().setText(getString(R.string.tab_transactions))
        mTabsAdapter!!.addTab(mTransactionsTab, TransactionHistoryFragment::class.java, null, TAB_HISTORY)
        mRecommendationsTab = binding.pagerTabs.newTab().setText(getString(R.string.tab_partners))
        mTabsAdapter!!.addTab(mRecommendationsTab,
                RecommendationsFragment::class.java, null, TAB_RECOMMENDATIONS)
        mFioRequestsTab = binding.pagerTabs.newTab().setText(getString(R.string.tab_fio_requests))
        mTabsAdapter!!.addTab(mFioRequestsTab, FioRequestsHistoryFragment::class.java, null, TAB_FIO_REQUESTS)
        val addressBookConfig = Bundle().apply {
            putBoolean(AddressBookFragment.OWN, false)
            putBoolean(AddressBookFragment.SELECT_ONLY, false)
            putBoolean(AddressBookFragment.AVAILABLE_FOR_SENDING, false)
        }
        mTabsAdapter!!.addTab(binding.pagerTabs.newTab().setText(getString(R.string.tab_addresses)), AddressBookFragment::class.java,
                addressBookConfig, TAB_ADDRESS_BOOK)
        addAdsTabs(binding.pagerTabs)
        selectTab(if (intent.getStringExtra(TAB_KEY) != null) intent.getStringExtra(TAB_KEY) else TAB_ACCOUNTS)
        _toaster = Toaster(this)
        checkTorState()
        if (savedInstanceState != null) {
            _lastSync = savedInstanceState.getLong(LAST_SYNC, 0)
            _isAppStart = savedInstanceState.getBoolean(APP_START, true)
        }
        if (_isAppStart) {
            mbwManager.versionManager.showFeatureWarningIfNeeded(this, Feature.APP_START)
            checkGapBug()
            _isAppStart = false
        }
        ModularisationVersionHelper.notifyWrongModuleVersion(this)
        handleIntent(intent)

        val tab = mTabsAdapter!!.indexOf(TAB_EXCHANGE)
        binding.pagerTabs.getTabAt(tab)?.setCustomView(R.layout.layout_exchange_tab)

        lifecycleScope.launchWhenResumed {
            ChangeLog.showIfNewVersion(this@ModernMain, supportFragmentManager)
        }
    }

    fun selectTab(tabTag: String?) {
        val selectTab = mTabsAdapter!!.indexOf(tabTag)
        if(selectTab != -1) {
            binding.pagerTabs.getTabAt(selectTab)?.select()
            binding.pager.currentItem = selectTab
        }
        intent.removeExtra(TAB_KEY)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when(intent.action) {
            NewsUtils.MEDIA_FLOW_ACTION -> {
                if(mediaFlowEnabled) {
                    selectTab(TAB_NEWS)
                    if (getIntent().hasExtra(NewsConstants.NEWS)) {
                        startActivity(Intent(this, NewsActivity::class.java)
                                .putExtras(getIntent().extras!!))
                    }
                }
            }
            FioRequestNotificator.FIO_REQUEST_ACTION -> {
                selectTab(TAB_FIO_REQUESTS)
                startActivity(Intent(this, ApproveFioRequestActivity::class.java)
                        .putExtras(getIntent().extras!!))
            }
            MainActions.ACTION_ACCOUNTS -> {
                mAccountsTab!!.select()
                binding.pager.currentItem = mTabsAdapter!!.indexOf(TAB_ACCOUNTS)
            }
            MainActions.ACTION_TXS -> {
                mTransactionsTab!!.select()
                binding.pager.currentItem = mTabsAdapter!!.indexOf(TAB_HISTORY)
            }
            MainActions.ACTION_EXCHANGE -> {
                selectTab(TAB_EXCHANGE)
            }
            else -> if(intent.hasExtra("action")) {
                startActivity(Intent(this, ActionActivity::class.java).putExtras(intent))
            }
        }
    }

    private fun addAdsTabs(tabLayout: TabLayout) {
        getMainMenuContent()?.pages?.sortedBy { it.tabIndex }?.forEach { page ->
            if (page.isActive() && isContentEnabled(page.parentId)) {
                val adsBundle = Bundle().apply {
                    putSerializable("page", page)
                }
                val tabIndex = page.tabIndex
                val newTab = tabLayout.newTab().setText(page.tabName)
                if (0 <= tabIndex && tabIndex < mTabsAdapter!!.count) {
                    mTabsAdapter!!.addTab(tabIndex, newTab,
                            AdsFragment::class.java, adsBundle, TAB_ADS + tabIndex)
                } else {
                    mTabsAdapter!!.addTab(newTab,
                            AdsFragment::class.java, adsBundle, TAB_ADS + tabIndex)
                }
            }
        }
    }

    private fun checkGapBug() {
        val module = mbwManager.getWalletManager(false).getModuleById(BitcoinHDModule.ID) as BitcoinHDModule?
        val gaps = module?.getGapsBug()
        if (gaps?.isNotEmpty() == true) {
            Preconditions.checkNotNull<BitcoinHDModule?>(module)
            val gapAddresses = module.getGapAddresses(AesKeyCipher.defaultKeyCipher())
            val gapsString = gapAddresses.joinToString(", ")
            Log.d("Gaps", gapsString)
            val s = SpannableString(resources.getString(R.string.check_gap_bug_spannable_string))
            Linkify.addLinks(s, Linkify.ALL)
            val d = AlertDialog.Builder(this).setTitle(resources.getString(R.string.account_gap)).setMessage(s)
                    .setPositiveButton(resources.getString(R.string.gaps_button_ok)) { dialog, which ->
                        createPlaceHolderAccounts(gaps)
                        mbwManager.reportIgnoredException(RuntimeException(resources.getString(R.string.address_gaps) + gapsString))
                    }.setNegativeButton(resources.getString(R.string.gaps_button_ignore), null).show()

            // Make the textview clickable. Must be called after show()
            (Objects.requireNonNull<Any?>(d.findViewById(android.R.id.message)) as TextView).movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun createPlaceHolderAccounts(gapIndex: Set<Int>) {
        val module = mbwManager.getWalletManager(false).getModuleById(BitcoinHDModule.ID) as BitcoinHDModule?
        for (index in gapIndex) {
            val newAccount = module!!.createArchivedGapFiller(AesKeyCipher.defaultKeyCipher(), index)
            mbwManager.metadataStorage.storeAccountLabel(newAccount, "Gap Account " + (index + 1))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(LAST_SYNC, _lastSync)
        outState.putBoolean(APP_START, _isAppStart)
    }

    private fun checkTorState() {
        if (mbwManager.torMode == ServerEndpointType.Types.ONLY_TOR) {
            OrbotHelper.get(this).init()
        }
    }

    protected fun stopBalanceRefreshTimer() {
        balanceRefreshTimer?.cancel()
    }

    @Subscribe
    fun malformedOutgoingTransactionFound(event: MalformedOutgoingTransactionsFound) {
        if (mbwManager.isShowQueuedTransactionsRemovalAlert) {
            // Whatever option the user choose, the confirmation dialog will not be shown
            // until the next application start
            mbwManager.isShowQueuedTransactionsRemovalAlert = false
            AlertDialog.Builder(this)
                    .setTitle(R.string.failed_transaction_removal_title)
                    .setMessage(R.string.failed_transaction_removal_message)
                    .setPositiveButton(R.string.yes) { arg0, arg1 ->
                        val account = mbwManager.getWalletManager(false).getAccount(event.account)
                        account!!.removeAllQueuedTransactions()
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
        }
    }

    override fun onStart() {
        MbwManager.getEventBus().register(this)
        val curTime = Date().time
        if (_lastSync == 0L || curTime - _lastSync > MIN_AUTOSYNC_INTERVAL) {
            val h = Handler()
            h.postDelayed({ mbwManager.versionManager.checkForUpdate() }, 50)
        }
        stopBalanceRefreshTimer()
        balanceRefreshTimer = Timer()
        balanceRefreshTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (Utils.isConnected(applicationContext)) {
                    mbwManager.exchangeRateManager.requestRefresh()

                    // if the last full sync is too old (or not known), start a full sync for _all_
                    // accounts
                    // otherwise just run a normal sync for all accounts
                    val lastFullSync = mbwManager.metadataStorage.lastFullSync
                    if (lastFullSync.isPresent
                            && Date().time - lastFullSync.get() < MIN_FULLSYNC_INTERVAL) {
                        mbwManager.getWalletManager(false)
                                .startSynchronization(SyncMode.NORMAL_ALL_ACCOUNTS_FORCED)
                    } else {
                        mbwManager.getWalletManager(false).startSynchronization(SyncMode.FULL_SYNC_ALL_ACCOUNTS)
                        mbwManager.metadataStorage.setLastFullSync(Date().time)
                    }
                    _lastSync = Date().time
                }
            }
        }, 100, MIN_AUTOSYNC_INTERVAL.toLong())
        supportInvalidateOptionsMenu()
        updateNetworkConnectionState()
        super.onStart()
    }

    override fun onStop() {
        stopBalanceRefreshTimer()
        MbwManager.getEventBus().unregister(this)
        mbwManager.versionManager.closeDialog()
        super.onStop()
    }

    override fun onBackPressed() {
        backListeners.forEach {
            if(it.onBackPressed()) {
               return
            }
        }
        if (mBalanceTab!!.isSelected) {
            // this is not finishing on Android 6 LG G4, so the pin on startup is not
            // requested.
            // commented out code above doesn't do the trick, neither.
            mbwManager.setStartUpPinUnlocked(false)
            super.onBackPressed()
        } else {
            mBalanceTab!!.select()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.record_options_menu_global, menu)
        inflater.inflate(R.menu.transaction_history_options_global, menu)
        inflater.inflate(R.menu.main_activity_options_menu, menu)
        addEnglishSetting(menu.findItem(R.id.miSettings))
        inflater.inflate(R.menu.refresh, menu)
        inflater.inflate(R.menu.addressbook_options_global, menu)
        inflater.inflate(R.menu.verify_message, menu)
        if (!(mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule?)!!.getAllRegisteredFioNames().isEmpty()) {
            inflater.inflate(R.menu.record_fio_options, menu)
        }
        inflater.inflate(R.menu.giftbox, menu)
        inflater.inflate(R.menu.exchange_changelly2, menu)
        return true
    }

    private fun addEnglishSetting(settingsItem: MenuItem) {
        val displayed = resources.getString(R.string.settings)
        val settingsEn = Utils.loadEnglish(R.string.settings)
        if (settingsEn != displayed) {
            settingsItem.title = settingsItem.title.toString() + " (" + settingsEn + ")"
        }
    }

    // controlling the behavior here is the safe but slightly slower responding
    // way of doing this.
    // controlling the visibility from the individual fragments is a bug-ridden
    // nightmare.
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val tabTag = mTabsAdapter!!.getPageTag(binding.pager.currentItem)
        // at the moment, we allow to make backups multiple times
        Preconditions.checkNotNull(menu.findItem(R.id.miBackup)).isVisible = true

        // Add Record menu
        val isAccountTab = TAB_ACCOUNTS == tabTag
        val locked = mbwManager.isKeyManagementLocked
        Preconditions.checkNotNull(menu.findItem(R.id.miAddRecord)).isVisible = isAccountTab && !locked
        Preconditions.checkNotNull(menu.findItem(R.id.miAddRecordDuplicate)).isVisible = isAccountTab && !locked

        // Lock menu
        val hasPin = mbwManager.isPinProtected
        Preconditions.checkNotNull(menu.findItem(R.id.miLockKeys)).isVisible = isAccountTab && !locked && hasPin

        // Refresh menu
        val isBalanceTab = TAB_BALANCE == tabTag
        val isHistoryTab = TAB_HISTORY == tabTag
        val isRequestsTab = TAB_FIO_REQUESTS == tabTag
        refreshItem = Preconditions.checkNotNull(menu.findItem(R.id.miRefresh))
        refreshItem?.isVisible = isBalanceTab || isHistoryTab || isRequestsTab || isAccountTab
        setRefreshAnimation()
        Preconditions.checkNotNull(menu.findItem(R.id.miRescanTransactions)).isVisible = isHistoryTab
        val isAddressBook = TAB_ADDRESS_BOOK == tabTag
        Preconditions.checkNotNull(menu.findItem(R.id.miAddAddress)).isVisible = isAddressBook
        Preconditions.checkNotNull(menu.findItem(R.id.miGiftBox)).isVisible = isContentEnabled(GiftboxConstants.PARTNER_ID)
        Preconditions.checkNotNull(menu.findItem(R.id.history)).isVisible = isContentEnabled(ChangellyConstants.PARTNER_ID_CHANGELLY)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.miColdStorage -> {
                    InstantWalletActivity.callMe(this)
                    true
                }
                R.id.miSettings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivityForResult(intent, REQUEST_SETTING_CHANGED)
                    true
                }
                R.id.miBackup -> {
                    Utils.pinProtectedWordlistBackup(this)
                    true
                }
                R.id.miRefresh -> {
                    // default only sync the current account
                    var syncMode = SyncMode.NORMAL_FORCED
                    // every 5th manual refresh make a full scan
                    if (counter == 4) {
                        syncMode = SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED
                        counter = 0
                    } else if (TAB_ACCOUNTS == mTabsAdapter!!.getPageTag(binding.pager.currentItem)) {
                        // if we are in the accounts tab, sync all accounts if the users forces a sync
                        syncMode = SyncMode.NORMAL_ALL_ACCOUNTS_FORCED
                        counter++
                    }
                    if (!startSynchronization(syncMode)) {
                        binding.pager.postDelayed({ setRefreshAnimation() }, TimeUnit.SECONDS.toMillis(1))
                    }

                    // also fetch a new exchange rate, if necessary
                    mbwManager.exchangeRateManager.requestOptionalRefresh()
                    showRefresh() // without this call sometime user not see click feedback
                    true
                }
                R.id.miRescanTransactions -> {
                    mbwManager.selectedAccount.dropCachedData()
                    startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED)
                    true
                }
                R.id.miVerifyMessage -> {
                    startActivity(Intent(this, MessageVerifyActivity::class.java))
                    true
                }
                R.id.miMyFIONames -> {
                    startActivity(Intent(this, AccountMappingActivity::class.java))
                    true
                }
                R.id.miFIORequests -> {
                    selectTab(TAB_FIO_REQUESTS)
                    true
                }
                R.id.miGiftBox -> {
                    GiftBoxRootActivity.start(this)
                    true
                }
                R.id.history -> {
                    HistoryFragment().show(supportFragmentManager, ExchangeFragment.TAG_HISTORY)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun startSynchronization(syncMode: SyncMode): Boolean {
        val account = mbwManager.selectedAccount
        val started = mbwManager.getWalletManager(false)
                .startSynchronization(syncMode, listOf(account))
        if (!started) {
            MbwManager.getEventBus().post(SyncFailed(account.id))
        }
        return started
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SETTING_CHANGED) {
            // restart activity if language changes
            // or anything else in settings. this makes some of the listeners
            // obsolete
            val running = intent
                    .putExtra(TAB_KEY, mTabsAdapter!!.getPageTag(binding.pager.currentItem))
            finish()
            startActivity(running)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun setRefreshAnimation() {
        if (refreshItem != null) {
            if (mbwManager.getWalletManager(false).state === State.SYNCHRONIZING) {
                showRefresh()
            } else {
                hideRefresh()
            }
        }
    }

    private fun hideRefresh() {
        refreshItem?.actionView = null
    }

    private fun showRefresh() {
        val menuItem = refreshItem!!.setActionView(R.layout.actionbar_indeterminate_progress)
        val ivTorIcon = menuItem.actionView.findViewById<ImageView>(R.id.ivTorIcon)
        if (mbwManager.torMode == ServerEndpointType.Types.ONLY_TOR && mbwManager.torManager != null) {
            ivTorIcon.visibility = View.VISIBLE
            if (mbwManager.torManager.initState == 100) {
                ivTorIcon.setImageResource(R.drawable.tor)
            } else {
                ivTorIcon.setImageResource(R.drawable.tor_gray)
            }
        } else {
            ivTorIcon.visibility = View.GONE
        }
    }

    fun updateNetworkConnectionState() {
        if (Utils.isConnected(this)) {
            binding.connectionError.collapse()
        } else {
            binding.connectionError.expand()
        }
    }

    @Subscribe
    fun syncStarted(event: SyncStarted?) {
        setRefreshAnimation()
        updateNetworkConnectionState()
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        setRefreshAnimation()
        updateNetworkConnectionState()
    }

    @Subscribe
    fun torState(event: TorStateChanged?) {
        setRefreshAnimation()
    }

    @Subscribe
    fun synchronizationFailed(event: SyncFailed?) {
        hideRefresh()
        updateNetworkConnectionState()
    }

    @Subscribe
    fun transactionBroadcasted(event: TransactionBroadcasted?) {
        _toaster?.toast(R.string.transaction_sent, false)
    }

    @Subscribe
    fun onNewFeatureWarnings(event: FeatureWarningsAvailable?) {
        mbwManager.versionManager.showFeatureWarningIfNeeded(this, Feature.MAIN_SCREEN)
    }

    @Subscribe
    fun onNewVersion(event: NewWalletVersionAvailable) {
        mbwManager.versionManager.showIfRelevant(event.versionInfo, this)
    }

    @Subscribe
    fun selectTab(selectTab: SelectTab) {
        selectTab(selectTab.tabTag)
    }

    @Subscribe
    fun networkConnectionChanged(event:NetworkConnectionStateChanged){
        updateNetworkConnectionState()
    }

    override fun addBackListener(listener: BackListener) {
        backListeners.add(listener)
    }

    override fun removeBackListener(listener: BackListener) {
        backListeners.remove(listener)
    }

    companion object {
        private const val TAB_NEWS = "tab_news"
        private const val TAB_ACCOUNTS = "tab_accounts"
        const val TAB_BALANCE = "tab_balance"
        const val TAB_EXCHANGE = "tab_exchange"
        private const val TAB_HISTORY = "tab_history"
        const val TAB_FIO_REQUESTS = "tab_fio_requests"
        private const val TAB_ADS = "tab_ads"
        private const val TAB_RECOMMENDATIONS = "tab_recommendations"
        private const val TAB_ADDRESS_BOOK = "tab_address_book"
        private const val TAB_KEY = "tab"
        private const val REQUEST_SETTING_CHANGED = 5
        const val MIN_AUTOSYNC_INTERVAL = Constants.MS_PR_MINUTE.toInt()
        const val MIN_FULLSYNC_INTERVAL = (5 * Constants.MS_PR_HOUR).toInt()
        const val LAST_SYNC = "LAST_SYNC"
        private const val APP_START = "APP_START"
    }
}
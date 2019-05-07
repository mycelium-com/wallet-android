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

package com.mycelium.wallet;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mrd.bitlib.util.HashUtils;
import com.mycelium.WapiLogger;
import com.mycelium.lt.api.LtApiClient;
import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.net.TorManager;
import com.mycelium.net.TorManagerOrbot;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.activity.util.BlockExplorerManager;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.coinapult.CoinapultManager;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.colu.SqliteColuManagerBacking;
import com.mycelium.wallet.event.*;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.extsig.keepkey.KeepKeyManager;
import com.mycelium.wallet.extsig.ledger.LedgerManager;
import com.mycelium.wallet.extsig.trezor.TrezorManager;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.modularisation.GooglePlayModuleCollection;
import com.mycelium.wallet.modularisation.SpvBchFetcher;
import com.mycelium.wallet.modularisation.GEBHelper;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.persistence.TradeSessionDb;
import com.mycelium.wallet.wapi.SqliteWalletManagerBackingWrapper;
import com.mycelium.wapi.api.WapiClientElectrumX;
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.Currency;
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.mycelium.wapi.wallet.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProviderProxy;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import kotlin.jvm.Synchronized;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MbwManager {
    private static final String PROXY_HOST = "socksProxyHost";
    private static final String PROXY_PORT = "socksProxyPort";
    private static final String SELECTED_ACCOUNT = "selectedAccount";
    private static volatile MbwManager _instance = null;
    private static final String TAG = "MbwManager";

    /**
     * The root index we use for generating authentication keys.
     * 0x80 makes the number negative == hardened key derivation
     * 0x424944 = "BID"
     */
    private static final int BIP32_ROOT_AUTHENTICATION_INDEX = 0x80424944;
    private Optional<CoinapultManager> _coinapultManager;
    private volatile Optional<ColuManager> _coluManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private AtomicBoolean lastPinAgeOkay = new AtomicBoolean(false);
    private ScheduledFuture<?> pinOkTimeoutHandle;
    private int failedPinCount = 0;

    private volatile boolean showQueuedTransactionsRemovalAlert = true;

    private final CurrencySwitcher _currencySwitcher;
    private boolean startUpPinUnlocked = false;
    private boolean randomizePinPad;
    private Timer _addressWatchTimer;

    public static synchronized MbwManager getInstance(final Context context) {
        if (_instance == null) {
            if (BuildConfig.DEBUG) {
                StrictMode.ThreadPolicy threadPolicy = StrictMode.allowThreadDiskReads();
                _instance = new MbwManager(context.getApplicationContext());
                StrictMode.setThreadPolicy(threadPolicy);
            } else {
                _instance = new MbwManager(context.getApplicationContext());
            }
        }
        return _instance;
    }

    private static final Bus _eventBus = new Bus();
    private final ExternalSignatureDeviceManager _trezorManager;
    private final KeepKeyManager _keepkeyManager;
    private final LedgerManager _ledgerManager;
    private final WapiClientElectrumX _wapi;
    private volatile LoadingProgressTracker migrationProgressTracker;

    private final LtApiClient _ltApi;
    private Handler _torHandler;
    private Context _applicationContext;
    private final MetadataStorage _storage;
    private LocalTraderManager _localTraderManager;
    private Pin _pin;
    private boolean _pinRequiredOnStartup;

    private static final AddressType defaultAddressType = AddressType.P2SH_P2WPKH;
    private ChangeAddressMode changeAddressMode;
    private MinerFee _minerFee;
    private boolean _keyManagementLocked;
    private MrdExport.V1.EncryptionParameters _cachedEncryptionParameters;
    private final MrdExport.V1.ScryptParameters _deviceScryptParameters;
    private MbwEnvironment _environment;
    private HttpErrorCollector _httpErrorCollector;
    private String _language;
    private final VersionManager _versionManager;
    private final ExchangeRateManager _exchangeRateManager;
    private final WalletManager _walletManager;
    private WalletManager _tempWalletManager;
    private final RandomSource _randomSource;
    private final EventTranslator _eventTranslator;
    private ServerEndpointType.Types _torMode;
    private TorManager _torManager;
    public final BlockExplorerManager _blockExplorerManager;
    private Map<Currency, CurrencySettings> currenciesSettingsMap = new HashMap<>();

    private final Queue<LogEntry> _wapiLogs;
    private Cache<String, Object> _semiPersistingBackgroundObjects = CacheBuilder.newBuilder().maximumSize(10).build();

    private WalletConfiguration configuration;
    private final GEBHelper _gebHelper = null;

    private MbwManager(Context evilContext) {
        Queue<LogEntry> unsafeWapiLogs = EvictingQueue.create(100);
        _wapiLogs  = Queues.synchronizedQueue(unsafeWapiLogs);
        _applicationContext = Preconditions.checkNotNull(evilContext.getApplicationContext());
        _environment = MbwEnvironment.verifyEnvironment();

        // Preferences
        SharedPreferences preferences = getPreferences();
        // setProxy(preferences.getString(Constants.PROXY_SETTING, ""));
        // Initialize proxy early, to enable error reporting during startup..

        configuration = new WalletConfiguration(preferences, getNetwork());

        Handler handler = new Handler(_applicationContext.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                _eventBus.register(MbwManager.this);
            }
        });


        // init tor - if needed
        try {
            setTorMode(ServerEndpointType.Types.valueOf(preferences.getString(Constants.TOR_MODE, "")));
        } catch (IllegalArgumentException ex) {
            setTorMode(ServerEndpointType.Types.ONLY_HTTPS);
        }

        migrationProgressTracker = getMigrationProgressTracker();

        _wapi = initWapi();
        configuration.setServerListChangedListener(_wapi);
        _httpErrorCollector = HttpErrorCollector.registerInVM(_applicationContext, _wapi);

        _randomSource = new AndroidRandomSource();

        // Local Trader
        TradeSessionDb tradeSessionDb = new TradeSessionDb(_applicationContext);
        _ltApi = initLt();
        _localTraderManager = new LocalTraderManager(_applicationContext, tradeSessionDb, getLtApi(), this);

        _pin = new Pin(
            preferences.getString(Constants.PIN_SETTING, ""),
            preferences.getString(Constants.PIN_SETTING_RESETTABLE, "1").equals("1")
        );
        _pinRequiredOnStartup = preferences.getBoolean(Constants.PIN_SETTING_REQUIRED_ON_STARTUP, false);
        randomizePinPad = preferences.getBoolean(Constants.RANDOMIZE_PIN, false);
        _minerFee = MinerFee.fromString(preferences.getString(Constants.MINER_FEE_SETTING, MinerFee.NORMAL.toString()));
        _keyManagementLocked = preferences.getBoolean(Constants.KEY_MANAGEMENT_LOCKED_SETTING, false);
        changeAddressMode = ChangeAddressMode.valueOf(preferences.getString(Constants.CHANGE_ADDRESS_MODE,
                ChangeAddressMode.PRIVACY.name()));

        // Get the display metrics of this device
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) _applicationContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);

        _storage = new MetadataStorage(_applicationContext);
        _language = preferences.getString(Constants.LANGUAGE_SETTING, Locale.getDefault().getLanguage());
        _versionManager = new VersionManager(_applicationContext, _language,
                new AndroidAsyncApi(_wapi, _eventBus, handler), _eventBus);

        Set<String> currencyList = getPreferences().getStringSet(Constants.SELECTED_CURRENCIES, null);
        //TODO: get it through coluManager instead ?
        Set<String> fiatCurrencies = new HashSet<>();
        if (currencyList == null || currencyList.isEmpty()) {
            //if there is no list take the default currency
            fiatCurrencies.add(Constants.DEFAULT_CURRENCY);
        } else {
            //else take all dem currencies, yeah
            fiatCurrencies.addAll(currencyList);
        }

        _exchangeRateManager = new ExchangeRateManager(_applicationContext, _wapi, getMetadataStorage());
        _currencySwitcher = new CurrencySwitcher(
            _exchangeRateManager,
            fiatCurrencies,
            preferences.getString(Constants.FIAT_CURRENCY_SETTING, Constants.DEFAULT_CURRENCY),
            Denomination.fromString(preferences.getString(Constants.BITCOIN_DENOMINATION_SETTING, Denomination.BTC.toString()))
        );

        // Check the device MemoryClass and set the scrypt-parameters for the PDF backup
        ActivityManager am = (ActivityManager) _applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();

        _deviceScryptParameters = memoryClass > 20
                                  ? MrdExport.V1.ScryptParameters.DEFAULT_PARAMS
                                  : MrdExport.V1.ScryptParameters.LOW_MEM_PARAMS;

        initPerCurrencySettings();

        _trezorManager = new TrezorManager(_applicationContext, getNetwork(), getEventBus());
        _keepkeyManager = new KeepKeyManager(_applicationContext, getNetwork(), getEventBus());
        _ledgerManager = new LedgerManager(_applicationContext, getNetwork(), getEventBus());
        _walletManager = createWalletManager(_applicationContext, _environment);
        //_gebHelper = new GEBHelper(_applicationContext);

        _eventTranslator = new EventTranslator(handler, _eventBus);
        _exchangeRateManager.subscribe(_eventTranslator);

        _walletManager.addObserver(_eventTranslator);
        _coinapultManager = createCoinapultManager();
        if (_coinapultManager.isPresent()) {
            addExtraAccounts(_coinapultManager.get());
        }

        new InitColuManagerTask().execute();
        // set the currency-list after we added all extra accounts, they may provide
        // additional needed fiat currencies
        setCurrencyList(fiatCurrencies);

        migrate();
        createTempWalletManager();

        _versionManager.initBackgroundVersionChecker();
        _blockExplorerManager = new BlockExplorerManager(this,
                _environment.getBlockExplorerList(),
                preferences.getString(Constants.BLOCK_EXPLORER,
                                      _environment.getBlockExplorerList().get(0).getIdentifier()));
    }

    private void initPerCurrencySettings() {
        initBTCSettings();
    }

    private void initBTCSettings() {
        BTCSettings btcSettings = new BTCSettings(defaultAddressType, new Reference<>(changeAddressMode));
        currenciesSettingsMap.put(Currency.BTC, btcSettings);
    }

    private class InitColuManagerTask extends AsyncTask<Void, Void, Optional<ColuManager>> {
        protected Optional<ColuManager> doInBackground(Void... params) {
            return Optional.of(getColuManager());
        }

        protected void onPostExecute(Optional<ColuManager> coluMgr) {
            _coluManager = coluMgr;
            if(_coluManager.isPresent()) {
                addExtraAccounts(_coluManager.get());
                _eventBus.post(new ExtraAccountsChanged());
            }
        }
    }

    public void addExtraAccounts(AccountProvider accounts) {
        _walletManager.addExtraAccounts(accounts);
        _hasCoinapultAccounts = null;  // invalidate cache
    }

    @Subscribe()
    public void onExtraAccountsChanged(ExtraAccountsChanged event) {
        _walletManager.refreshExtraAccounts();
        _hasCoinapultAccounts = null;  // invalidate cache
    }

    private Optional<CoinapultManager> createCoinapultManager() {
        if (_walletManager.hasBip32MasterSeed() && _storage.isPairedService(MetadataStorage.PAIRED_SERVICE_COINAPULT)) {
            BitIdKeyDerivation derivation = new BitIdKeyDerivation() {
                @Override
                public InMemoryPrivateKey deriveKey(int accountIndex, String site) {
                    try {
                        Bip39.MasterSeed masterSeed = _walletManager.getMasterSeed(AesKeyCipher.defaultKeyCipher());
                        return createBip32WebsitePrivateKey(masterSeed.getBip32Seed(), accountIndex, site);
                    } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                        throw new RuntimeException(invalidKeyCipher);
                    }
                }
            };
            return Optional.of(new CoinapultManager(
                                   _environment,
                                   derivation,
                                   _eventBus,
                                   new Handler(_applicationContext.getMainLooper()),
                                   _storage,
                                   _exchangeRateManager,
                                   retainingWapiLogger));

        } else {
            return Optional.absent();
        }
    }

    private Optional<ColuManager> createColuManager(final Context context) {
        // Create persisted account backing
        // we never talk directly to this class. Instead, we use SecureKeyValueStore API
        SqliteColuManagerBacking coluBacking = new SqliteColuManagerBacking(context);

        // Create persisted secure storage instance
        SecureKeyValueStore coluSecureKeyValueStore = new SecureKeyValueStore(coluBacking,
                new AndroidRandomSource());

        return Optional.of(new ColuManager(
                               coluSecureKeyValueStore,
                               coluBacking,
                               this,
                               _environment,
                               _eventBus,
                               new Handler(_applicationContext.getMainLooper()),
                               _storage, Utils.isConnected(context)));
    }

    private void createTempWalletManager() {
        //for managing temp accounts created through scanning
        _tempWalletManager = createTempWalletManager(_environment);
        _tempWalletManager.addObserver(_eventTranslator);
    }

    public boolean isPinPadRandomized() {
        return randomizePinPad;
    }

    public void setPinPadRandomized(boolean randomizePinPad) {
        getEditor().putBoolean(Constants.RANDOMIZE_PIN, randomizePinPad).apply();
        this.randomizePinPad = randomizePinPad;
    }

    private LtApiClient initLt() {
        return new LtApiClient(_environment.getLtEndpoints(), new LtApiClient.Logger() {
            @Override
            public void logError(String message, Exception e) {
                Log.e("", message, e);
                retainLog(Level.SEVERE, message);
            }

            @Override
            public void logError(String message) {
                Log.e("", message);
                retainLog(Level.SEVERE, message);
            }

            @Override
            public void logInfo(String message) {
                Log.i("", message);
                retainLog(Level.INFO, message);
            }
        });
    }

    private void retainLog(Level level, String message) {
        _wapiLogs.add(new LogEntry(message, level, new Date()));
    }

    public WapiLogger retainingWapiLogger = new WapiLogger() {
        @Override
        public void logError(String message) {
            Log.e("Wapi", message);
            retainLog(Level.SEVERE, message);
        }

        @Override
        public void logError(String message, Exception e) {
            Log.e("Wapi", message, e);
            retainLog(Level.SEVERE, message);
        }

        @Override
        public void logInfo(String message) {
            Log.i("Wapi", message);
            retainLog(Level.INFO, message);
        }
    };

    private WapiClientElectrumX initWapi() {
        String version = "" + BuildConfig.VERSION_CODE;

        List<TcpEndpoint> tcpEndpoints = configuration.getElectrumEndpoints();
        List<HttpEndpoint> wapiEndpoints = configuration.getWapiEndpoints();
        return new WapiClientElectrumX(new ServerEndpoints(wapiEndpoints.toArray(new HttpEndpoint[0])),
                tcpEndpoints.toArray(new TcpEndpoint[0]),
                retainingWapiLogger, version);
    }

    private void initTor() {
        _torHandler = new Handler(Looper.getMainLooper());

        if (_torMode == ServerEndpointType.Types.ONLY_TOR) {
            this._torManager = new TorManagerOrbot();
        } else {
            throw new IllegalArgumentException();
        }

        _torManager.setStateListener(new TorManager.TorState() {
            @Override
            public void onStateChange(String status, final int percentage) {
                Log.i("Tor init", status + ", " + String.valueOf(percentage));
                retainLog(Level.INFO, "Tor: " + status + ", " + String.valueOf(percentage));
                _torHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        _eventBus.post(new TorStateChanged(percentage));
                    }
                });
            }
        });

        _environment.getWapiEndpoints().setTorManager(this._torManager);
        _environment.getLtEndpoints().setTorManager(this._torManager);
    }

    public AddressType getDefaultAddressType() {
        return defaultAddressType;
    }

    public ChangeAddressMode getChangeAddressMode() {
        return changeAddressMode;
    }

    public void setChangeAddressMode(ChangeAddressMode changeAddressMode) {
        this.changeAddressMode = changeAddressMode;
        BTCSettings currencySettings = (BTCSettings) _walletManager.getCurrencySettings(Currency.BTC);
        currencySettings.setChangeAddressMode(changeAddressMode);
        _walletManager.setCurrencySettings(Currency.BTC, currencySettings);
        getEditor().putString(Constants.CHANGE_ADDRESS_MODE, changeAddressMode.toString()).apply();
    }

    /**
     * One time migration tasks that require more than what is at hands on the DB level can be
     * migrated here.
     */
    private void migrate() {
        int fromVersion = getPreferences().getInt("upToDateVersion", 0);
        if(fromVersion < 20021) {
            migrateOldKeys();
        }
        if(fromVersion < 2120029) {
            // set default address type to P2PKH for uncompressed SA accounts
            for(UUID accountId : _walletManager.getAccountIds()) {
                WalletAccount account = _walletManager.getAccount(accountId);
                if (account instanceof SingleAddressAccount) {
                    PublicKey pubKey = ((SingleAddressAccount) account).getPublicKey();
                    if(pubKey != null && !pubKey.isCompressed()) {
                        ((SingleAddressAccount) account).setDefaultAddressType(AddressType.P2PKH);
                    }
                }
            }
        }
        getPreferences().edit().putInt("upToDateVersion", 2120029).apply();
    }

    private void migrateOldKeys() {
        // We only migrate old keys if we don't have any accounts yet - otherwise, migration has already taken place
        if (!_walletManager.getAccountIds().isEmpty()) {
            return;
        }

        // Get the local trader address, may be null
        Address localTraderAddress = _localTraderManager.getLocalTraderAddress();
        if (localTraderAddress == null) {
            _localTraderManager.unsetLocalTraderAccount();
        }

        //check which address was the last recently selected one
        SharedPreferences prefs = _applicationContext.getSharedPreferences("selected", Context.MODE_PRIVATE);
        String lastAddress = prefs.getString("last", null);

        // Migrate all existing records to accounts
        List<Record> records = loadClassicRecords();
        for (Record record : records) {

            // Create an account from this record
            UUID account;
            if (record.hasPrivateKey()) {
                try {
                    account = _walletManager.createSingleAddressAccount(record.key, AesKeyCipher.defaultKeyCipher());
                    _eventBus.post(new AccountListChanged());
                } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                    throw new RuntimeException(invalidKeyCipher);
                }
            } else {
                account = _walletManager.createSingleAddressAccount(record.key.getPublicKey());
                _eventBus.post(new AccountListChanged());
            }

            //check whether this was the selected record
            if (record.address.toString().equals(lastAddress)) {
                setSelectedAccount(account);
            }

            //check whether the record was archived
            if (record.tag.equals(Record.Tag.ARCHIVE)) {
                _walletManager.getAccount(account).archiveAccount();
            }

            // See if we need to migrate this account to local trader
            if (record.address.equals(localTraderAddress)) {
                if (record.hasPrivateKey()) {
                    _localTraderManager.setLocalTraderData(account, record.key, record.address, _localTraderManager.getNickname());
                } else {
                    _localTraderManager.unsetLocalTraderAccount();
                }
            }
        }
    }

    private List<Record> loadClassicRecords() {
        SharedPreferences prefs = _applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE);
        List<Record> recordList = new LinkedList<>();

        // Load records
        String records = prefs.getString("records", "");
        for (String one : records.split(",")) {
            one = one.trim();
            if (one.length() == 0) {
                continue;
            }
            Record record = Record.fromSerializedString(one);
            if (record != null) {
                recordList.add(record);
            }
        }

        // Sort all records
        Collections.sort(recordList);
        return recordList;
    }

    /**
     * Create a Wallet Manager instance
     *
     * @param context     the application context
     * @param environment the Mycelium environment
     * @return a new wallet manager instance
     */
    private WalletManager createWalletManager(final Context context, MbwEnvironment environment) {
        // Create persisted account backing
        WalletManagerBacking backing = new SqliteWalletManagerBackingWrapper(context);

        // Create persisted secure storage instance
        SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing,
                new AndroidRandomSource());

        ExternalSignatureProviderProxy externalSignatureProviderProxy = new ExternalSignatureProviderProxy(
            getTrezorManager(),
            getKeepKeyManager(),
            getLedgerManager()
        );

        SpvBalanceFetcher spvBchFetcher = getSpvBchFetcher();
        // Create and return wallet manager
        WalletManager walletManager = new WalletManager(secureKeyValueStore, backing, environment.getNetwork(), _wapi,
                externalSignatureProviderProxy, spvBchFetcher, Utils.isConnected(context), currenciesSettingsMap, migrationProgressTracker);

        // notify the walletManager about the current selected account
        UUID lastSelectedAccountId = getLastSelectedAccountId();
        if (lastSelectedAccountId != null) {
            walletManager.setActiveAccount(lastSelectedAccountId);
        }

        importLabelsToBch(walletManager);

        return walletManager;
    }

    public void importLabelsToBch(WalletManager walletManager) {
        if (getSpvBchFetcher() == null)
            return;
        List<WalletAccount> accounts = new ArrayList<>();
        accounts.addAll(walletManager.getActiveAccounts());
        accounts.addAll(walletManager.getArchivedAccounts());
        for (WalletAccount walletAccount : accounts) {
            if (walletAccount.getType() == WalletAccount.Type.BTCSINGLEADDRESS
                    || walletAccount.getType() == WalletAccount.Type.BTCBIP44) {
                UUID bchId = getBitcoinCashAccountId(walletAccount);
                String bchLabel = getMetadataStorage().getLabelByAccount(bchId);
                if (bchLabel == null || bchLabel.isEmpty()) {
                    getMetadataStorage().storeAccountLabel(bchId, getMetadataStorage().getLabelByAccount(walletAccount.getId()));
                }
            }
        }
    }

    public static UUID getBitcoinCashAccountId(WalletAccount walletAccount) {
        return UUID.nameUUIDFromBytes(("BCH" + walletAccount.getId().toString()).getBytes());
    }

    /**
     * Create a Wallet Manager instance for temporary accounts just backed by in-memory persistence
     *
     * @param environment the Mycelium environment
     * @return a new in memory backed wallet manager instance
     */
    private WalletManager createTempWalletManager(MbwEnvironment environment) {
        // Create in-memory account backing
        WalletManagerBacking backing = new InMemoryWalletManagerBacking();

        // Create secure storage instance
        SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing, new AndroidRandomSource());


        // Create and return wallet manager
        WalletManager walletManager = new WalletManager(secureKeyValueStore, backing, environment.getNetwork(), _wapi,
                null, getSpvBchFetcher(), Utils.isConnected(_applicationContext), currenciesSettingsMap,
                migrationProgressTracker);

        walletManager.disableTransactionHistorySynchronization();
        return walletManager;
    }

    public SpvBalanceFetcher getSpvBchFetcher() {
        SpvBalanceFetcher result = null;
        if (CommunicationManager.getInstance().getPairedModules()
                .contains(GooglePlayModuleCollection.getModules(_applicationContext).get("bch"))) {
            result = new SpvBchFetcher(_applicationContext);
        }
        return result;
    }

    @Synchronized
    private LoadingProgressTracker getMigrationProgressTracker() {
        if (migrationProgressTracker == null) {
            migrationProgressTracker =  new LoadingProgressTracker(_applicationContext);
        }
        return migrationProgressTracker;
    }


    public String getFiatCurrency() {
        return _currencySwitcher.getCurrentFiatCurrency();
    }

    public boolean hasFiatCurrency() {
        return !getCurrencyList().isEmpty();
    }

    private SharedPreferences getPreferences() {
        return _applicationContext.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
    }

    public List<String> getCurrencyList() {
        return _currencySwitcher.getCurrencyList();
    }

    public void setCurrencyList(Set<String> currencies) {
        Set<String> allActiveFiatCurrencies = _walletManager.getAllActiveFiatCurrencies();
        // let the exchange-rate manager fetch all currencies, that we might need
        _exchangeRateManager.setCurrencyList(Sets.union(currencies, allActiveFiatCurrencies));

        // but tell the currency-switcher only to switch over the user selected currencies
        _currencySwitcher.setCurrencyList(currencies);

        getEditor().putStringSet(Constants.SELECTED_CURRENCIES, new HashSet<>(currencies)).apply();
    }

    public String getNextCurrency(boolean includeBitcoin) {
        return _currencySwitcher.getNextCurrency(includeBitcoin);
    }

    private SharedPreferences.Editor getEditor() {
        return getPreferences().edit();
    }

    public LocalTraderManager getLocalTraderManager() {
        return _localTraderManager;
    }

    public ExchangeRateManager getExchangeRateManager() {
        return _exchangeRateManager;
    }

    public CurrencySwitcher getCurrencySwitcher() {
        return _currencySwitcher;
    }

    public boolean isPinProtected() {
        return getPin().isSet();
    }

    // returns the age of the PIN in blocks (~10min)
    public Optional<Integer> getRemainingPinLockdownDuration() {
        Optional<Integer> pinSetHeight = getMetadataStorage().getLastPinSetBlockheight();
        int blockHeight = getSelectedAccount().getBlockChainHeight();

        if (!pinSetHeight.isPresent() || blockHeight < pinSetHeight.get()) {
            return Optional.absent();
        }

        int pinAge = blockHeight - pinSetHeight.get();
        if (pinAge > Constants.MIN_PIN_BLOCKHEIGHT_AGE_ADDITIONAL_BACKUP) {
            return Optional.of(0);
        } else {
            return Optional.of(Constants.MIN_PIN_BLOCKHEIGHT_AGE_ADDITIONAL_BACKUP - pinAge);
        }
    }

    public boolean isPinOldEnough() {
        if (!isPinProtected()) {
            return false;
        }

        Optional<Integer> pinLockdownDuration = getRemainingPinLockdownDuration();
        if (!pinLockdownDuration.isPresent()) {
            // PIN height was not set (older version) - take the current height and let the user wait...
            setPinBlockheight();
            return false;
        }
        return !(pinLockdownDuration.get() > 0);
    }

    public Pin getPin() {
        return _pin;
    }

    public void showClearPinDialog(final Activity activity, final Optional<Runnable> afterDialogClosed) {
        this.runPinProtectedFunction(activity, new ClearPinDialog(activity, true), new Runnable() {
            @Override
            public void run() {
                MbwManager.this.savePin(Pin.CLEAR_PIN);
                Toast.makeText(_applicationContext, R.string.pin_cleared, Toast.LENGTH_LONG).show();
                if (afterDialogClosed.isPresent()) {
                    afterDialogClosed.get().run();
                }
            }
        });
    }

    public void showSetPinDialog(final Activity activity, final Optional<Runnable> afterDialogClosed) {
        // Must make a backup before setting PIN
        if (this.getMetadataStorage().getMasterSeedBackupState() != MetadataStorage.BackupState.VERIFIED) {
            Utils.showSimpleMessageDialog(activity, R.string.pin_backup_first, afterDialogClosed.get());
            return;
        }

        final NewPinDialog _dialog = new NewPinDialog(activity, false);
        _dialog.setOnPinValid(new PinDialog.OnPinEntered() {
            private String newPin = null;

            @Override
            public void pinEntered(PinDialog dialog, Pin pin) {
                if (newPin == null) {
                    newPin = pin.getPin();
                    dialog.setTitle(R.string.pin_confirm_pin);
                } else if (newPin.equals(pin.getPin())) {
                    MbwManager.this.savePin(pin);
                    Toast.makeText(activity, R.string.pin_set, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    if (afterDialogClosed.isPresent()) {
                        afterDialogClosed.get().run();
                    }
                } else {
                    Toast.makeText(activity, R.string.pin_codes_dont_match, Toast.LENGTH_LONG).show();
                    MbwManager.this.vibrate();
                    dialog.dismiss();
                    if (afterDialogClosed.isPresent()) {
                        afterDialogClosed.get().run();
                    }
                }
            }
        });

        this.runPinProtectedFunction(activity, new Runnable() {
            @Override
            public void run() {
                _dialog.show();
            }
        });
    }

    public void savePin(Pin pin) {
        // if we were not pin protected and get a new pin, remember the blockheight
        // at which the pin was set - so that we can measure the age of the pin.
        if (!isPinProtected()) {
            setPinBlockheight();
        } else {
            // if we were pin-protected and now the pin is removed, reset the blockheight
            if (!pin.isSet()) {
                getMetadataStorage().clearLastPinSetBlockheight();
            }
        }
        _pin = pin;
        getEditor()
        .putString(Constants.PIN_SETTING, _pin.getPin())
        .putString(Constants.PIN_SETTING_RESETTABLE, pin.isResettable() ? "1" : "0")
        .apply();
    }

    private void setPinBlockheight() {
        int blockHeight = getSelectedAccount().getBlockChainHeight();
        getMetadataStorage().setLastPinSetBlockheight(blockHeight);
    }

    // returns the PinDialog or null, if no pin was needed
    public PinDialog runPinProtectedFunction(final Activity activity, final Runnable fun, boolean cancelable) {
        return runPinProtectedFunctionInternal(activity, fun, cancelable);
    }

    // returns the PinDialog or null, if no pin was needed
    public PinDialog runPinProtectedFunction(final Activity activity, final Runnable fun) {
        return runPinProtectedFunctionInternal(activity, fun, true);
    }

    // returns the PinDialog or null, if no pin was needed
    private PinDialog runPinProtectedFunctionInternal(Activity activity, Runnable fun, boolean cancelable) {
        if (isPinProtected() && !lastPinAgeOkay.get()) {
            PinDialog d = new PinDialog(activity, true, cancelable);
            runPinProtectedFunction(activity, d, fun);
            return d;
        } else {
            fun.run();
            return null;
        }
    }

    protected void runPinProtectedFunction(final Activity activity, PinDialog pinDialog, final Runnable fun) {
        if (isPinProtected()) {
            failedPinCount = getPreferences().getInt(Constants.FAILED_PIN_COUNT, 0);
            pinDialog.setOnPinValid(new PinDialog.OnPinEntered() {
                @Override
                public void pinEntered(final PinDialog pinDialog, Pin pin) {
                    if(failedPinCount > 0) {
                        long millis = (long) (Math.pow(1.2, failedPinCount) * 10);
                        try {
                            Thread.sleep(millis);
                        } catch (InterruptedException ignored) {
                            Toast.makeText(activity, "Something weird is happening. avoid getting to pin check", Toast.LENGTH_LONG).show();
                            vibrate();
                            pinDialog.dismiss();
                            return;
                        }
                    }
                    if (pin.equals(getPin())) {
                        failedPinCount = 0;
                        getEditor().putInt(Constants.FAILED_PIN_COUNT, failedPinCount).apply();
                        pinDialog.dismiss();

                        // as soon as you enter the correct pin once, abort the reset-pin-procedure
                        MbwManager.this.getMetadataStorage().clearResetPinStartBlockheight();
                        // if last Pin entry was 1sec ago, don't ask for it again.
                        // to prevent if there are two pin protected functions cascaded
                        // like startup-pin request and account-choose-pin request if opened by a bitcoin url
                        pinOkForOneS();

                        fun.run();
                    } else {
                        getEditor().putInt(Constants.FAILED_PIN_COUNT, ++failedPinCount).apply();
                        if (_pin.isResettable()) {
                            // Show hint, that this pin is resettable
                            new AlertDialog.Builder(activity)
                            .setTitle(R.string.pin_invalid_pin)
                            .setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    pinDialog.dismiss();
                                }
                            })
                            .setNeutralButton(activity.getString(R.string.reset_pin_button), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    pinDialog.dismiss();
                                    MbwManager.this.showClearPinDialog(activity, Optional.<Runnable>absent());
                                }
                            })

                            .setMessage(activity.getString(R.string.wrong_pin_message))
                            .show();
                        } else {
                            // This pin is not resettable, you are out of luck
                            Toast.makeText(activity, R.string.pin_invalid_pin, Toast.LENGTH_LONG).show();
                            vibrate();
                            pinDialog.dismiss();
                        }
                    }
                }
            });
            if(!activity.isFinishing()) {
                pinDialog.show();
            }
        } else {
            fun.run();
        }
    }

    public void startResetPinProcedure() {
        getMetadataStorage().setResetPinStartBlockheight(getSelectedAccount().getBlockChainHeight());
    }

    public Optional<Integer> getResetPinRemainingBlocksCount() {
        Optional<Integer> resetPinStartBlockHeight = getMetadataStorage().getResetPinStartBlockHeight();
        if (!resetPinStartBlockHeight.isPresent()) {
            // no reset procedure ongoing
            return Optional.absent();
        } else {
            int blockAge = getSelectedAccount().getBlockChainHeight() - resetPinStartBlockHeight.get();
            return Optional.of(Math.max(0, Constants.MIN_PIN_BLOCKHEIGHT_AGE_RESET_PIN - blockAge));
        }
    }

    public void vibrate() {
        Vibrator v = (Vibrator) _applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(500);
        }
    }

    public MinerFee getMinerFee() {
        return _minerFee;
    }

    public void setMinerFee(MinerFee minerFee) {
        _minerFee = minerFee;
        getEditor().putString(Constants.MINER_FEE_SETTING, _minerFee.toString()).apply();
    }

    public void setBlockExplorer(BlockExplorer blockExplorer) {
        _blockExplorerManager.setBlockExplorer(blockExplorer);
        getEditor().putString(Constants.BLOCK_EXPLORER, blockExplorer.getIdentifier()).apply();
    }


    public CoinUtil.Denomination getBitcoinDenomination() {
        return _currencySwitcher.getBitcoinDenomination();
    }

    public void setBitcoinDenomination(CoinUtil.Denomination denomination) {
        _currencySwitcher.setBitcoinDenomination(denomination);
        getEditor().putString(Constants.BITCOIN_DENOMINATION_SETTING, denomination.toString()).apply();
    }

    public String getBtcValueString(long satoshis) {
        return _currencySwitcher.getBtcValueString(satoshis);
    }

    public String getBchValueString(long satoshis) {
        return _currencySwitcher.getBchValueString(satoshis);
    }

    public boolean isKeyManagementLocked() {
        return _keyManagementLocked;
    }

    public void setKeyManagementLocked(boolean locked) {
        _keyManagementLocked = locked;
        getEditor().putBoolean(Constants.KEY_MANAGEMENT_LOCKED_SETTING, _keyManagementLocked).apply();
    }

    public void setProxy(String proxy) {
        getEditor().putString(Constants.PROXY_SETTING, proxy).apply();
        ImmutableList<String> vals = ImmutableList.copyOf(Splitter.on(":").split(proxy));
        if (vals.size() != 2) {
            noProxy();
            return;
        }
        Integer portNumber = Ints.tryParse(vals.get(1));
        if (portNumber == null || portNumber < 1 || portNumber > 65535) {
            noProxy();
            return;
        }
        String hostname = vals.get(0);
        System.setProperty(PROXY_HOST, hostname);
        System.setProperty(PROXY_PORT, portNumber.toString());
    }

    private void noProxy() {
        System.clearProperty(PROXY_HOST);
        System.clearProperty(PROXY_PORT);
    }

    public MrdExport.V1.EncryptionParameters getCachedEncryptionParameters() {
        return _cachedEncryptionParameters;
    }

    public void setCachedEncryptionParameters(MrdExport.V1.EncryptionParameters cachedEncryptionParameters) {
        _cachedEncryptionParameters = cachedEncryptionParameters;
    }

    public void clearCachedEncryptionParameters() {
        _cachedEncryptionParameters = null;
    }

    @NonNull
    public static Bus getEventBus() {
        return _eventBus;
    }

    /**
     * Get the Bitcoin network parameters that the wallet operates on
     */
    public NetworkParameters getNetwork() {
        return _environment.getNetwork();
    }

    public MbwEnvironment getEnvironmentSettings() {
        return _environment;
    }

    public void reportIgnoredException(Throwable e) {
        reportIgnoredException(null, e);
    }

    public void reportIgnoredException(String message, Throwable e) {
        if (_httpErrorCollector != null) {
            if(null != message && message.length() > 0) {
                message += "\n";
            } else {
                message = "";
            }
            RuntimeException msg = new RuntimeException("We caught an exception that we chose to ignore.\n" + message, e);
            _httpErrorCollector.reportErrorToServer(msg);
        }
    }

    public String getLanguage() {
        return _language;
    }

    public Locale getLocale() {
        return new Locale(_language);
    }

    public void setLanguage(String _language) {
        this._language = _language;
        getEditor().putString(Constants.LANGUAGE_SETTING, _language).apply();
    }

    public void setTorMode(ServerEndpointType.Types torMode) {
        this._torMode = torMode;
        getEditor().putString(Constants.TOR_MODE, torMode.toString()).apply();

        ServerEndpointType serverEndpointType = ServerEndpointType.fromType(torMode);
        if (serverEndpointType.mightUseTor()) {
            initTor();
        } else {
            if (_torManager != null) {
                _torManager.stopClient();
            }
        }

        _environment.getWapiEndpoints().setAllowedEndpointTypes(serverEndpointType);
        _environment.getLtEndpoints().setAllowedEndpointTypes(serverEndpointType);
    }

    public ServerEndpointType.Types getTorMode() {
        return _torMode;
    }

    public VersionManager getVersionManager() {
        return _versionManager;
    }

    public MrdExport.V1.ScryptParameters getDeviceScryptParameters() {
        return _deviceScryptParameters;
    }

    public WalletManager getWalletManager(boolean isColdStorage) {
        if (isColdStorage) {
            return _tempWalletManager;
        }
        return _walletManager;
    }

    public UUID createOnTheFlyAccount(Address address) {
        UUID accountId = _tempWalletManager.createSingleAddressAccount(address);
        _tempWalletManager.getAccount(accountId).setAllowZeroConfSpending(true);
        _tempWalletManager.setActiveAccount(accountId);  // this also starts a sync
        return accountId;
    }

    public UUID createOnTheFlyAccount(InMemoryPrivateKey privateKey) {
        UUID accountId;
        try {
            accountId = _tempWalletManager.createSingleAddressAccount(privateKey, AesKeyCipher.defaultKeyCipher());
        } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
        }
        _tempWalletManager.getAccount(accountId).setAllowZeroConfSpending(true);
        _tempWalletManager.setActiveAccount(accountId); // this also starts a sync
        return accountId;
    }

    public void forgetColdStorageWalletManager() {
        createTempWalletManager();
    }

    public WalletAccount getSelectedAccount() {
        UUID uuid = getLastSelectedAccountId();

        // If nothing is selected, or selected is archived, pick the first one
        if (uuid == null || !_walletManager.hasAccount(uuid) || _walletManager.getAccount(uuid).isArchived()) {
            if (_walletManager.getActiveAccounts(WalletAccount.Type.BTCBIP44).isEmpty()) {
                // That case should never happen, because we prevent users from archiving all of their
                // accounts.
                // We had a bug that allowed it, and the app will crash always after restart.
                _walletManager.activateFirstAccount();
            }
            uuid = _walletManager.getActiveAccounts(WalletAccount.Type.BTCBIP44).get(0).getId();
            setSelectedAccount(uuid);
        }

        return _walletManager.getAccount(uuid);
    }


    public Optional<UUID> getAccountId(Address address, Class accountClass) {
        Optional<UUID> result = Optional.absent();
        for (UUID uuid : _walletManager.getAccountIds()) {
            WalletAccount account = _walletManager.getAccount(uuid);
            if ((accountClass == null || accountClass.isAssignableFrom(account.getClass()))
                    && account.isMine(address)) {
                result = Optional.of(uuid);
                break;
            }
        }
        return result;
    }

    @Nullable
    private UUID getLastSelectedAccountId() {
        // Get the selected account ID
        String uuidStr = getPreferences().getString(SELECTED_ACCOUNT, "");
        UUID uuid = null;
        if (uuidStr.length() != 0) {
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                // default to null and select another account below
            }
        }
        return uuid;
    }

    public void setSelectedAccount(UUID uuid) {
        final WalletAccount account;
        account = _walletManager.getAccount(uuid);
        Preconditions.checkState(account.isActive());
        getEditor().putString(SELECTED_ACCOUNT, uuid.toString()).apply();
        getEventBus().post(new SelectedAccountChanged(uuid));
        Optional<Address> receivingAddress = account.getReceivingAddress();
        getEventBus().post(new ReceivingAddressChanged(receivingAddress));
        // notify the wallet manager that this is the active account now
        _walletManager.setActiveAccount(account.getId());
    }

    public InMemoryPrivateKey obtainPrivateKeyForAccount(WalletAccount account, String website, KeyCipher cipher) {
        if (account instanceof SingleAddressAccount) {
            // For single address accounts we use the private key directly
            try {
                return ((SingleAddressAccount) account).getPrivateKey(cipher);
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                throw new RuntimeException();
            }
        } else if (account instanceof HDAccount && ((HDAccount) account).getAccountType() == HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
            // For BIP44 accounts we derive a private key from the BIP32 hierarchy
            try {
                Bip39.MasterSeed masterSeed = _walletManager.getMasterSeed(cipher);
                int accountIndex = ((HDAccount) account).getAccountIndex();
                return createBip32WebsitePrivateKey(masterSeed.getBip32Seed(), accountIndex, website);
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                throw new RuntimeException(invalidKeyCipher);
            }
        } else {
            throw new RuntimeException("Invalid account type");
        }
    }

    public InMemoryPrivateKey getBitIdKeyForWebsite(String website) {
        try {
            IdentityAccountKeyManager identity = _walletManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher());
            return identity.getPrivateKeyForWebsite(website, AesKeyCipher.defaultKeyCipher());
        } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
        }
    }

    private InMemoryPrivateKey createBip32WebsitePrivateKey(byte[] masterSeed, int accountIndex, String site) {
        // Create BIP32 root node
        HdKeyNode rootNode = HdKeyNode.fromSeed(masterSeed, null);
        // Create bit id node
        HdKeyNode bidNode = rootNode.createChildNode(BIP32_ROOT_AUTHENTICATION_INDEX);
        // Create the private key for the specified account
        InMemoryPrivateKey accountPriv = bidNode.createChildPrivateKey(accountIndex);
        // Concatenate the private key bytes with the site name
        byte[] sitePrivateKeySeed;
        try {
            sitePrivateKeySeed = BitUtils.concatenate(accountPriv.getPrivateKeyBytes(), site.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Does not happen
            throw new RuntimeException(e);
        }
        // Hash the seed and create a new private key from that which uses compressed public keys
        byte[] sitePrivateKeyBytes = HashUtils.doubleSha256(sitePrivateKeySeed).getBytes();
        return new InMemoryPrivateKey(sitePrivateKeyBytes, true);
    }

    public UUID createAdditionalBip44Account(Context context) {
        UUID accountId;
        try {
            accountId = _walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
            //set default label for the created HD account
            WalletAccount account = _walletManager.getAccount(accountId);
            String defaultName = Utils.getNameForNewAccount(account, context);
            _storage.storeAccountLabel(accountId, defaultName);
        } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
        }
        return accountId;
    }

    public boolean isWalletPaired(ExternalService service) {
        return getMetadataStorage().isPairedService(service.getHost(getNetwork()));
    }

    public MetadataStorage getMetadataStorage() {
        return _storage;
    }

    public RandomSource getRandomSource() {
        return _randomSource;
    }

    public ExternalSignatureDeviceManager getTrezorManager() {
        return _trezorManager;
    }

    public KeepKeyManager getKeepKeyManager() {
        return _keepkeyManager;
    }

    public LedgerManager getLedgerManager() {
        return _ledgerManager;
    }

    public GEBHelper getGEBHelper() {
        return _gebHelper;
    }

    public WapiClientElectrumX getWapi() {
        return _wapi;
    }

    public Queue<LogEntry> getWapiLogs() {
        return _wapiLogs;
    }

    public TorManager getTorManager() {
        return _torManager;
    }

    public LtApiClient getLtApi() {
        return _ltApi;
    }

    @Subscribe
    public void onSelectedCurrencyChanged(SelectedCurrencyChanged event) {
        getEditor().putString(Constants.FIAT_CURRENCY_SETTING, _currencySwitcher.getCurrentFiatCurrency()).apply();
    }

    public boolean getPinRequiredOnStartup() {
        return this._pinRequiredOnStartup;
    }

    public boolean isUnlockPinRequired() {
        return getPinRequiredOnStartup() && !startUpPinUnlocked;
    }

    public void setStartUpPinUnlocked(boolean unlocked) {
        this.startUpPinUnlocked = unlocked;
    }

    public void setPinRequiredOnStartup(boolean _pinRequiredOnStartup) {
        getEditor().putBoolean(Constants.PIN_SETTING_REQUIRED_ON_STARTUP, _pinRequiredOnStartup).apply();
        this._pinRequiredOnStartup = _pinRequiredOnStartup;
    }

    /**
     * this should only be called if a coinapult account was created once.
     */
    public CoinapultManager getCoinapultManager() {
        if (_coinapultManager.isPresent()) {
            return _coinapultManager.get();
        } else {
            //lazily create one
            _coinapultManager = createCoinapultManager();
            //still not certain, if user never created one
            if (_coinapultManager.isPresent()) {
                return _coinapultManager.get();
            } else {
                throw new IllegalStateException("tried to obtain coinapult manager without having created one");
            }
        }
    }

    public synchronized ColuManager getColuManager() {
        if(_coluManager != null && _coluManager.isPresent()) {
            return _coluManager.get();
        } else {
            _coluManager = createColuManager(_applicationContext);
            if (_coluManager.isPresent()) {
                return _coluManager.get();
            } else {
                throw new IllegalStateException("Tried to obtain colu manager without having created one.");
            }
        }
    }

    public Cache<String, Object> getBackgroundObjectsCache() {
        return _semiPersistingBackgroundObjects;
    }

    public void stopWatchingAddress() {
        if (_addressWatchTimer != null) {
            _addressWatchTimer.cancel();
        }
    }

    public void watchAddress(final Address address) {
        stopWatchingAddress();
        _addressWatchTimer = new Timer();
        _addressWatchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getWalletManager(false).startSynchronization(new SyncMode(address));
            }
        }, 1000, 5 * 1000);
    }

    private Boolean _hasCoinapultAccounts = null;

    public boolean hasCoinapultAccount() {
        if (_hasCoinapultAccounts == null) {
            _hasCoinapultAccounts = getMetadataStorage().isPairedService(MetadataStorage.PAIRED_SERVICE_COINAPULT);
        }
        return _hasCoinapultAccounts;
    }

    public boolean hasColoredAccounts() {
        return getMetadataStorage().isPairedService(MetadataStorage.PAIRED_SERVICE_COLU);
    }

    private void pinOkForOneS() {
        if(pinOkTimeoutHandle != null) {
            pinOkTimeoutHandle.cancel(true);
        }
        lastPinAgeOkay.set(true);
        pinOkTimeoutHandle = scheduler.schedule(new Runnable() {
            public void run() {
                lastPinAgeOkay.set(false);
            }
        }, 1, SECONDS);
    }

    // Derivation constants for mycelium messages' signing key
    private static final int DERIVATION_NUMBER_LEVEL_ONE = 1234;
    private static final int DERIVATION_NUMBER_LEVEL_TWO = 7865;

    // Returns the public part of mycelium messages' signing key called 'myceliumId'
    public String getMyceliumId() {
        try {
            PrivateKey privateKey = getMessagesSigningKey();
            return privateKey.getPublicKey().toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // Derives a key for signing messages (messages signing key) from the master seed
    private PrivateKey getMessagesSigningKey() throws KeyCipher.InvalidKeyCipher {
        Bip39.MasterSeed seed = getWalletManager(false).getMasterSeed(AesKeyCipher.defaultKeyCipher());
        return HdKeyNode.fromSeed(seed.getBip32Seed(), null).createChildNode(DERIVATION_NUMBER_LEVEL_ONE).createChildNode(DERIVATION_NUMBER_LEVEL_TWO).getPrivateKey();
    }

    // Signs a message using the mycelium messages' signing key
    public String signMessage(String unsignedMessage) {
        try {
            PrivateKey privateKey = getMessagesSigningKey();
            SignedMessage signedMessage = privateKey.signMessage(unsignedMessage);
            return signedMessage.getBase64Signature();
        } catch (Exception ex) {
            return "";
        }
    }

    public boolean isShowQueuedTransactionsRemovalAlert() {
        return showQueuedTransactionsRemovalAlert;
    }

    public void setShowQueuedTransactionsRemovalAlert(boolean showQueuedTransactionsRemovalAlert) {
        this.showQueuedTransactionsRemovalAlert = showQueuedTransactionsRemovalAlert;
    }
}

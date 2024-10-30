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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mycelium.wapi.wallet.fio.FioModuleKt.getActiveFioAccount;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;
import com.mycelium.bequant.InvestmentModule;
import com.mycelium.generated.logger.database.LoggerDB;
import com.mycelium.generated.logger.database.Logs;
import com.mycelium.generated.rates.database.RatesDB;
import com.mycelium.generated.wallet.database.WalletDB;
import com.mycelium.lt.api.LtApiClient;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.net.TorManager;
import com.mycelium.net.TorManagerOrbot;
import com.mycelium.view.Denomination;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.activity.util.GlobalBlockExplorerManager;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.bitid.ExternalService;
import com.mycelium.wallet.colu.SqliteColuManagerBacking;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountSyncStopped;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.EventTranslator;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.event.TorStateChanged;
import com.mycelium.wallet.event.TransactionBroadcasted;
import com.mycelium.wallet.exchange.RatesBacking;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.extsig.keepkey.KeepKeyManager;
import com.mycelium.wallet.extsig.ledger.LedgerManager;
import com.mycelium.wallet.extsig.trezor.TrezorManager;
import com.mycelium.wallet.fio.AbiFioSerializationProviderWrapper;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.modularisation.GooglePlayModuleCollection;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.persistence.TradeSessionDb;
import com.mycelium.wallet.wapi.SqliteBtcWalletManagerBacking;
import com.mycelium.wapi.api.WapiClientElectrumX;
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint;
import com.mycelium.wapi.content.ContentResolver;
import com.mycelium.wapi.content.btc.BitcoinUriParser;
import com.mycelium.wapi.content.btcv.BitcoinVaultUriParser;
import com.mycelium.wapi.content.colu.mss.MSSUriParser;
import com.mycelium.wapi.content.colu.mt.MTUriParser;
import com.mycelium.wapi.content.colu.rmc.RMCUriParser;
import com.mycelium.wapi.content.eth.EthUriParser;
import com.mycelium.wapi.content.fio.FIOUriParser;
import com.mycelium.wapi.wallet.AccountListener;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.CurrencySettings;
import com.mycelium.wapi.wallet.IdentityAccountKeyManager;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount;
import com.mycelium.wapi.wallet.btc.BTCSettings;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.BtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.ChangeAddressMode;
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.Reference;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModuleKt;
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProviderProxy;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.btc.single.AddressSingleConfig;
import com.mycelium.wapi.wallet.btc.single.BitcoinSingleAddressModule;
import com.mycelium.wapi.wallet.btc.single.PrivateSingleConfig;
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore;
import com.mycelium.wapi.wallet.btc.single.PublicSingleConfig;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHDBacking;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHDModule;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.ColuApiImpl;
import com.mycelium.wapi.wallet.colu.ColuClient;
import com.mycelium.wapi.wallet.colu.ColuModule;
import com.mycelium.wapi.wallet.erc20.ERC20Backing;
import com.mycelium.wapi.wallet.erc20.ERC20Module;
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.eth.EthAccountContext;
import com.mycelium.wapi.wallet.eth.EthAddress;
import com.mycelium.wapi.wallet.eth.EthAddressConfig;
import com.mycelium.wapi.wallet.eth.EthBacking;
import com.mycelium.wapi.wallet.eth.EthBlockchainService;
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig;
import com.mycelium.wapi.wallet.eth.EthereumModule;
import com.mycelium.wapi.wallet.fiat.coins.FiatType;
import com.mycelium.wapi.wallet.fio.FIOAddressConfig;
import com.mycelium.wapi.wallet.fio.FIOMasterseedConfig;
import com.mycelium.wapi.wallet.fio.FIOPrivateKeyConfig;
import com.mycelium.wapi.wallet.fio.FioAccount;
import com.mycelium.wapi.wallet.fio.FioAccountContext;
import com.mycelium.wapi.wallet.fio.FioAddress;
import com.mycelium.wapi.wallet.fio.FioApiEndpoints;
import com.mycelium.wapi.wallet.fio.FioBacking;
import com.mycelium.wapi.wallet.fio.FioEndpoints;
import com.mycelium.wapi.wallet.fio.FioHistoryEndpoints;
import com.mycelium.wapi.wallet.fio.FioKeyManager;
import com.mycelium.wapi.wallet.fio.FioModule;
import com.mycelium.wapi.wallet.fio.RecordObtData;
import com.mycelium.wapi.wallet.fio.coins.FIOToken;
import com.mycelium.wapi.wallet.genericdb.AccountContextsBacking;
import com.mycelium.wapi.wallet.genericdb.AdaptersKt;
import com.mycelium.wapi.wallet.genericdb.Backing;
import com.mycelium.wapi.wallet.genericdb.InMemoryAccountContextsBacking;
import com.mycelium.wapi.wallet.manager.Config;
import com.mycelium.wapi.wallet.manager.WalletListener;
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager;
import com.mycelium.wapi.wallet.providers.FeeProvider;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import app.cash.sqldelight.android.AndroidSqliteDriver;
import app.cash.sqldelight.db.SqlDriver;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import fiofoundation.io.fiosdk.errors.FIOError;
import kotlin.jvm.Synchronized;

public class MbwManager {
    private static final String PROXY_HOST = "socksProxyHost";
    private static final String PROXY_PORT = "socksProxyPort";
    private static final String SELECTED_ACCOUNT = "selectedAccount";
    private static volatile MbwManager _instance = null;

    /**
     * The root index we use for generating authentication keys.
     * 0x80 makes the number negative == hardened key derivation
     * 0x424944 = "BID"
     */
    private static final int BIP32_ROOT_AUTHENTICATION_INDEX = 0x80424944;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private AtomicBoolean lastPinAgeOkay = new AtomicBoolean(false);
    private ScheduledFuture<?> pinOkTimeoutHandle;
    private int failedPinCount = 0;

    private volatile boolean showQueuedTransactionsRemovalAlert = true;
    private final CurrencySwitcher _currencySwitcher;
    private boolean startUpPinUnlocked = false;
    private boolean randomizePinPad;
    private Timer _addressWatchTimer;
    private final WalletDB db;
    private LoggerDB loggerDB;
    private Logger logger = Logger.getLogger(MbwManager.class.getSimpleName());

    @Nonnull
    public static synchronized MbwManager getInstance(Context context) {
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
    private final WapiClientElectrumX btcvWapi;
    private volatile LoadingProgressTracker migrationProgressTracker;

    private final LtApiClient _ltApi;
    private Handler _torHandler;
    private Context _applicationContext;
    private LocalTraderManager _localTraderManager;
    private Pin _pin;
    private boolean _pinRequiredOnStartup;

    private static final AddressType defaultAddressType = AddressType.P2SH_P2WPKH;
    private ChangeAddressMode changeAddressMode;
    private Map<String, MinerFee> _minerFee;
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
    private MasterSeedManager masterSeedManager;
    private ContentResolver contentResolver;
    private final RandomSource _randomSource;
    private final EventTranslator _eventTranslator;
    private ServerEndpointType.Types _torMode;
    private TorManager _torManager;
    public final GlobalBlockExplorerManager _blockExplorerManager;
    private HashMap<String, CurrencySettings> currenciesSettingsMap = new HashMap<>();

    private Cache<String, Object> _semiPersistingBackgroundObjects = CacheBuilder.newBuilder().maximumSize(10).build();

    private WalletConfiguration configuration;

    private final Handler mainLoopHandler;
    private boolean appInForeground = false;
    private int activityCount = 0;

    private MbwManager(Context evilContext) {
        _applicationContext = checkNotNull(evilContext.getApplicationContext());
        //accountsDao = AccountsDB.getDatabase(_applicationContext).contextDao();
        _environment = MbwEnvironment.verifyEnvironment();
        startLogger();

        SqlDriver driver = new AndroidSqliteDriver(WalletDB.Companion.getSchema(), _applicationContext, "wallet.db");
        db = WalletDB.Companion.invoke(driver, AdaptersKt.getAccountBackingAdapter(), AdaptersKt.getAccountContextAdapter(),
                AdaptersKt.getBTCVAccountBackingAdapter(), AdaptersKt.getBTCVContextAdapter(),
                AdaptersKt.getBTCVOutgoingTxAdapter(), AdaptersKt.getBTCVPtxoAdapter(),
                AdaptersKt.getBTCVRefersPtxoAdapter(), AdaptersKt.getBTCVTransactionAdapter(), AdaptersKt.getBTCVUtxoAdapter(),
                AdaptersKt.getErc20ContextAdapter(), AdaptersKt.getEthAccountBackingAdapter(), AdaptersKt.getEthContextAdapter(),
                AdaptersKt.getFeeEstimatorAdapter(), AdaptersKt.getFioAccountBackingAdapter(), AdaptersKt.getFioContextAdapter(),
                AdaptersKt.getFioKnownNamesAdapter(), AdaptersKt.getFioNameAccountMappingsAdapter(),
                AdaptersKt.getFioOtherBlockchainTransactionsAdapter(),
                AdaptersKt.getFioReceivedRequestsAdapter(), AdaptersKt.getFioSentRequestsAdapter());
        driver.execute(null, "PRAGMA foreign_keys=ON;", 0, null);

        // Preferences
        SharedPreferences preferences = getPreferences();
        configuration = new WalletConfiguration(preferences, getNetwork());
        configuration.loadFromAssetsIfNeed();

        mainLoopHandler = new Handler(Looper.getMainLooper());
        _eventTranslator = new EventTranslator(mainLoopHandler, _eventBus);
        mainLoopHandler.post(() -> _eventBus.register(MbwManager.this));

        migrationProgressTracker = getMigrationProgressTracker();

        _wapi = initWapi(configuration.getElectrumEndpoints(), configuration.getWapiEndpoints());
        List<TcpEndpoint> btcvEndpoints = configuration.getElectrumVEndpoints();
        btcvWapi = initWapi(btcvEndpoints, configuration.getWapiEndpoints(), false);
        configuration.setElectrumServerListChangedListener(_wapi);
        configuration.setElectrumVServerListChangedListener(btcvWapi);
        _httpErrorCollector = HttpErrorCollector.registerInVM(_applicationContext, _wapi);

        _randomSource = new AndroidRandomSource();

        // Local Trader
        TradeSessionDb tradeSessionDb = new TradeSessionDb(_applicationContext);
        _ltApi = initLt();
        _localTraderManager = new LocalTraderManager(_applicationContext, tradeSessionDb, getLtApi(), this);

        updateTorMode(preferences);

        _pin = new Pin(
                preferences.getString(Constants.PIN_SETTING, ""),
                preferences.getString(Constants.PIN_SETTING_RESETTABLE, "1").equals("1")
        );
        _pinRequiredOnStartup = preferences.getBoolean(Constants.PIN_SETTING_REQUIRED_ON_STARTUP, false);
        randomizePinPad = preferences.getBoolean(Constants.RANDOMIZE_PIN, false);
        _keyManagementLocked = preferences.getBoolean(Constants.KEY_MANAGEMENT_LOCKED_SETTING, false);
        changeAddressMode = ChangeAddressMode.valueOf(preferences.getString(Constants.CHANGE_ADDRESS_MODE,
                ChangeAddressMode.PRIVACY.name()));

        _language = preferences.getString(Constants.LANGUAGE_SETTING, Locale.getDefault().getLanguage());
        _versionManager = new VersionManager(_applicationContext, _language, new AndroidAsyncApi(_wapi, _eventBus, mainLoopHandler), _eventBus);

        Set<String> currencyList = getPreferences().getStringSet(Constants.SELECTED_CURRENCIES, null);
        Set<AssetInfo> fiatCurrencies = new HashSet<>();
        if (currencyList == null || currencyList.isEmpty()) {
            //if there is no list take the default currency
            fiatCurrencies.add(new FiatType(Constants.DEFAULT_CURRENCY));
        } else {
            //else take all dem currencies, yeah
            for (String currency : currencyList) {
                fiatCurrencies.add(new FiatType(currency));
            }
        }



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
        _walletManager = createWalletManager(_applicationContext, _environment, db);
        contentResolver = createContentResolver(getNetwork());

        migrate();
        SqlDriver rateDriver = new AndroidSqliteDriver(RatesDB.Companion.getSchema(), _applicationContext, "exchange.db");
        RatesDB ratesDB = RatesDB.Companion.invoke(rateDriver);
        _exchangeRateManager = new ExchangeRateManager(_applicationContext, _wapi, new RatesBacking(ratesDB));
        _exchangeRateManager.subscribe(_eventTranslator);
        _walletManager.addObserver(_eventTranslator);

        _minerFee = getMinerFeeMap(preferences);
        _currencySwitcher = createCurrencySwitcher(preferences, fiatCurrencies);
        // set the currency-list after we added all extra accounts, they may provide
        // additional needed fiat currencies
        setCurrencyList(fiatCurrencies);
        createTempWalletManager();
        if (_walletManager.getAssetTypes().size() != 0) {
            _currencySwitcher.setWalletCurrencies(_walletManager.getAssetTypes());
        }

        //should be called after all accounts are loaded and managers created only
        updateConfig();

        _versionManager.initBackgroundVersionChecker();

        _blockExplorerManager = getBlockExplorerManager(preferences);
    }

    private void updateTorMode(SharedPreferences preferences) {
        // init tor - if needed
        try {
            setTorMode(ServerEndpointType.Types.valueOf(preferences.getString(Constants.TOR_MODE, "")));
        } catch (IllegalArgumentException ex) {
            setTorMode(ServerEndpointType.Types.ONLY_HTTPS);
        }
    }

    private void startLogger() {
        LogManager.getLogManager().reset();
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        SqlDriver driver = new AndroidSqliteDriver(LoggerDB.Companion.getSchema(), _applicationContext, "logger.db");
        loggerDB = LoggerDB.Companion.invoke(driver);
        DbLogHandler handler = new DbLogHandler(loggerDB);
        rootLogger.addHandler(handler);
        rootLogger.addHandler(new AndroidLogHandler());
        handler.cleanUp();
        logger.log(Level.INFO, "Logging started...");
    }

    private CurrencySwitcher createCurrencySwitcher(SharedPreferences preferences, Set<AssetInfo> fiatCurrencies) {
        String defaultCurrencyJson = "{\"" + Utils.getBtcCoinType().getName() + "\":\"" + Constants.DEFAULT_CURRENCY + "\"}";
        String currentCurrenciesString = preferences.getString(Constants.CURRENT_CURRENCIES_SETTING, defaultCurrencyJson);
        String currentFiatString = preferences.getString(Constants.FIAT_CURRENCIES_SETTING, defaultCurrencyJson);
        Map<AssetInfo, AssetInfo> currentCurrencyMap = getCurrentCurrenciesMap(currentCurrenciesString);
        Map<AssetInfo, AssetInfo> currentFiatMap = getCurrentCurrenciesMap(currentFiatString);
        return new CurrencySwitcher(
                _exchangeRateManager,
                fiatCurrencies,
                new FiatType(preferences.getString(Constants.TOTAL_FIAT_CURRENCY_SETTING, Constants.DEFAULT_CURRENCY)),
                currentCurrencyMap,
                currentFiatMap,
                getDenominationMap(preferences)
        );
    }

    private GlobalBlockExplorerManager getBlockExplorerManager(SharedPreferences preferences) {
        Gson gson = new GsonBuilder().create();
        Map<String, String> resultMap = new HashMap<>();

        String preferenceValue = preferences.getString(Constants.BLOCK_EXPLORERS, null);
        if (preferenceValue == null) {
            for (Map.Entry<String, List<BlockExplorer>> entry : _environment.getBlockExplorerMap().entrySet()) {
                resultMap.put(entry.getKey(), entry.getValue().get(0).getIdentifier());
            }
        } else {
            resultMap = gson.fromJson(preferenceValue, new TypeToken<Map<String, String>>() {
            }.getType());
        }
        return new GlobalBlockExplorerManager(this, _environment.getBlockExplorerMap(), resultMap);
    }

    private Map<String, MinerFee> getMinerFeeMap(SharedPreferences preferences) {
        Gson gson = new GsonBuilder().create();
        Map<String, MinerFee> resultMap = new HashMap<>();

        String preferenceValue = preferences.getString(Constants.MINER_FEE_SETTING, null);
        if (preferenceValue == null) {
            resultMap.put(Utils.getBtcCoinType().getName(), MinerFee.NORMAL);
        } else {
            Map<String, String> preferenceMap = gson.fromJson(preferenceValue, new TypeToken<Map<String, String>>() {
            }.getType());
            for (Map.Entry<String, String> entry : preferenceMap.entrySet()) {
                resultMap.put(entry.getKey(), MinerFee.fromString(entry.getValue()));
            }
        }
        return resultMap;
    }

    private Map<AssetInfo, Denomination> getDenominationMap(SharedPreferences preferences) {
        Gson gson = new GsonBuilder().create();
        Map<AssetInfo, Denomination> resultMap = new HashMap<>();

        String preferenceValue = preferences.getString(Constants.DENOMINATION_SETTING, null);
        if (preferenceValue == null) {
            resultMap.put(Utils.getBtcCoinType(), Denomination.UNIT);
        } else {
            Map<String, String> preferenceMap = gson.fromJson(preferenceValue, new TypeToken<Map<String, String>>() {
            }.getType());
            for (Map.Entry<String, String> entry : preferenceMap.entrySet()) {
                resultMap.put(Utils.getTypeByName(entry.getKey()), Denomination.fromString(entry.getValue()));
            }
        }
        return resultMap;
    }

    private Map<AssetInfo, AssetInfo> getCurrentCurrenciesMap(String jsonString) {
        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<AssetInfo, AssetInfo> result = new HashMap<>();

        Map<String, String> coinNamesMap = gson.fromJson(jsonString, type);
        for (Map.Entry<String, String> entry : coinNamesMap.entrySet()) {
            result.put(Utils.getTypeByName(entry.getKey()), Utils.getTypeByName(entry.getValue()));
        }
        return result;
    }

    private String getCurrentCurrenciesString(Map<AssetInfo, AssetInfo> currenciesMap) {
        Gson gson = new GsonBuilder().create();
        Map<String, String> coinNamesMap = new HashMap<>();

        for (Map.Entry<AssetInfo, AssetInfo> entry : currenciesMap.entrySet()) {
            coinNamesMap.put(entry.getKey().getName(), entry.getValue().getName());
        }
        return gson.toJson(coinNamesMap);
    }

    public void updateConfig() {
        configuration.updateConfig();
        updateTorMode(getPreferences());
    }

    private ContentResolver createContentResolver(NetworkParameters network) {
        ContentResolver result = new ContentResolver();
        result.add(new BitcoinUriParser(network));
        result.add(new MTUriParser(network));
        result.add(new MSSUriParser(network));
        result.add(new RMCUriParser(network));
        result.add(new EthUriParser(network, getSupportedERC20Tokens()));
        result.add(new FIOUriParser(network));
        result.add(new BitcoinVaultUriParser(network));
        return result;
    }

    public ContentResolver getContentResolver() {
        return contentResolver;
    }

    public FioEndpoints getFioEndpoints() {
        return new FioEndpoints(new FioApiEndpoints(configuration.getFioApiEndpoints()),
                new FioHistoryEndpoints(configuration.getFioHistoryEndpoints()));
    }

    public Map<String, ERC20Token> getSupportedERC20Tokens() {
        return configuration.getSupportedERC20Tokens();
    }

    private void initPerCurrencySettings() {
        initBTCSettings();
        initBTCVSettings();
    }

    private void initBTCSettings() {
        BTCSettings btcSettings = new BTCSettings(defaultAddressType, new Reference<>(changeAddressMode));
        currenciesSettingsMap.put(BitcoinHDModule.ID, btcSettings);
        currenciesSettingsMap.put(BitcoinSingleAddressModule.ID, btcSettings);
    }

    private void initBTCVSettings() {
        BTCSettings btcSettings = new BTCSettings(defaultAddressType, new Reference<>(changeAddressMode));
        currenciesSettingsMap.put(BitcoinVaultHDModule.ID, btcSettings);
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

    public boolean isFingerprintEnabled() {
        return getPreferences().getBoolean(Constants.FINGERPRINT, false);
    }

    public void setFingerprintEnabled(boolean enable) {
        getEditor().putBoolean(Constants.FINGERPRINT, enable).apply();
    }

    public boolean isTwoFactorEnabled() {
        return getPreferences().getBoolean(Constants.TWO_FACTOR, false);
    }

    public void setTwoFactorEnabled(boolean enable) {
        getEditor().putBoolean(Constants.TWO_FACTOR, enable).apply();
    }


    private LtApiClient initLt() {
        return new LtApiClient(_environment.getLtEndpoints());
    }

    private WapiClientElectrumX initWapi(List<TcpEndpoint> tcpEndpoints, List<HttpEndpoint> wapiEndpoints) {
        return initWapi(tcpEndpoints, wapiEndpoints, true);
    }

    private WapiClientElectrumX initWapi(
            List<TcpEndpoint> tcpEndpoints,
            List<HttpEndpoint> wapiEndpoints,
            boolean isActive
    ) {
        String version = String.valueOf(BuildConfig.VERSION_CODE);

        WapiClientElectrumX wapiClientElectrumX = new WapiClientElectrumX(
                new ServerEndpoints(wapiEndpoints.toArray(new HttpEndpoint[0])),
                tcpEndpoints.toArray(new TcpEndpoint[0]),
                version,
                Build.VERSION.SDK_INT,
                isActive
        );

        wapiClientElectrumX.setNetworkConnected(Utils.isConnected(_applicationContext));
        return wapiClientElectrumX;
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
                logger.log(Level.INFO, "Tor: " + status + ", " + percentage);
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
        _wapi.getServerEndpoints().setTorManager(this._torManager);
    }

    public AddressType getDefaultAddressType() {
        return defaultAddressType;
    }

    public ChangeAddressMode getChangeAddressMode() {
        return changeAddressMode;
    }

    public void setChangeAddressMode(ChangeAddressMode changeAddressMode) {
        this.changeAddressMode = changeAddressMode;
        BTCSettings currencySettings = (BTCSettings) _walletManager.getCurrencySettings(BitcoinHDModule.ID);
        if (currencySettings != null) {
            currencySettings.setChangeAddressMode(changeAddressMode);
            _walletManager.setCurrencySettings(BitcoinHDModule.ID, currencySettings);
            getEditor().putString(Constants.CHANGE_ADDRESS_MODE, changeAddressMode.toString()).apply();
        }
    }

    /**
     * One time migration tasks that require more than what is at hands on the DB level can be
     * migrated here.
     */
    private void migrate() {
        int fromVersion = getPreferences().getInt("upToDateVersion", 0);
        if (fromVersion < 20021) {
            migrateOldKeys();
        }
        if (fromVersion < 2120029) {
            // set default address type to P2PKH for uncompressed SA accounts
            for (UUID accountId : _walletManager.getAccountIds()) {
                WalletAccount account = _walletManager.getAccount(accountId);
                if (account instanceof SingleAddressAccount) {
                    PublicKey pubKey = ((SingleAddressAccount) account).getPublicKey();
                    if (pubKey != null && !pubKey.isCompressed()) {
                        ((SingleAddressAccount) account).setDefaultAddressType(AddressType.P2PKH);
                    }
                }
            }
        }
        if (fromVersion < 3030000) {
            migratePreferences();
        }
        getPreferences().edit().putInt("upToDateVersion", 3030000).apply();
    }

    /**
     * put previously single values into map-like values where key is a coin type
     * i.e. "mbtc" -> {"Bitcoin": "mbtc"}
     */
    private void migratePreferences() {
        Gson gson = new GsonBuilder().create();

        // Miner fee
        String oldFee = getPreferences().getString(Constants.MINER_FEE_SETTING, null);
        if (oldFee != null) {
            Map<String, MinerFee> newFee = new HashMap<>();
            newFee.put(Utils.getBtcCoinType().getName(), MinerFee.fromString(oldFee));
            getEditor().putString(Constants.MINER_FEE_SETTING, gson.toJson(newFee)).commit();
        }

        // Block explorer
        String oldExplorer = getPreferences().getString("BlockExplorer", null);
        if (oldExplorer != null) {
            Map<String, String> newExplorer = new HashMap<>();
            for (Map.Entry<String, List<BlockExplorer>> entry : _environment.getBlockExplorerMap().entrySet()) {
                newExplorer.put(entry.getKey(), entry.getValue().get(0).getIdentifier());
            }
            newExplorer.put(Utils.getBtcCoinType().getName(), oldExplorer);
            getEditor().remove("BlockExplorer") // don't use old name anymore
                    .putString(Constants.BLOCK_EXPLORERS, gson.toJson(newExplorer)).commit();
        }

        // Denomination
        String oldDenomination = getPreferences().getString("BitcoinDenomination", null);
        if (oldDenomination != null) {
            Map<String, Denomination> newDenomination = new HashMap<>();
            newDenomination.put(Utils.getBtcCoinType().getName(), Denomination.fromString(oldDenomination));
            getEditor().remove("BitcoinDenomination") // don't use old name anymore
                    .putString(Constants.DENOMINATION_SETTING, gson.toJson(newDenomination)).commit();
        }

        // Exchange rates source (different preference file)
        SharedPreferences exchangeSourcePref = _applicationContext.getSharedPreferences(Constants.EXCHANGE_DATA, Activity.MODE_PRIVATE);
        String oldSource = exchangeSourcePref.getString(Constants.EXCHANGE_RATE_SETTING, null);
        if (oldSource != null) {
            Map<String, String> newSource = new HashMap<>();
            newSource.put(Utils.getBtcCoinType().getSymbol(), oldSource);
            exchangeSourcePref.edit().putString(Constants.EXCHANGE_RATE_SETTING, gson.toJson(newSource)).apply();
        }
    }

    private void migrateOldKeys() {
        // We only migrate old keys if we don't have any accounts yet - otherwise, migration has already taken place
        if (!_walletManager.getAccountIds().isEmpty()) {
            return;
        }

        // Get the local trader address, may be null
        BitcoinAddress localTraderAddress = _localTraderManager.getLocalTraderAddress();
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
                account = _walletManager.createAccounts(new PrivateSingleConfig(record.key, AesKeyCipher.defaultKeyCipher())).get(0);
            } else {
                account = _walletManager.createAccounts(new PublicSingleConfig(record.key.getPublicKey())).get(0);
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
            if (BitcoinAddress.fromString(record.address.toString()).equals(localTraderAddress)) {
                if (record.hasPrivateKey()) {
                    _localTraderManager.setLocalTraderData(account, record.key, BitcoinAddress.fromString(record.address.toString()),
                            _localTraderManager.getNickname());
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

    private AccountListener accountListener = new AccountListener() {
        @Override
        public void receivingAddressChanged(@NotNull WalletAccount<?> walletAccount, @NotNull Address receivingAddress) {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new ReceivingAddressChanged(receivingAddress));
                }
            });
        }

        @Override
        public void serverConnectionError(WalletAccount<?> walletAccount, @NotNull String s) {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new SyncFailed(walletAccount.getId()));
                }
            });
        }

        @Override
        public void balanceUpdated(final WalletAccount<?> walletAccount) {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new BalanceChanged(walletAccount.getId()));
                }
            });
        }

        @Override
        public void onAccountActiveStateChanged(@NonNull final UUID id) {
            WalletAccount<?> account = _walletManager.getAccount(id);
            if (account instanceof BitcoinVaultHdAccount) {
                BitcoinVaultHDModule module =
                        (BitcoinVaultHDModule) _walletManager.getModuleById(BitcoinVaultHDModule.ID);
                if (module != null) {
                    module.setupClientIsActive();
                }
            }
        }
    };

    /**
     * Create a Wallet Manager instance
     *
     * @param context     the application context
     * @param environment the Mycelium environment
     * @return a new wallet manager instance
     */
    private WalletManager createWalletManager(final Context context, final MbwEnvironment environment,
                                              final WalletDB walletDB) {
        // Create persisted account backing
        BtcWalletManagerBacking backing = new SqliteBtcWalletManagerBacking(context);

        // Create persisted secure storage instance
        SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing,
                new AndroidRandomSource());

        masterSeedManager = new MasterSeedManager(secureKeyValueStore);
        final WalletManager walletManager = new WalletManager(environment.getNetwork(),
                _wapi, btcvWapi,
                currenciesSettingsMap, walletDB);

        ExternalSignatureProviderProxy externalSignatureProviderProxy = new ExternalSignatureProviderProxy(
                getTrezorManager(),
                getKeepKeyManager(),
                getLedgerManager()
        );

        // Create and return wallet manager

        walletManager.setIsNetworkConnected(Utils.isConnected(context));
        walletManager.setWalletListener(new SyncEventsListener());

        // notify the walletManager about the current selected account
        walletManager.startSynchronization(getLastSelectedAccountId());

        NetworkParameters networkParameters = environment.getNetwork();
        PublicPrivateKeyStore publicPrivateKeyStore = new PublicPrivateKeyStore(secureKeyValueStore);

        SqliteColuManagerBacking coluBacking = new SqliteColuManagerBacking(context, networkParameters);

        SecureKeyValueStore coluSecureKeyValueStore = new SecureKeyValueStore(coluBacking, new AndroidRandomSource());

        SSLSocketFactory socketFactory = new DelegatingSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault()) {
            @Override
            protected SSLSocket configureSocket(SSLSocket socket) throws IOException {
                TrafficStats.tagSocket(socket);
                return socket;
            }
        };

        AccountEventManager accountEventManager = new AccountEventManager(walletManager);
        walletManager.add(new BitcoinHDModule(backing, secureKeyValueStore, networkParameters, _wapi, (BTCSettings) currenciesSettingsMap.get(BitcoinHDModule.ID), getMetadataStorage(),
                externalSignatureProviderProxy, migrationProgressTracker, accountEventManager));

        BitcoinSingleAddressModule saModule = new BitcoinSingleAddressModule(backing, publicPrivateKeyStore,
                networkParameters, _wapi, (BTCSettings) currenciesSettingsMap.get(BitcoinSingleAddressModule.ID), walletManager, getMetadataStorage(),
                migrationProgressTracker, accountEventManager);

        walletManager.add(saModule);

        ColuClient coluClient = new ColuClient(networkParameters, BuildConfig.ColoredCoinsApiURLs, BuildConfig.ColuBlockExplorerApiURLs, socketFactory);
        walletManager.add(new ColuModule(networkParameters, new PublicPrivateKeyStore(coluSecureKeyValueStore)
                , new ColuApiImpl(coluClient), _wapi, coluBacking, accountListener, getMetadataStorage(), saModule));

        AccountContextsBacking genericBacking = new AccountContextsBacking(db);
        EthBacking ethBacking = new EthBacking(db, genericBacking);
        EthBlockchainService ethBlockchainService = new EthBlockchainService(configuration.getBlockBookEndpoints());
        configuration.addEthServerListChangedListener(ethBlockchainService);
        EthereumModule ethereumModule = new EthereumModule(secureKeyValueStore, ethBacking, walletDB,
                ethBlockchainService, networkParameters, getMetadataStorage(), accountListener);
        walletManager.add(ethereumModule);

        walletManager.add(new ERC20Module(secureKeyValueStore, new ERC20Backing(db, genericBacking), walletDB,
                ethBlockchainService, networkParameters, getMetadataStorage(), accountListener, ethereumModule));

        FioModule fioModule = new FioModule(configuration, new AbiFioSerializationProviderWrapper(), new FioApiEndpoints(configuration.getFioApiEndpoints()), new FioHistoryEndpoints(configuration.getFioHistoryEndpoints()),
                secureKeyValueStore, new FioBacking(db, genericBacking), walletDB, networkParameters, getMetadataStorage(),
                new FioKeyManager(new MasterSeedManager(secureKeyValueStore)), accountListener, walletManager, configuration.getFioTpid());
        walletManager.add(fioModule);

        BitcoinVaultHDBacking bitcoinVaultBacking = new BitcoinVaultHDBacking(db, genericBacking);
        walletManager.add(new BitcoinVaultHDModule(bitcoinVaultBacking, secureKeyValueStore,
                environment.getBTCVNetwork(),
                walletDB, btcvWapi, (BTCSettings) currenciesSettingsMap.get(BitcoinVaultHDModule.ID),
                getMetadataStorage(), accountListener));

        walletManager.add(new InvestmentModule(getMetadataStorage()));
        walletManager.init();
        walletManager.startSynchronization(SyncMode.FULL_SYNC_ALL_ACCOUNTS);
        return walletManager;
    }

    @Nonnull
    public List<String> getCryptocurrenciesSorted() {
        List<String> cryptocurrencies = getWalletManager(false).getCryptocurrenciesNames();
        Collections.sort(cryptocurrencies, String::compareToIgnoreCase);
        return cryptocurrencies;
    }

    public List<Logs> getLastLogsDesc(long limit) {
        return loggerDB.getLogsQueries().selectWithLimit(limit).executeAsList();
    }

    public List<Logs> getLogsAsc() {
        return loggerDB.getLogsQueries().select().executeAsList();
    }

    private class AccountEventManager implements AbstractBtcAccount.EventHandler {
        private WalletManager walletManager;

        AccountEventManager(WalletManager walletManager) {
            this.walletManager = walletManager;
        }

        @Override
        public void onEvent(UUID accountId, WalletManager.Event event) {
            _eventTranslator.onAccountEvent(walletManager, accountId, event);
        }
    }

    /**
     * Create a Wallet Manager instance for temporary accounts just backed by in-memory persistence
     *
     * @param environment the Mycelium environment
     * @return a new in memory backed wallet manager instance
     */
    private WalletManager createTempWalletManager(MbwEnvironment environment) {
        // Create in-memory account backing
        BtcWalletManagerBacking backing = new InMemoryBtcWalletManagerBacking();

        // Create secure storage instance
        SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing, new AndroidRandomSource());

        // Create and return wallet manager
        WalletManager walletManager = new WalletManager(environment.getNetwork(), _wapi, btcvWapi, currenciesSettingsMap, db);
        walletManager.setIsNetworkConnected(Utils.isConnected(_applicationContext));
        walletManager.setWalletListener(new SyncEventsListener());

        NetworkParameters networkParameters = environment.getNetwork();
        PublicPrivateKeyStore publicPrivateKeyStore = new PublicPrivateKeyStore(secureKeyValueStore);

        AccountEventManager accountEventManager = new AccountEventManager(walletManager);
        walletManager.add(new BitcoinHDModule(backing, secureKeyValueStore, networkParameters, _wapi,
                (BTCSettings) currenciesSettingsMap.get(BitcoinHDModule.ID), getMetadataStorage()
                , null, null, accountEventManager));
        walletManager.add(new BitcoinSingleAddressModule(backing, publicPrivateKeyStore, networkParameters, _wapi,
                (BTCSettings) currenciesSettingsMap.get(BitcoinSingleAddressModule.ID), walletManager, getMetadataStorage(), null, accountEventManager));

        Backing<EthAccountContext> genericBacking = new InMemoryAccountContextsBacking<>();
        EthBlockchainService ethBlockchainService = new EthBlockchainService(configuration.getBlockBookEndpoints());
        configuration.addEthServerListChangedListener(ethBlockchainService);
        EthereumModule ethModule = new EthereumModule(secureKeyValueStore, genericBacking, db,
                ethBlockchainService, networkParameters, getMetadataStorage(), accountListener);
        walletManager.add(ethModule);

        Backing<FioAccountContext> fioGenericBacking = new InMemoryAccountContextsBacking<>();
        FioModule fioModule = new FioModule(configuration, new AbiFioSerializationProviderWrapper(), new FioApiEndpoints(configuration.getFioApiEndpoints()), new FioHistoryEndpoints(configuration.getFioHistoryEndpoints()),
                secureKeyValueStore, fioGenericBacking, db, networkParameters, getMetadataStorage(),
                new FioKeyManager(new MasterSeedManager(secureKeyValueStore)), accountListener, walletManager, configuration.getFioTpid());
        walletManager.add(fioModule);
        walletManager.disableTransactionHistorySynchronization();
        return walletManager;
    }

    class SyncEventsListener implements WalletListener {
        @Override
        public void syncStarted() {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new SyncStarted());
                }
            });
        }

        @Override
        public void syncStopped() {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new SyncStopped());
                }
            });
        }

        @Override
        public void accountSyncStopped(@NotNull WalletAccount<?> walletAccount) {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    _eventBus.post(new AccountSyncStopped(walletAccount));
                }
            });
        }
    }

    @Synchronized
    private LoadingProgressTracker getMigrationProgressTracker() {
        if (migrationProgressTracker == null) {
            migrationProgressTracker = new LoadingProgressTracker(_applicationContext);
        }
        return migrationProgressTracker;
    }

    public AssetInfo getFiatCurrency(AssetInfo coinType) {
        return _currencySwitcher.getCurrentFiatCurrency(coinType);
    }

    public boolean isAppInForeground() {
        return appInForeground;
    }

    public void setAppInForeground(boolean appInForeground) {
        this.appInForeground = appInForeground;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public void setActivityCount(int activityCount) {
        this.activityCount = activityCount;
    }

    public boolean hasFiatCurrency() {
        return !getCurrencyList().isEmpty();
    }

    private SharedPreferences getPreferences() {
        return _applicationContext.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
    }

    public List<AssetInfo> getCurrencyList() {
        return _currencySwitcher.getCurrencyList();
    }

    public void setCurrencyList(Set<AssetInfo> currencies) {
        // let the exchange-rate manager fetch all currencies, that we might need
        _exchangeRateManager.setCurrencyList(currencies);

        // but tell the currency-switcher only to switch over the user selected currencies
        _currencySwitcher.setCurrencyList(currencies);

        Set<String> data = new HashSet<>();
        for (AssetInfo currency : currencies) {
            data.add(currency.getSymbol());
        }
        getEditor().putStringSet(Constants.SELECTED_CURRENCIES, data).apply();
    }

    public AssetInfo getNextCurrency(boolean includeBitcoin) {
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
        return pinLockdownDuration.get() <= 0;
    }

    public Pin getPin() {
        return _pin;
    }

    public void showClearPinDialog(final Activity activity, final Runnable afterDialogClosed) {
        this.runPinProtectedFunction(activity, new ClearPinDialog(activity, true), new Runnable() {
            @Override
            public void run() {
                MbwManager.this.savePin(Pin.CLEAR_PIN);
                MbwManager.this.setTwoFactorEnabled(false);
                new Toaster(_applicationContext).toast(R.string.pin_cleared, false);
                if (afterDialogClosed != null) {
                    afterDialogClosed.run();
                }
            }
        });
    }

    public void showSetPinDialog(final Activity activity, final Runnable afterDialogClosed) {
        // Must make a backup before setting PIN
        if (this.getMetadataStorage().getMasterSeedBackupState() != MetadataStorage.BackupState.VERIFIED) {
            Utils.showSimpleMessageDialog(activity, R.string.pin_backup_first, afterDialogClosed);
            return;
        }

        final NewPinDialog pinDialog = new NewPinDialog(activity, false);
        pinDialog.setOnPinValid(new PinDialog.OnPinEntered() {
            private String newPin = null;

            @Override
            public void pinEntered(PinDialog dialog, Pin pin) {
                if (newPin == null) {
                    newPin = pin.getPin();
                    dialog.setTitle(R.string.pin_confirm_pin);
                } else if (newPin.equals(pin.getPin())) {
                    MbwManager.this.savePin(pin);
                    new Toaster(activity).toast(R.string.pin_set, false);
                    dialog.dismiss();
                    if (afterDialogClosed != null) {
                        afterDialogClosed.run();
                    }
                } else {
                    new Toaster(activity).toast(R.string.pin_codes_dont_match, false);
                    MbwManager.this.vibrate();
                    dialog.dismiss();
                    if (afterDialogClosed != null) {
                        afterDialogClosed.run();
                    }
                }
            }
        });

        runPinProtectedFunction(activity, new Runnable() {
            @Override
            public void run() {
                pinDialog.show();
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
        getEditor().putString(Constants.PIN_SETTING, _pin.getPin())
                .putString(Constants.PIN_SETTING_RESETTABLE, pin.isResettable() ? "1" : "0").apply();
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
                    if (failedPinCount > 0) {
                        long millis = (long) (Math.pow(1.2, failedPinCount) * 10);
                        try {
                            Thread.sleep(millis);
                        } catch (InterruptedException ignored) {
                            new Toaster(activity).toast(R.string.avoid_get_to_pin_check, false);
                            vibrate();
                            pinDialog.dismiss();
                            Thread.currentThread().interrupt();
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
                                            MbwManager.this.showClearPinDialog(activity, null);
                                        }
                                    })

                                    .setMessage(activity.getString(R.string.wrong_pin_message))
                                    .show();
                        } else {
                            // This pin is not resettable, you are out of luck
                            new Toaster(activity).toast(R.string.pin_invalid_pin, false);
                            vibrate();
                            pinDialog.dismiss();
                        }
                    }
                }
            });
            pinDialog.setFingerprintCallback(new PinDialog.FingerprintCallback() {
                @Override
                public void onSuccess() {
                    fun.run();
                }
            });
            if (!activity.isFinishing()) {
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

    public MinerFee getMinerFee(String coinName) {
        if (_minerFee.get(coinName) == null) {
            _minerFee.put(coinName, MinerFee.NORMAL);
        }
        return _minerFee.get(coinName);
    }

    public void setMinerFee(String coinName, MinerFee minerFee) {
        _minerFee.put(coinName, minerFee);
        Gson gson = new GsonBuilder().create();
        getEditor().putString(Constants.MINER_FEE_SETTING, gson.toJson(_minerFee)).apply();
    }

    public void setBlockExplorer(String coinName, BlockExplorer blockExplorer) {
        _blockExplorerManager.getBEMByCurrency(coinName).setBlockExplorer(blockExplorer);
        Gson gson = new GsonBuilder().create();
        getEditor().putString(Constants.BLOCK_EXPLORERS, gson.toJson(_blockExplorerManager.getCurrentBlockExplorersMap())).apply();
    }

    public Denomination getDenomination(AssetInfo coinType) {
        return _currencySwitcher.getDenomination(coinType);
    }

    public void setDenomination(AssetInfo coinType, Denomination denomination) {
        _currencySwitcher.setDenomination(coinType, denomination);
        Gson gson = new GsonBuilder().create();
        Map<String, String> resultMap = new HashMap<>();
        for (Map.Entry<AssetInfo, Denomination> entry : _currencySwitcher.getDenominationMap().entrySet()) {
            resultMap.put(entry.getKey().getName(), entry.getValue().toString());
        }
        getEditor().putString(Constants.DENOMINATION_SETTING, gson.toJson(resultMap)).apply();
    }

    public String getBtcValueString(long satoshis) {
        return ValueExtensionsKt.toStringWithUnit(Utils.getBtcCoinType().value(satoshis), getDenomination(Utils.getBtcCoinType()));
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
            if (null != message && message.length() > 0) {
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

    public void setLanguage(String language) {
        this._language = language;
        getEditor().putString(Constants.LANGUAGE_SETTING, language).apply();
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

        try {
            _environment.getWapiEndpoints().setAllowedEndpointTypes(serverEndpointType);
        } catch (IOException e) {
        }
        try {
            _environment.getLtEndpoints().setAllowedEndpointTypes(serverEndpointType);
        } catch (IOException e) {
        }
        try {
            _wapi.getServerEndpoints().setAllowedEndpointTypes(serverEndpointType);
        } catch (IOException e) {
        }
        List<TcpEndpoint> endpoints = torMode == ServerEndpointType.Types.ONLY_TOR ?
                configuration.getElectrumTorEndpoints() : configuration.getElectrumEndpoints();
        _wapi.serverListChanged(endpoints);
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

    public MasterSeedManager getMasterSeedManager() {
        return masterSeedManager;
    }

    public UUID createOnTheFlyAccount(Address address) {
        UUID accountId;
        if (address instanceof BtcAddress) {
            accountId = _tempWalletManager.createAccounts(new AddressSingleConfig(
                    new BtcAddress(Utils.getBtcCoinType(), ((BtcAddress) address).getAddress()))).get(0);
        } else if (address instanceof EthAddress) {
            accountId = _tempWalletManager.createAccounts(new EthAddressConfig((EthAddress) address)).get(0);
        } else if (address instanceof FioAddress) {
            accountId = _tempWalletManager.createAccounts(new FIOAddressConfig((FioAddress) address)).get(0);
        } else {
            throw new IllegalArgumentException("Not implemented");
        }
        _tempWalletManager.getAccount(accountId).setAllowZeroConfSpending(true);
        _tempWalletManager.startSynchronization(accountId);
        return accountId;
    }

    public UUID createOnTheFlyAccount(InMemoryPrivateKey privateKey, CryptoCurrency coinType) {
        final Config config = coinType instanceof FIOToken
                ? new FIOPrivateKeyConfig(privateKey)
                : new PrivateSingleConfig(privateKey, AesKeyCipher.defaultKeyCipher());
        final UUID accountId = _tempWalletManager.createAccounts(config).get(0);
        _tempWalletManager.getAccount(accountId).setAllowZeroConfSpending(true);
        _tempWalletManager.startSynchronization(accountId);
        return accountId;
    }

    public void forgetColdStorageWalletManager() {
        createTempWalletManager();
    }

    public WalletAccount getSelectedAccount() {
        UUID uuid = getLastSelectedAccountId();

        // If nothing is selected, or selected is archived, pick the first one but not Investment Account
        if (uuid != null && _walletManager.hasAccount(uuid) && _walletManager.getAccount(uuid).isActive()) {
            return _walletManager.getAccount(uuid);
        } else if (uuid == null || !_walletManager.hasAccount(uuid) || _walletManager.getAccount(uuid).isArchived()) {
            List<WalletAccount<?>> btcAccounts = BitcoinHDModuleKt.getActiveHDAccounts(_walletManager);
            if (btcAccounts.isEmpty()) {
                uuid = _walletManager.getAllActiveAccounts().get(0).getId();
            } else {
                uuid = btcAccounts.get(0).getId();
            }
            setSelectedAccount(uuid);
        }

        return _walletManager.getAccount(uuid);
    }

    public Optional<UUID> getAccountId(Address address) {
        return getAccountId(address, null);
    }

    public Optional<UUID> getAccountId(Address address, AssetInfo coinType) {
        Optional<UUID> result = Optional.absent();
        for (UUID uuid : _walletManager.getAccountIds()) {
            WalletAccount account = checkNotNull(_walletManager.getAccount(uuid));
            if ((coinType == null || account.getCoinType().equals(coinType))
                    && account.isMineAddress(address)) {
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
        WalletAccount account = _walletManager.getAccount(uuid);
        Preconditions.checkState(account.isActive());
        getEditor().putString(SELECTED_ACCOUNT, uuid.toString()).apply();
        getEventBus().post(new SelectedAccountChanged(uuid));
        Address receivingAddress = account.getReceiveAddress();
        getEventBus().post(new ReceivingAddressChanged(receivingAddress));
        _walletManager.startSynchronization(account.getId());
    }

    public InMemoryPrivateKey obtainPrivateKeyForAccount(WalletAccount account, String website, KeyCipher cipher) {
        if (account instanceof SingleAddressAccount) {
            // For single address accounts we use the private key directly
            try {
                return account.getPrivateKey(cipher);
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                throw new RuntimeException();
            }
        } else if (account instanceof HDAccount && ((HDAccount) account).getAccountType() == HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
            // For BIP44 accounts we derive a private key from the BIP32 hierarchy
            try {
                Bip39.MasterSeed masterSeed = masterSeedManager.getMasterSeed(cipher);
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
            IdentityAccountKeyManager identity = masterSeedManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher());
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
        sitePrivateKeySeed = BitUtils.concatenate(accountPriv.getPrivateKeyBytes(), site.getBytes(StandardCharsets.UTF_8));
        // Hash the seed and create a new private key from that which uses compressed public keys
        byte[] sitePrivateKeyBytes = HashUtils.doubleSha256(sitePrivateKeySeed).getBytes();
        return new InMemoryPrivateKey(sitePrivateKeyBytes, true);
    }

    public UUID createAdditionalBip44AccountUninterruptedly(Config accountConfig) {
        UUID accountId = _walletManager.createAccountsUninterruptedly(accountConfig).get(0);
        //set default label for the created HD account
        WalletAccount account = _walletManager.getAccount(accountId);
        String defaultName = account.getLabel();
        MetadataStorage.INSTANCE.storeAccountLabel(accountId, defaultName);
        return accountId;
    }

    public List<UUID> createAdditionalBip44AccountsUninterruptedly(List<Config> accounts) {
        List<UUID> result = new ArrayList<>();
        for (Config accountConfig : accounts) {
            result.add(createAdditionalBip44AccountUninterruptedly(accountConfig));
        }
        return result;
    }

    /**
     * Checks if user has created first Bitcoin HD, Ethereum, FIO accounts out of mnemonic seed
     * and returns configs for the accounts that hasn't been created yet
     * @return list of configs for the accounts that still has to be created
     */
    public List<Config> checkMainAccountsCreated() {
        Map<CryptoCurrency, List<WalletAccount<?>>> accounts = getWalletManager(false).getMasterSeedDerivedAccounts();
        List<Config> needsToBeCreated = new ArrayList<>();
        if (accounts.get(Utils.getBtcCoinType()) == null) {
            needsToBeCreated.add(new AdditionalHDAccountConfig());
        }
        if (accounts.get(Utils.getEthCoinType()) == null) {
            needsToBeCreated.add(new EthereumMasterseedConfig());
        }
        if (accounts.get(Utils.getFIOCoinType()) == null) {
            needsToBeCreated.add(new FIOMasterseedConfig());
        }
        return needsToBeCreated;
    }

    public boolean isWalletPaired(ExternalService service) {
        return getMetadataStorage().isPairedService(service.getHost(getNetwork()));
    }

    public MetadataStorage getMetadataStorage() {
        return MetadataStorage.INSTANCE;
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

    public WapiClientElectrumX getWapi() {
        return _wapi;
    }

    public WapiClientElectrumX getBtcvWapi() {
        return btcvWapi;
    }

    public TorManager getTorManager() {
        return _torManager;
    }

    public LtApiClient getLtApi() {
        return _ltApi;
    }

    @Subscribe
    public void onSelectedCurrencyChanged(SelectedCurrencyChanged event) {
        String currentCurrenciesString = getCurrentCurrenciesString(_currencySwitcher.getCurrentCurrencyMap());
        String currentFiatString = getCurrentCurrenciesString(_currencySwitcher.getCurrentFiatCurrencyMap());
        getEditor().putString(Constants.CURRENT_CURRENCIES_SETTING, currentCurrenciesString)
                .putString(Constants.FIAT_CURRENCIES_SETTING, currentFiatString)
                .putString(Constants.TOTAL_FIAT_CURRENCY_SETTING, _currencySwitcher.getCurrentTotalCurrency().getName()).apply();
    }

    @Subscribe
    public void accountChanged(AccountChanged accountChanged) {
        // AccountChanged event is posted in both cases: when account is created and when account is deleted
        // AccountCreated only when account is created. Reacting only on AccountCreated could leave walletCurrencies list
        // in incorrect state if no accounts of a particular type left after delete event
        _currencySwitcher.setWalletCurrencies(_walletManager.getAssetTypes());
        for (AssetInfo asset : _walletManager.getAssetTypes()) {
            if (!_minerFee.containsKey(asset.getName())) {
                _minerFee.put(asset.getName(), MinerFee.NORMAL);
                Gson gson = new GsonBuilder().create();
                getEditor().putString(Constants.MINER_FEE_SETTING, gson.toJson(_minerFee)).apply();
            }
        }
    }

    @Nullable
    public RecordObtData obtDataRecordCache = null;

    @Subscribe
    public void onTransactionBroadcast(TransactionBroadcasted tbe) {
        if (tbe.getTxid() != null && obtDataRecordCache != null) {
            FioAccount fioAccount = getActiveFioAccount(_walletManager, obtDataRecordCache.getPayerFioAddress());
            new Thread(() -> {
                try {
                    fioAccount.recordObtData(
                            BigInteger.ZERO, // let the sdk figure it out
                            obtDataRecordCache.getPayerFioAddress(),
                            obtDataRecordCache.getPayeeFioAddress(),
                            obtDataRecordCache.getPayerTokenPublicAddress(),
                            obtDataRecordCache.getPayeeTokenPublicAddress(),
                            obtDataRecordCache.getAmount(),
                            obtDataRecordCache.getChainCode(),
                            obtDataRecordCache.getTokenCode(),
                            tbe.getTxid(),
                            obtDataRecordCache.getMemo());
                } catch (Exception e) {
                    // TODO: 10/8/20 Actually handle the failure to send the obt record.
                    logger.log(Level.WARNING, "Sending fio obt record failed!", e);
                    FioModule fioModule = (FioModule) getWalletManager(false).getModuleById(FioModule.ID);
                    if (e instanceof FIOError) {
                        fioModule.addFioServerLog(((FIOError) e).toJson());
                    }
                }
            }).start();
        }
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

    public void setPinRequiredOnStartup(boolean pinRequiredOnStartup) {
        getEditor().putBoolean(Constants.PIN_SETTING_REQUIRED_ON_STARTUP, pinRequiredOnStartup).apply();

        this._pinRequiredOnStartup = pinRequiredOnStartup;
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
                getWalletManager(false).startSynchronization(new SyncMode(address),
                        Collections.<WalletAccount<?>>singletonList(getSelectedAccount()));
            }
        }, SECONDS.toMillis(1), SECONDS.toMillis(5));
    }

    private void pinOkForOneS() {
        if (pinOkTimeoutHandle != null) {
            pinOkTimeoutHandle.cancel(true);
        }
        lastPinAgeOkay.set(true);
        pinOkTimeoutHandle = scheduler.schedule(new Runnable() {
            public void run() {
                lastPinAgeOkay.set(false);
            }
        }, 1, SECONDS);
    }

    public boolean isShowQueuedTransactionsRemovalAlert() {
        return showQueuedTransactionsRemovalAlert;
    }

    public void setShowQueuedTransactionsRemovalAlert(boolean showQueuedTransactionsRemovalAlert) {
        this.showQueuedTransactionsRemovalAlert = showQueuedTransactionsRemovalAlert;
    }

    public FeeProvider getFeeProvider(AssetInfo asset) {
        return _walletManager.getFeeEstimations().getProvider(asset);
    }

    public boolean isAccountCanBeDeleted(WalletAccount<?> walletAccount) {
        boolean isDerivedFromInternalMasterseed = walletAccount.isDerivedFromInternalMasterseed();
        boolean isBch = walletAccount instanceof SingleAddressBCHAccount || walletAccount instanceof Bip44BCHAccount;

        if (walletAccount instanceof EthAccount) {
            return !((EthAccount) walletAccount).hasHadActivity();
        } else if (walletAccount instanceof FioAccount) {
            return !((FioAccount) walletAccount).hasHadActivity() || !isDerivedFromInternalMasterseed;
        } else {
            return !isDerivedFromInternalMasterseed && !isBch;
        }
    }
}

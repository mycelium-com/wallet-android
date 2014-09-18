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
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.CoinUtil.Denomination;
import com.mrd.bitlib.util.HashUtils;
import com.mycelium.wallet.activity.modern.ExploreHelper;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.event.EventTranslator;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.persistence.TradeSessionDb;
import com.mycelium.wallet.wapi.SqliteWalletManagerBacking;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Bus;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class MbwManager {

   public static final String PROXY_HOST = "socksProxyHost";
   public static final String PROXY_PORT = "socksProxyPort";
   public static final String SELECTED_ACCOUNT = "selectedAccount";
   private static volatile MbwManager _instance = null;

   /**
    * The root index we use for generating authentication keys.
    * 0x80 makes the number negative == hardened key derivation
    * 0x424944 = "BID"
    */
   private static final int BIP32_ROOT_AUTHENTICATION_INDEX = 0x80424944;

   public static synchronized MbwManager getInstance(Context context) {
      if (_instance == null) {
         _instance = new MbwManager(context);
      }
      return _instance;
   }

   private final Bus _eventBus;
   private NetworkConnectionWatcher _connectionWatcher;
   private Context _applicationContext;
   private int _displayWidth;
   private int _displayHeight;
   private AndroidAsyncApi _asyncApi;
   private AddressBookManager _addressBookManager;
   private MetadataStorage _storage;
   private LocalTraderManager _localTraderManager;
   private final String _btcValueFormatString;
   private String _pin;
   private Denomination _bitcoinDenomination;
   private String _fiatCurrency;
   private boolean _enableContinuousFocus;
   private boolean _keyManagementLocked;
   private boolean _isBitidEnabled;
   private int _mainViewFragmentIndex;
   private MrdExport.V1.EncryptionParameters _cachedEncryptionParameters;
   private final MrdExport.V1.ScryptParameters _deviceScryptParameters;
   private MbwEnvironment _environment;
   private final ExploreHelper exploreHelper;
   private HttpErrorCollector _httpErrorCollector;
   private String _language;
   private final VersionManager _versionManager;
   private final ExchangeRateManager _exchangeRateManager;
   private final WalletManager _walletManager;
   private WalletManager _tempWalletManager;
   private final RandomSource _randomSource;
   private final EventTranslator _eventTranslator;

   private MbwManager(Context evilContext) {
      _applicationContext = Preconditions.checkNotNull(evilContext.getApplicationContext());
      _environment = MbwEnvironment.determineEnvironment(_applicationContext);
      String version = VersionManager.determineVersion(_applicationContext);

      _httpErrorCollector = HttpErrorCollector.registerInVM(_applicationContext, version, _environment.getMwsApi());

      if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
         // Disable HTTP keep-alive on systems predating Gingerbread
         System.setProperty("http.keepAlive", "false");
      }

      _eventBus = new Bus();
      _randomSource = new AndroidRandomSource();

      // Preferences
      SharedPreferences preferences = getPreferences();
      setProxy(preferences.getString(Constants.PROXY_SETTING, ""));
      // Initialize proxy early, to enable error reporting during startup..

      _connectionWatcher = new NetworkConnectionWatcher(_applicationContext);
      _asyncApi = new AndroidAsyncApi(_environment.getMwsApi(), _eventBus);

      _isBitidEnabled = _applicationContext.getResources().getBoolean(R.bool.bitid_enabled);

      // Local Trader
      TradeSessionDb tradeSessionDb = new TradeSessionDb(_applicationContext);
      _localTraderManager = new LocalTraderManager(_applicationContext, tradeSessionDb,
            _environment.getLocalTraderApi(), this);

      _btcValueFormatString = _applicationContext.getResources().getString(R.string.btc_value_string);

      _pin = preferences.getString(Constants.PIN_SETTING, "");
      _fiatCurrency = preferences.getString(Constants.FIAT_CURRENCY_SETTING, Constants.DEFAULT_CURRENCY);
      _bitcoinDenomination = Denomination.fromString(preferences.getString(Constants.BITCOIN_DENOMINATION_SETTING,
            Denomination.mBTC.toString()));
      _enableContinuousFocus = preferences.getBoolean(Constants.ENABLE_CONTINUOUS_FOCUS_SETTING, false);
      _keyManagementLocked = preferences.getBoolean(Constants.KEY_MANAGEMENT_LOCKED_SETTING, false);
      _mainViewFragmentIndex = preferences.getInt(Constants.MAIN_VIEW_FRAGMENT_INDEX_SETTING, 0);

      // Get the display metrics of this device
      DisplayMetrics dm = new DisplayMetrics();
      WindowManager windowManager = (WindowManager) _applicationContext.getSystemService(Context.WINDOW_SERVICE);
      windowManager.getDefaultDisplay().getMetrics(dm);
      _displayWidth = dm.widthPixels;
      _displayHeight = dm.heightPixels;

      _addressBookManager = new AddressBookManager(_applicationContext);
      _storage = new MetadataStorage(_applicationContext);
      exploreHelper = new ExploreHelper();
      _language = preferences.getString(Constants.LANGUAGE_SETTING, Locale.getDefault().getLanguage());
      _versionManager = new VersionManager(_applicationContext, _language, _asyncApi, version);
      _exchangeRateManager = new ExchangeRateManager(_applicationContext, _environment.getWapi(), this);


      // Check the device MemoryClass and set the scrypt-parameters for the PDF backup
      ActivityManager am = (ActivityManager) _applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
      int memoryClass = am.getMemoryClass();

      _deviceScryptParameters = memoryClass > 20
            ? MrdExport.V1.ScryptParameters.DEFAULT_PARAMS
            : MrdExport.V1.ScryptParameters.LOW_MEM_PARAMS;

      _walletManager = createWalletManager(_applicationContext, _environment);
      _eventTranslator = new EventTranslator(new Handler(), _eventBus);
      _walletManager.addObserver(_eventTranslator);
      _exchangeRateManager.subscribe(_eventTranslator);
      migrateOldKeys();

      //for managing temp accounts created through scanning
      _tempWalletManager = createTempWalletManager(_applicationContext, _environment);
      _tempWalletManager.addObserver(_eventTranslator);
   }

   private void migrateOldKeys() {
      // We only migrate old keys if we don't have any accounts yet - otherwise, migration has already taken place
      if (!_walletManager.getAccountIds().isEmpty()) return;

      // Get the local trader address, may be null
      Address localTraderAddress = _localTraderManager.getLocalTraderAddress();
      if (localTraderAddress == null) {
         _localTraderManager.unsetLocalTraderAccount();
      }

      // Migrate all existing records to accounts
      List<Record> records = loadClassicRecords();
      for (Record record : records) {

         // Create an account from this record
         UUID account;
         if (record.hasPrivateKey()) {
            try {
               account = _walletManager.createSingleAddressAccount(record.key, AesKeyCipher.defaultKeyCipher());
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
               throw new RuntimeException(invalidKeyCipher);
            }
         } else {
            account = _walletManager.createSingleAddressAccount(record.address);
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

      migrateAddressAndAccountLabelsToMetadataStorage();
   }

   private List<Record> loadClassicRecords() {
      SharedPreferences prefs = _applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE);
      List<Record> recordList = new LinkedList<Record>();

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

   private void migrateAddressAndAccountLabelsToMetadataStorage() {
      List<AddressBookManager.Entry> entries = _addressBookManager.getEntries();
      for (AddressBookManager.Entry entry : entries) {

         Address address = entry.getAddress();
         String label = entry.getName();
         //check whether this is actually an addressbook entry, or the name of an account
         UUID accountid = SingleAddressAccount.calculateId(address);
         if (_walletManager.getAccountIds().contains(accountid)) {
            //its one of our accounts, so we name it
            _storage.storeAccountLabel(accountid, label);
         } else {
            //we just put it into the addressbook
            _storage.storeAddressLabel(address, label);
         }

      }
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
      WalletManagerBacking backing = new SqliteWalletManagerBacking(context);

      // Create persisted secure storage instance
      SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing,
            new AndroidRandomSource());

      // Create and return wallet manager
      return new WalletManager(secureKeyValueStore, backing, environment.getNetwork(),
            environment.getWapi());
   }

   /**
    * Create a Wallet Manager instance for temporary accounts just backed by in-memory persistence
    *
    * @param context     the application context
    * @param environment the Mycelium environment
    * @return a new in memory backed wallet manager instance
    */
   private WalletManager createTempWalletManager(final Context context, MbwEnvironment environment) {

      // Create in-memory account backing
      WalletManagerBacking backing = new InMemoryWalletManagerBacking();

      // Create secure storage instance
      SecureKeyValueStore secureKeyValueStore = new SecureKeyValueStore(backing, new AndroidRandomSource());

      // Create and return wallet manager
      WalletManager walletManager = new WalletManager(secureKeyValueStore, backing, environment.getNetwork(),
            environment.getWapi());
      walletManager.disableTransactionHistorySynchronization();
      return walletManager;
   }

   public int getDisplayWidth() {
      return _displayWidth;
   }

   public NetworkConnectionWatcher getNetworkConnectionWatcher() {
      return _connectionWatcher;
   }

   public int getDisplayHeight() {
      return _displayHeight;
   }

   public String getFiatCurrency() {
      return _fiatCurrency;
   }

   public void setFiatCurrency(String currency) {
      _fiatCurrency = currency;
      SharedPreferences.Editor editor = getEditor();
      editor.putString(Constants.FIAT_CURRENCY_SETTING, _fiatCurrency);
      editor.commit();
   }

   private SharedPreferences getPreferences() {
      return _applicationContext.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
   }

   private SharedPreferences.Editor getEditor() {
      return getPreferences().edit();
   }

   public AndroidAsyncApi getAsyncApi() {
      return _asyncApi;
   }

   public LocalTraderManager getLocalTraderManager() {
      return _localTraderManager;
   }

   public ExchangeRateManager getExchangeRateManager() {
      return _exchangeRateManager;
   }

   public AddressBookManager getAddressBookManager() {
      return _addressBookManager;
   }

   public boolean isPinProtected() {
      return getPin().length() > 0;
   }

   private String getPin() {
      return _pin;
   }

   public void setPin(String pin) {
      _pin = pin;
      getEditor().putString(Constants.PIN_SETTING, _pin).commit();
   }

   public void runPinProtectedFunction(final Context context, final Runnable fun) {
      if (isPinProtected()) {
         Dialog d = new PinDialog(context, true, new PinDialog.OnPinEntered() {

            @Override
            public void pinEntered(PinDialog dialog, String pin) {
               if (pin.equals(getPin())) {
                  dialog.dismiss();
                  fun.run();
               } else {
                  Toast.makeText(context, R.string.pin_invalid_pin, Toast.LENGTH_LONG).show();
                  vibrate(500);
                  dialog.dismiss();
               }
            }
         });
         d.setTitle(R.string.pin_enter_pin);
         d.show();
      } else {
         fun.run();
      }
   }

   public void vibrate() {
      vibrate(500);
   }

   public void vibrate(int milliseconds) {
      Vibrator v = (Vibrator) _applicationContext.getSystemService(Context.VIBRATOR_SERVICE);
      if (v != null) {
         v.vibrate(milliseconds);
      }
   }

   public CoinUtil.Denomination getBitcoinDenomination() {
      return _bitcoinDenomination;
   }

   public void setBitcoinDenomination(CoinUtil.Denomination denomination) {
      _bitcoinDenomination = denomination;
      getEditor().putString(Constants.BITCOIN_DENOMINATION_SETTING, _bitcoinDenomination.toString()).commit();
   }

   public String getBtcValueString(long satoshis) {
      return getBtcValueString(satoshis, _btcValueFormatString);
   }

   private String getBtcValueString(long satoshis, String formatString) {
      Denomination d = getBitcoinDenomination();
      String valueString = CoinUtil.valueString(satoshis, d, true);
      return String.format(formatString, valueString, d.getUnicodeName());
   }

   public boolean isKeyManagementLocked() {
      return _keyManagementLocked;
   }

   public void setKeyManagementLocked(boolean locked) {
      _keyManagementLocked = locked;
      getEditor().putBoolean(Constants.KEY_MANAGEMENT_LOCKED_SETTING, _keyManagementLocked).commit();
   }

   public boolean getContinuousFocus() {
      return _enableContinuousFocus;
   }

   public void setContinuousFocus(boolean enableContinuousFocus) {
      _enableContinuousFocus = enableContinuousFocus;
      getEditor().putBoolean(Constants.ENABLE_CONTINUOUS_FOCUS_SETTING, _enableContinuousFocus).commit();
   }


   public void setProxy(String proxy) {
      getEditor().putString(Constants.PROXY_SETTING, proxy).commit();
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

   public int getMainViewFragmentIndex() {
      return _mainViewFragmentIndex;
   }

   public void setMainViewFragmentIndex(int index) {
      _mainViewFragmentIndex = index;
      getEditor().putInt(Constants.MAIN_VIEW_FRAGMENT_INDEX_SETTING, _mainViewFragmentIndex).commit();
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

   public Bus getEventBus() {
      return _eventBus;
   }

   /**
    * Get the Bitcoin network parameters that the wallet operates on
    */
   public NetworkParameters getNetwork() {
      return _environment.getNetwork();
   }

   /**
    * Get the brand of the wallet. This allows us to behave differently
    * depending on the brand of the wallet.
    */
   public String getBrand() {
      return _environment.getBrand();
   }

   public boolean isBitidEnabled() {
      return _isBitidEnabled;
   }

   public ExploreHelper getExploreHelper() {
      return exploreHelper;
   }

   public void reportIgnoredException(Throwable e) {
      if (_httpErrorCollector != null) {
         RuntimeException msg = new RuntimeException("We caught an exception that we chose to ignore.\n", e);
         _httpErrorCollector.reportErrorToServer(msg);
      }
   }

   public String getLanguage() {
      return _language;
   }

   public void setLanguage(String _language) {
      this._language = _language;
      SharedPreferences.Editor editor = getEditor();
      editor.putString(Constants.LANGUAGE_SETTING, _language);
      editor.commit();
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
      _tempWalletManager.startSynchronization();
      return accountId;
   }

   public UUID createOnTheFlyAccount(InMemoryPrivateKey privateKey) {
      UUID accountId;
      try {
         accountId = _tempWalletManager.createSingleAddressAccount(privateKey, AesKeyCipher.defaultKeyCipher());
      } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
         throw new RuntimeException(invalidKeyCipher);
      }
      return accountId;
   }

   public void forgetColdStorageWalletManager() {
      _tempWalletManager = createTempWalletManager(_applicationContext, _environment);
      _tempWalletManager.addObserver(_eventTranslator);
   }

   public WalletAccount getSelectedAccount() {
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
      // If nothing is selected, or selected is archived, pick the first one
      if (uuid == null || !_walletManager.hasAccount(uuid) || _walletManager.getAccount(uuid).isArchived()) {
         uuid = _walletManager.getActiveAccounts().get(0).getId();
         setSelectedAccount(uuid);
      }

      return _walletManager.getAccount(uuid);
   }

   public void setSelectedAccount(UUID uuid) {
      WalletAccount account = _walletManager.getAccount(uuid);
      Preconditions.checkState(account.isActive());
      getEditor().putString(SELECTED_ACCOUNT, uuid.toString()).commit();
      getEventBus().post(new SelectedAccountChanged(uuid));
      getEventBus().post(new ReceivingAddressChanged(account.getReceivingAddress()));
   }

   public InMemoryPrivateKey obtainPrivateKeyForAccount(WalletAccount account, String website, KeyCipher cipher) {
      if (account instanceof SingleAddressAccount) {
         // For single address accounts we use the private key directly
         try {
            return ((SingleAddressAccount) account).getPrivateKey(cipher);
         } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException();
         }
      } else if (account instanceof Bip44Account) {
         // For BIP44 accounts we derive a private key from the BIP32 hierarchy
         try {
            Bip39.MasterSeed masterSeed = _walletManager.getMasterSeed(cipher);
            int accountIndex = ((Bip44Account) account).getAccountIndex();
            return createBip32WebsitePrivateKey(masterSeed.getBip32Seed(), accountIndex, website);
         } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException();
         }
      } else {
         throw new RuntimeException("Invalid account type");
      }
   }

   private InMemoryPrivateKey createBip32WebsitePrivateKey(byte[] masterSeed, int accountIndex, String site) {
      // Create BIP32 root node
      HdKeyNode rootNode = HdKeyNode.fromSeed(masterSeed);
      // Create bit id node
      HdKeyNode bidNode = rootNode.createChildNode(BIP32_ROOT_AUTHENTICATION_INDEX);
      // Create the private key for the specified account
      InMemoryPrivateKey accountPriv = bidNode.createChildPrivateKey(accountIndex);
      // Concatenate the private key bytes with the site name
      byte[] sitePrivateKeySeed = new byte[0];
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

   public MetadataStorage getMetadataStorage() {
      return _storage;
   }

   public RandomSource getRandomSource() {
      return _randomSource;
   }

}

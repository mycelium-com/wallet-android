/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet;

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.lib.FeeEstimation;
import com.mycelium.wapi.api.response.MinerFeeEstimationResponse;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.bip44.*;
import com.mycelium.wapi.wallet.single.PublicPrivateKeyStore;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;

import java.util.*;

/**
 * Allows you to manage a wallet that contains multiple HD accounts and
 * 'classic' single address accounts.
 */
//TODO we might optimize away full TX history for cold storage spending

public class WalletManager {

   private static final byte[] MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990");
   // maximum age where we say a fetched fee estimation is valid
   private static final long MAX_AGE_FEE_ESTIMATION = 2 * 60 * 60 * 1000;

   //if there are more external account types, expand this to a list
   private Optional<? extends WalletAccount> extraAccount;

   public void setExtraAccount(Optional<? extends WalletAccount> extraAccount) {
      this.extraAccount = extraAccount;
      if (extraAccount.isPresent()) {
         WalletAccount key = extraAccount.get();
         if (!_allAccounts.containsKey(key.getId())) {
            _allAccounts.put(key.getId(), key);
         }
      }
   }

   /**
    * Implement this interface to get a callback when the wallet manager changes
    * state or when some event occurs
    */
   public interface Observer {

      /**
       * Callback which occurs when the state of a wallet manager changes while
       * the wallet is synchronizing
       *
       * @param wallet the wallet manager instance
       * @param state  the new state of the wallet manager
       */
      void onWalletStateChanged(WalletManager wallet, State state);

      /**
       * Callback telling that an event occurred while synchronizing
       *
       * @param wallet    the wallet manager
       * @param accountId the ID of the account causing the event
       * @param events    the event that occurred
       */
      void onAccountEvent(WalletManager wallet, UUID accountId, Event events);
   }

   public enum State {
      /**
       * The wallet manager is synchronizing
       */
      SYNCHRONIZING,
      /**
       * The wallet manager is ready
       */
      READY
   }

   public enum Event {
      /**
       * There is currently no connection to the block chain. This is probably
       * due to network issues, or the Mycelium servers are down (unlikely).
       */
      SERVER_CONNECTION_ERROR,
      /**
       * The wallet broadcasted a transaction which was accepted by the network
       */
      BROADCASTED_TRANSACTION_ACCEPTED,
      /**
       * The wallet broadcasted a transaction which was rejected by the network.
       * This is an rare event which could happen if you double spend yourself,
       * or you spent an unconfirmed change which was subject to a malleability
       * attack
       */
      BROADCASTED_TRANSACTION_DENIED,
      /**
       * The balance of the account changed
       */
      BALANCE_CHANGED,
      /**
       * The transaction history of the account changed
       */
      TRANSACTION_HISTORY_CHANGED,
      /**
       * The receiving address of an account has been updated
       */
      RECEIVING_ADDRESS_CHANGED
   }

   private final SecureKeyValueStore _secureKeyValueStore;
   private WalletManagerBacking _backing;
   private final Map<UUID, WalletAccount> _allAccounts;
   private final List<Bip44Account> _bip44Accounts;
   private final Collection<Observer> _observers;
   private State _state;
   private Thread _synchronizationThread;
   private AccountEventManager _accountEventManager;
   private NetworkParameters _network;
   private Wapi _wapi;
   private WapiLogger _logger;
   private boolean _synchronizeTransactionHistory;
   private final ExternalSignatureProviderProxy _signatureProviders;
   private IdentityAccountKeyManager _identityAccountKeyManager;

   private FeeEstimation _lastFeeEstimations = FeeEstimation.DEFAULT;

   public AccountScanManager accountScanManager;


   /**
    * Create a new wallet manager instance
    *
    * @param backing the backing to use for storing everything related to wallet accounts
    * @param network the network used
    * @param wapi    the Wapi instance to use
    */
   public WalletManager(SecureKeyValueStore secureKeyValueStore, WalletManagerBacking backing,
                        NetworkParameters network, Wapi wapi, ExternalSignatureProviderProxy signatureProviders) {
      _secureKeyValueStore = secureKeyValueStore;
      _backing = backing;
      _network = network;
      _wapi = wapi;
      _signatureProviders = signatureProviders;
      _logger = _wapi.getLogger();
      _allAccounts = Maps.newHashMap();
      _bip44Accounts = new ArrayList<Bip44Account>();
      _state = State.READY;
      _accountEventManager = new AccountEventManager();
      _observers = new LinkedList<Observer>();
      _synchronizeTransactionHistory = true;
      loadAccounts();
   }

   /**
    * Get the current state
    *
    * @return the current state
    */
   public State getState() {
      return _state;
   }

   /**
    * Add an observer that gets callbacks when the wallet manager state changes
    * or account events occur.
    *
    * @param observer the observer to add
    */
   public void addObserver(Observer observer) {
      synchronized (_observers) {
         _observers.add(observer);
      }
   }

   /**
    * Remove an observer
    *
    * @param observer the observer to remove
    */
   public void removeObserver(Observer observer) {
      synchronized (_observers) {
         _observers.remove(observer);
      }
   }

   /**
    * Create a new read-only account using a single address
    *
    * @param address the address to use
    * @return the ID of the new account
    */
   public UUID createSingleAddressAccount(Address address) {
      UUID id = SingleAddressAccount.calculateId(address);
      synchronized (_allAccounts) {
         if (_allAccounts.containsKey(id)) {
            return id;
         }
         _backing.beginTransaction();
         try {
            SingleAddressAccountContext context = new SingleAddressAccountContext(id, address, false, 0);
            _backing.createSingleAddressAccountContext(context);
            SingleAddressAccountBacking accountBacking = _backing.getSingleAddressAccountBacking(context.getId());
            Preconditions.checkNotNull(accountBacking);
            PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
            SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, _wapi);
            context.persist(accountBacking);
            _backing.setTransactionSuccessful();
            addAccount(account);
         } finally {
            _backing.endTransaction();
         }
      }
      return id;
   }


   /**
    * Create a new Bp44 account using a accountRoot or xPrivKey (unrelated to the Masterseed)
    *
    * @param hdKeyNode the xPub/xPriv to use
    * @return the ID of the new account
    */
   public UUID createUnrelatedBip44Account(HdKeyNode hdKeyNode) {
      final int accountIndex = 0;  // use any index for this account, as we dont know and we dont care
      final Bip44AccountKeyManager keyManager;

      // get a subKeyStorage, to ensure that the data for this key does not get mixed up
      // with other derived or imported keys.
      SecureSubKeyValueStore secureStorage = getSecureStorage().createNewSubKeyStore();

      if (hdKeyNode.isPrivateHdKeyNode()){
         try {
            keyManager = Bip44AccountKeyManager.createFromAccountRoot(hdKeyNode, _network, accountIndex, secureStorage, AesKeyCipher.defaultKeyCipher());
         } catch (InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
         }
      }else {
         keyManager = Bip44PubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode, _network, accountIndex, secureStorage);
      }

      final UUID id = keyManager.getAccountId();

      synchronized (_allAccounts){
         // check if it already exists
         if (_allAccounts.containsKey(id)) {
            return id;
         }
         _backing.beginTransaction();
         try {

            // Generate the context for the account
            Bip44AccountContext context;
            if (hdKeyNode.isPrivateHdKeyNode()) {
               context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false,
                     Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PRIV, secureStorage.getSubId());
            } else {
               context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false,
                     Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB, secureStorage.getSubId());
            }
            _backing.createBip44AccountContext(context);

            // Get the backing for the new account
            Bip44AccountBacking accountBacking = _backing.getBip44AccountBacking(context.getId());
            Preconditions.checkNotNull(accountBacking);

            // Create actual account
            Bip44Account account;
            if (hdKeyNode.isPrivateHdKeyNode()) {
               account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);
            } else {
               account = new Bip44PubOnlyAccount(context, keyManager, _network, accountBacking, _wapi);
            }

            // Finally persist context and add account
            context.persist(accountBacking);
            _backing.setTransactionSuccessful();
            addAccount(account);
            _bip44Accounts.add(account);
            return id;
         } finally {
            _backing.endTransaction();
         }

      }
   }


   public UUID createExternalSignatureAccount(HdKeyNode hdKeyNode, ExternalSignatureProvider externalSignatureProvider, int accountIndex) {
      SecureSubKeyValueStore newSubKeyStore = getSecureStorage().createNewSubKeyStore();
      Bip44AccountKeyManager keyManager = Bip44PubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode, _network, accountIndex, newSubKeyStore);
      final UUID id = keyManager.getAccountId();

      synchronized (_allAccounts) {
         _backing.beginTransaction();
         try {

            // check if it already exists
            if (_allAccounts.containsKey(id)) {
               return id;
            }

            // Generate the context for the account
            Bip44AccountContext context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false,
                  externalSignatureProvider.getBIP44AccountType(), newSubKeyStore.getSubId());
            _backing.createBip44AccountContext(context);

            // Get the backing for the new account
            Bip44AccountBacking accountBacking = _backing.getBip44AccountBacking(context.getId());
            Preconditions.checkNotNull(accountBacking);

            // Create actual account
            Bip44Account account = new Bip44AccountExternalSignature(context, keyManager, _network, accountBacking, _wapi, externalSignatureProvider);

            // Finally persist context and add account
            context.persist(accountBacking);
            _backing.setTransactionSuccessful();
            addAccount(account);
            _bip44Accounts.add(account);
            return account.getId();
         } finally {
            _backing.endTransaction();
         }
      }
   }

   /**
    * Create a new account using a single private key and address
    *
    * @param privateKey key the private key to use
    * @param cipher     the cipher used to encrypt the private key. Must be the same
    *                   cipher as the one used by the secure storage instance
    * @return the ID of the new account
    * @throws InvalidKeyCipher
    */
   public UUID createSingleAddressAccount(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
      PublicKey publicKey = privateKey.getPublicKey();
      Address address = publicKey.toAddress(_network);
      PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
      store.setPrivateKey(address, privateKey, cipher);
      return createSingleAddressAccount(address);
   }

   /**
    * Delete an account that uses a single address
    * <p/>
    * This method cannot be used for deleting Masterseed-based HD accounts.
    *
    * @param id the ID of the account to delete.
    */
   public void deleteUnrelatedAccount(UUID id, KeyCipher cipher) throws InvalidKeyCipher {
      synchronized (_allAccounts) {
         WalletAccount account = _allAccounts.get(id);
         if (account instanceof AbstractAccount) {
            AbstractAccount abstractAccount = (AbstractAccount) account;
            abstractAccount.setEventHandler(null);
         }
         if (account instanceof SingleAddressAccount) {
            SingleAddressAccount singleAddressAccount = (SingleAddressAccount) account;
            singleAddressAccount.forgetPrivateKey(cipher);
            _backing.deleteSingleAddressAccountContext(id);
            _allAccounts.remove(id);
         } else if (account instanceof Bip44Account) {
            Bip44Account hdAccount = (Bip44Account) account;
            if (hdAccount.isDerivedFromInternalMasterseed()) {
               throw new RuntimeException("cant delete masterseed based accounts");
            }
            hdAccount.clearBacking();
            _bip44Accounts.remove(hdAccount);
            _backing.deleteBip44AccountContext(id);
            _allAccounts.remove(id);
         }
      }
   }

   /**
    * Call this method to disable transaction history synchronization for single address accounts.
    * <p/>
    * This is useful if the wallet manager is used for cold storage spending where the transaction history is
    * uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
    * address has been used a lot.
    */
   public void disableTransactionHistorySynchronization() {
      _synchronizeTransactionHistory = false;
   }

   /**
    * Get the IDs of the accounts managed by the wallet manager
    *
    * @return the IDs of the accounts managed by the wallet manager
    */
   public List<UUID> getAccountIds() {
      List<UUID> list = new ArrayList<UUID>(_allAccounts.size());
      for (WalletAccount account : _allAccounts.values()) {
         list.add(account.getId());
      }
      return list;
   }

   /**
    * Get the active accounts managed by the wallet manager, excluding on-the-fly-accounts
    *
    * @return the active accounts managed by the wallet manager
    */
   public List<WalletAccount> getActiveAccounts() {
      return filterAndConvert(Predicates.not(IS_ARCHIVE));
   }

   /**
    * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getActiveMasterseedAccounts() {
      return filterAndConvert(Predicates.and(MAIN_SEED_HD_ACCOUNT, Predicates.not(IS_ARCHIVE)));
   }

   /**
    * Get the active none-HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getActiveOtherAccounts() {
      return filterAndConvert(Predicates.not(Predicates.or(MAIN_SEED_HD_ACCOUNT, IS_ARCHIVE)));
   }


   /**
    * Get archived accounts managed by the wallet manager
    *
    * @return the archived accounts managed by the wallet manager
    */
   public List<WalletAccount> getArchivedAccounts() {
      return filterAndConvert(IS_ARCHIVE);
   }

   /**
    * Get accounts that can spend and are active
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getSpendingAccounts() {
      return filterAndConvert(ACTIVE_CAN_SPEND);
   }

   /**
    * Get accounts that can spend and have a positive balance
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getSpendingAccountsWithBalance() {
      return filterAndConvert(Predicates.and(ACTIVE_CAN_SPEND, HAS_BALANCE));
   }

   private List<WalletAccount> filterAndConvert(Predicate<Map.Entry<UUID, WalletAccount>> filter) {
      Set<UUID> uuids = Maps.filterEntries(_allAccounts, filter).keySet();
      return Lists.transform(Lists.newArrayList(uuids), key2Account);
   }

   /**
    * Check whether the wallet manager has a particular account
    *
    * @param id the account to look for
    * @return true if the wallet manager has an account with the specified ID
    */
   public boolean hasAccount(UUID id) {
      return _allAccounts.containsKey(id);
   }

   /**
    * Get a wallet account
    *
    * @param id the ID of the account to get
    * @return a wallet account
    */
   public WalletAccount getAccount(UUID id) {
      WalletAccount normalAccount = _allAccounts.get(id);
      return Preconditions.checkNotNull(normalAccount);
   }

   /**
    * Make the wallet manager synchronize all its active accounts.
    * <p/>
    * Synchronization occurs in the background. To get feedback register an
    * observer.
    */
   public void startSynchronization() {
      if (_synchronizationThread != null) {
         // Already running
         return;
      }
      // Launch synchronizer thread
      Synchronizer synchronizer = new Synchronizer();
      _synchronizationThread = new Thread(synchronizer);
      _synchronizationThread.setDaemon(true);
      _synchronizationThread.setName(synchronizer.getClass().getSimpleName());
      _synchronizationThread.start();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int Bip44Accounts = 0;
      int simpleAccounts = 0;
      for (UUID id : getAccountIds()) {
         if (_allAccounts.get(id) instanceof Bip44Account) {
            Bip44Accounts++;
         } else if (_allAccounts.get(id) instanceof SingleAddressAccount) {
            simpleAccounts++;
         }
      }
      sb.append("Accounts: ").append(_allAccounts.size()).append(" Active: ").append(getActiveAccounts().size())
            .append(" HD: ").append(Bip44Accounts).append(" Simple:").append(simpleAccounts);
      return sb.toString();
   }

   /**
    * Determine whether this address is managed by an account of the wallet
    *
    * @param address the address to query for
    * @return if any account in the wallet manager has the address
    */
   public boolean isMyAddress(Address address) {
      return getAccountByAddress(address).isPresent();
   }


   /**
    * Get the account associated with an address if any
    *
    * @param address the address to query for
    * @return the account UUID if found.
    */
   public synchronized Optional<UUID> getAccountByAddress(Address address) {
      for (WalletAccount account : _allAccounts.values()) {
         if (account.isMine(address)) {
            return Optional.of(account.getId());
         }
      }
      return Optional.absent();
   }

   /**
    * Determine whether any account in the wallet manager has the private key for the specified address
    *
    * @param address the address to query for
    * @return true if any account in the wallet manager has the private key for the specified address
    */
   public synchronized boolean hasPrivateKeyForAddress(Address address) {
      for (WalletAccount account : _allAccounts.values()) {
         if (account.isMine(address) && account.canSpend()) {
            return true;
         }
      }
      return false;
   }

   private void setStateAndNotify(State state) {
      _state = state;
      synchronized (_observers) {
         for (Observer o : _observers) {
            o.onWalletStateChanged(this, _state);
         }
      }
   }

   private void loadAccounts() {
      if (hasBip32MasterSeed()) {
         loadBip44Accounts();
      }
      // Load all single address accounts
      loadSingleAddressAccounts();
   }

   private void loadBip44Accounts() {
      _logger.logInfo("Loading BIP44 accounts");
      List<Bip44AccountContext> contexts = _backing.loadBip44AccountContexts();
      for (Bip44AccountContext context : contexts) {
         Bip44AccountKeyManager keyManager;
         Bip44Account account;

         Bip44AccountBacking accountBacking = _backing.getBip44AccountBacking(context.getId());
         Preconditions.checkNotNull(accountBacking);

         if (context.getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
            // Normal account - derived from masterseed
            keyManager = new Bip44AccountKeyManager(context.getAccountIndex(), _network, _secureKeyValueStore);
            account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);
         } else if (context.getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB) {
            // Imported xPub-based account
            SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
            keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStore);
            account = new Bip44PubOnlyAccount(context, keyManager, _network, accountBacking, _wapi);
         } else if (context.getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PRIV) {
            // Imported xPriv-based account
            SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
            keyManager = new Bip44AccountKeyManager(context.getAccountIndex(), _network, subKeyStore);
            account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);
         } else if (context.getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR) {
            SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
            keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStore);
            account = new Bip44AccountExternalSignature(
                  context,
                  keyManager,
                  _network,
                  accountBacking,
                  _wapi,
                  _signatureProviders.get(Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR)
            );
         } else if (context.getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER){
             SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
             keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStore);
             account = new Bip44AccountExternalSignature(
                   context,
                   keyManager,
                   _network,
                   accountBacking,
                   _wapi,
                   _signatureProviders.get(Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER)
             );
         } else {
            throw new IllegalArgumentException("Unknown account type " + context.getAccountType());
         }

         addAccount(account);
         _bip44Accounts.add(account);
      }
   }

   private void loadSingleAddressAccounts() {
      _logger.logInfo("Loading single address accounts");
      List<SingleAddressAccountContext> contexts = _backing.loadSingleAddressAccountContexts();
      for (SingleAddressAccountContext context : contexts) {
         PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
         SingleAddressAccountBacking accountBacking = _backing.getSingleAddressAccountBacking(context.getId());
         Preconditions.checkNotNull(accountBacking);
         SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, _wapi);
         addAccount(account);
      }
   }

   public void addAccount(WalletAccount account) {
      synchronized (_allAccounts) {
         if (account instanceof AbstractAccount) {
            AbstractAccount abstractAccount = (AbstractAccount) account;
            abstractAccount.setEventHandler(_accountEventManager);
         }
         _allAccounts.put(account.getId(), account);
         _logger.logInfo("Account Added: " + account.getId());
      }
   }


   private class Synchronizer implements Runnable {

      @Override
      public void run() {
         try {
            setStateAndNotify(State.SYNCHRONIZING);
            synchronized (_allAccounts) {
               fetchFeeEstimation();

               // If we have any lingering outgoing transactions broadcast them
               // now
               if (!broadcastOutgoingTransactions()) {
                  return;
               }

               // Synchronize every account with the blockchain
               if (!synchronize()) {
                  return;
               }
            }
         } finally {
            _synchronizationThread = null;
            setStateAndNotify(State.READY);
         }
      }

      private boolean fetchFeeEstimation(){
         WapiResponse<MinerFeeEstimationResponse> minerFeeEstimations = _wapi.getMinerFeeEstimations();
         if (minerFeeEstimations != null && minerFeeEstimations.getErrorCode() == Wapi.ERROR_CODE_SUCCESS) {
            try {
               _lastFeeEstimations = minerFeeEstimations.getResult().feeEstimation;
               return true;
            } catch (WapiException e) {
               return false;
            }
         }
         return false;
      }

      private boolean broadcastOutgoingTransactions() {
         for (WalletAccount account : _allAccounts.values()) {
            if (account.isArchived()) {
               continue;
            }
            if (!account.broadcastOutgoingTransactions()) {
               // We failed to broadcast due to API error, we will have to try
               // again later
               return false;
            }
         }
         return true;
      }

      private boolean synchronize() {
         for (WalletAccount account : _allAccounts.values()) {
            if (account.isArchived()) {
               continue;
            }
            if (!account.synchronize(_synchronizeTransactionHistory)) {
               // We failed to broadcast due to API error, we will have to try
               // again later
               return false;
            }
         }
         return true;
      }

   }

   private class AccountEventManager implements AbstractAccount.EventHandler {
      @Override
      public void onEvent(UUID accountId, Event event) {
         synchronized (_observers) {
            for (Observer o : _observers) {
               o.onAccountEvent(WalletManager.this, accountId, event);
            }
         }
      }
   }

   /**
    * Determine whether the wallet manager has a master seed configured
    *
    * @return true if a master seed has been configured for this wallet manager
    */
   public boolean hasBip32MasterSeed() {
      return _secureKeyValueStore.hasCiphertextValue(MASTER_SEED_ID);
   }

   /**
    * Get the master seed in plain text
    *
    * @param cipher the cipher used to decrypt the master seed
    * @return the master seed in plain text
    * @throws InvalidKeyCipher if the cipher is invalid
    */
   public Bip39.MasterSeed getMasterSeed(KeyCipher cipher) throws InvalidKeyCipher {

      byte[] binaryMasterSeed = _secureKeyValueStore.getEncryptedValue(MASTER_SEED_ID, cipher);
      Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false);
      if (!masterSeed.isPresent()) {
         throw new RuntimeException();
      }
      return masterSeed.get();
   }

   /**
    * Configure the BIP32 master seed of this wallet manager
    *
    * @param masterSeed the master seed to use.
    * @param cipher     the cipher used to encrypt the master seed. Must be the same
    *                   cipher as the one used by the secure storage instance
    * @throws InvalidKeyCipher if the cipher is invalid
    */
   public void configureBip32MasterSeed(Bip39.MasterSeed masterSeed, KeyCipher cipher) throws InvalidKeyCipher {
      if (hasBip32MasterSeed()) {
         throw new RuntimeException("HD key store already loaded");
      }
      _secureKeyValueStore.encryptAndStoreValue(MASTER_SEED_ID, masterSeed.toBytes(false), cipher);
   }

   private static final Predicate<Map.Entry<UUID, WalletAccount>> IS_ARCHIVE = new Predicate<Map.Entry<UUID, WalletAccount>>() {
      @Override
      public boolean apply(Map.Entry<UUID, WalletAccount> input) {
         return input.getValue().isArchived();
      }
   };

   private static final Predicate<Map.Entry<UUID, WalletAccount>> ACTIVE_CAN_SPEND = new Predicate<Map.Entry<UUID, WalletAccount>>() {
      @Override
      public boolean apply(Map.Entry<UUID, WalletAccount> input) {
         return input.getValue().isActive() && input.getValue().canSpend();
      }
   };

   private static final Predicate<Map.Entry<UUID, WalletAccount>> MAIN_SEED_HD_ACCOUNT = new Predicate<Map.Entry<UUID, WalletAccount>>() {
      @Override
      public boolean apply(Map.Entry<UUID, WalletAccount> input) {
         // todo: if relevant also check if this account is derived from the main-masterseed
         return input.getValue() instanceof Bip44Account &&
               input.getValue().isDerivedFromInternalMasterseed();
      }
   };

   private static final Predicate<Map.Entry<UUID, WalletAccount>> HAS_BALANCE = new Predicate<Map.Entry<UUID, WalletAccount>>() {
      @Override
      public boolean apply(Map.Entry<UUID, WalletAccount> input) {
         return input.getValue().getBalance().getSpendableBalance() > 0;
      }
   };

   private final Function<UUID, WalletAccount> key2Account = new Function<UUID, WalletAccount>() {
      @Override
      public WalletAccount apply(UUID input) {
         return getAccount(input);
      }
   };

   public boolean canCreateAdditionalBip44Account() {
      if (!hasBip32MasterSeed()) {
         // No master seed
         return false;
      }
      if (getNextBip44Index() == 0) {
         // First account not created
         return true;
      }
      // We can add an additional account if the last account had activity
      Bip44Account last = _bip44Accounts.get(_bip44Accounts.size() - 1);
      return last.hasHadActivity();
   }

   public boolean removeUnusedBip44Account() {
      Bip44Account last = _bip44Accounts.get(_bip44Accounts.size() - 1);
      //we do not remove used accounts
      if (last.hasHadActivity()) return false;
      //if its unused, we can remove it from the manager
      synchronized (_allAccounts) {
         _bip44Accounts.remove(last);
         _allAccounts.remove(last.getId());
         _backing.deleteBip44AccountContext(last.getId());
         return true;
      }
   }

   private int getNextBip44Index() {
      return _bip44Accounts.size();
   }


   public UUID createAdditionalBip44Account(KeyCipher cipher) throws InvalidKeyCipher {
      if (!canCreateAdditionalBip44Account()) {
         throw new RuntimeException("Unable to create additional HD account");
      }

      // Get the master seed
      Bip39.MasterSeed mastrSeed = getMasterSeed(cipher);

      // Generate the root private key
      HdKeyNode root = HdKeyNode.fromSeed(mastrSeed.getBip32Seed());

      synchronized (_allAccounts) {
         // Determine the next BIP44 account index
         int accountIndex = getNextBip44Index();

         _backing.beginTransaction();
         try {
            // Create the base keys for the account
            Bip44AccountKeyManager keyManager = Bip44AccountKeyManager.createNew(root, _network, accountIndex, _secureKeyValueStore, cipher);

            // Generate the context for the account
            Bip44AccountContext context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false);
            _backing.createBip44AccountContext(context);

            // Get the backing for the new account
            Bip44AccountBacking accountBacking = _backing.getBip44AccountBacking(context.getId());
            Preconditions.checkNotNull(accountBacking);


            // Create actual account
            Bip44Account account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);

            // Finally persist context and add account
            context.persist(accountBacking);
            _backing.setTransactionSuccessful();
            addAccount(account);
            _bip44Accounts.add(account);
            return account.getId();
         } finally {
            _backing.endTransaction();
         }
      }
   }

   public SecureKeyValueStore getSecureStorage(){
      return _secureKeyValueStore;
   }

   public IdentityAccountKeyManager getIdentityAccountKeyManager(KeyCipher cipher) throws InvalidKeyCipher {
      if (null != _identityAccountKeyManager) {
         return _identityAccountKeyManager;
      }
      if (!hasBip32MasterSeed()) throw new RuntimeException("accessed identity account with no master seed configured");
      HdKeyNode rootNode = HdKeyNode.fromSeed(getMasterSeed(cipher).getBip32Seed());
      _identityAccountKeyManager = IdentityAccountKeyManager.createNew(rootNode, _secureKeyValueStore, cipher);
      return _identityAccountKeyManager;
   }

   public FeeEstimation getLastFeeEstimations() {
      if (_lastFeeEstimations != null && (new Date().getTime() - _lastFeeEstimations.getValidFor().getTime()) < MAX_AGE_FEE_ESTIMATION ) {
         return _lastFeeEstimations;
      } else {
         return FeeEstimation.DEFAULT;
      }
   }

}

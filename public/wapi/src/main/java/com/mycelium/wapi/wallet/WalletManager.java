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
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;
import static com.google.common.base.Predicates.not;
import static com.mycelium.wapi.wallet.bip44.Bip44AccountContext.*;

import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
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
   private final Set<AccountProvider> _extraAccountProviders = new HashSet<AccountProvider>();
   private final Set<String> _extraAccountsCurrencies = new HashSet<String>();
   private final HashMap<UUID, WalletAccount> _extraAccounts = new HashMap<UUID, WalletAccount>();

   public void addExtraAccounts(AccountProvider accountProvider) {
      _extraAccountProviders.add(accountProvider);
      refreshExtraAccounts();
   }

   public void refreshExtraAccounts() {
      _extraAccounts.clear();
      _extraAccountsCurrencies.clear();
      for (AccountProvider accounts : _extraAccountProviders) {
         for (WalletAccount account : accounts.getAccounts().values()) {
            if (!_extraAccounts.containsKey(account.getId())) {
               _extraAccounts.put(account.getId(), account);
               _extraAccountsCurrencies.add(account.getAccountDefaultCurrency());
            }
         }
      }
   }

   public Set<String> getAllActiveFiatCurrencies(){
      return ImmutableSet.copyOf(_extraAccountsCurrencies);
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

      /*
       * A fast sync (only a limited subset of all addresses) is running
       */
      FAST_SYNC,

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
   private final Map<UUID, WalletAccount> _walletAccounts;
   private final List<Bip44Account> _bip44Accounts;
   private final Collection<Observer> _observers;
   private State _state;
   private Thread _synchronizationThread;
   private AccountEventManager _accountEventManager;
   private NetworkParameters _network;
   private Wapi _wapi;
   private WapiLogger _logger;
   private final ExternalSignatureProviderProxy _signatureProviders;
   private IdentityAccountKeyManager _identityAccountKeyManager;
   private volatile UUID _activeAccountId;

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
      _walletAccounts = Maps.newHashMap();
      _bip44Accounts = new ArrayList<Bip44Account>();
      _state = State.READY;
      _accountEventManager = new AccountEventManager();
      _observers = new LinkedList<Observer>();
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
      synchronized (_walletAccounts) {
         if (_walletAccounts.containsKey(id)) {
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
    * Create a new Bp44 account using an accountRoot or xPrivKey (unrelated to the Masterseed)
    *
    * @param hdKeyNode the xPub/xPriv to use
    * @return the ID of the new account
    */
   public UUID createUnrelatedBip44Account(HdKeyNode hdKeyNode) {
      final int accountIndex = 0;  // use any index for this account, as we don't know and we don't care
      final Bip44AccountKeyManager keyManager;

      // get a subKeyStorage, to ensure that the data for this key does not get mixed up
      // with other derived or imported keys.
      SecureSubKeyValueStore secureStorage = getSecureStorage().createNewSubKeyStore();

      if (hdKeyNode.isPrivateHdKeyNode()) {
         try {
            keyManager = Bip44AccountKeyManager.createFromAccountRoot(hdKeyNode, _network, accountIndex, secureStorage, AesKeyCipher.defaultKeyCipher());
         } catch (InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
         }
      } else {
         keyManager = Bip44PubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode, _network, accountIndex, secureStorage);
      }

      final UUID id = keyManager.getAccountId();

      synchronized (_walletAccounts){
         // check if it already exists
         if (_walletAccounts.containsKey(id)) {
            return id;
         }
         _backing.beginTransaction();
         try {

            // Generate the context for the account
            Bip44AccountContext context;
            if (hdKeyNode.isPrivateHdKeyNode()) {
               context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false,
                     ACCOUNT_TYPE_UNRELATED_X_PRIV, secureStorage.getSubId());
            } else {
               context = new Bip44AccountContext(keyManager.getAccountId(), accountIndex, false,
                     ACCOUNT_TYPE_UNRELATED_X_PUB, secureStorage.getSubId());
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

      synchronized (_walletAccounts) {
         _backing.beginTransaction();
         try {

            // check if it already exists
            if (_walletAccounts.containsKey(id)) {
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
    * <p>
    * This method cannot be used for deleting Masterseed-based HD accounts.
    *
    * @param id the ID of the account to delete.
    */
   public void deleteUnrelatedAccount(UUID id, KeyCipher cipher) throws InvalidKeyCipher {
      synchronized (_walletAccounts) {
         WalletAccount account = _walletAccounts.get(id);
         if (account instanceof AbstractAccount) {
            AbstractAccount abstractAccount = (AbstractAccount) account;
            abstractAccount.setEventHandler(null);
         }
         if (account instanceof SingleAddressAccount) {
            SingleAddressAccount singleAddressAccount = (SingleAddressAccount) account;
            singleAddressAccount.forgetPrivateKey(cipher);
            _backing.deleteSingleAddressAccountContext(id);
            _walletAccounts.remove(id);
         } else if (account instanceof Bip44Account) {
            Bip44Account hdAccount = (Bip44Account) account;
            if (hdAccount.isDerivedFromInternalMasterseed()) {
               throw new RuntimeException("cant delete masterseed based accounts");
            }
            hdAccount.clearBacking();
            _bip44Accounts.remove(hdAccount);
            _backing.deleteBip44AccountContext(id);
            _walletAccounts.remove(id);
         }
      }
   }

   /**
    * Call this method to disable transaction history synchronization for single address accounts.
    * <p>
    * This is useful if the wallet manager is used for cold storage spending where the transaction history is
    * uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
    * address has been used a lot.
    */
   public void disableTransactionHistorySynchronization() {
   }

   /**
    * Get the IDs of the accounts managed by the wallet manager
    *
    * @return the IDs of the accounts managed by the wallet manager
    */
   public List<UUID> getAccountIds() {
      List<UUID> list = new ArrayList<UUID>(_walletAccounts.size() + _extraAccounts.size());
      for (WalletAccount account : getAllAccounts()) {
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
      return filterAndConvert(not(IS_ARCHIVE));
   }

   /**
    * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getActiveMasterseedAccounts() {
      return filterAndConvert(and(MAIN_SEED_HD_ACCOUNT, not(IS_ARCHIVE)));
   }

   /**
    * Get the active none-HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
    *
    * @return the list of accounts
    */
   public List<WalletAccount> getActiveOtherAccounts() {
      return filterAndConvert(not(or(MAIN_SEED_HD_ACCOUNT, IS_ARCHIVE)));
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
      return filterAndConvert(and(ACTIVE_CAN_SPEND, HAS_BALANCE));
   }

   private List<WalletAccount> filterAndConvert(Predicate<WalletAccount> filter) {
      return Lists.newArrayList(Iterables.filter(getAllAccounts(), filter));
   }

   /**
    * Check whether the wallet manager has a particular account
    *
    * @param id the account to look for
    * @return true if the wallet manager has an account with the specified ID
    */
   public boolean hasAccount(UUID id) {
      return _walletAccounts.containsKey(id) || _extraAccounts.containsKey(id);
   }

   /**
    * Get a wallet account
    *
    * @param id the ID of the account to get
    * @return a wallet account
    */
   public WalletAccount getAccount(UUID id) {
      WalletAccount normalAccount = _walletAccounts.get(id);
      if (normalAccount == null){
         normalAccount = _extraAccounts.get(id);
      }
      return Preconditions.checkNotNull(normalAccount);
   }

   /**
    * Make the wallet manager synchronize all its active accounts.
    * <p>
    * Synchronization occurs in the background. To get feedback register an
    * observer.
    */
   public void startSynchronization() {
      startSynchronization(SyncMode.NORMAL);
   }

   public void startSynchronization(SyncMode mode) {
      // Launch synchronizer thread
      Synchronizer synchronizer;
      if (hasAccount(_activeAccountId)) {
         SynchronizeAbleWalletAccount activeAccount = (SynchronizeAbleWalletAccount) getAccount(_activeAccountId);
         synchronizer = new Synchronizer(mode, activeAccount);
      } else {
         // we dont know the active account
         synchronizer = new Synchronizer(mode);
      }
      startSynchronizationThread(synchronizer);
   }

   public void startSynchronization(UUID receivingAcc) {
      // Launch synchronizer thread
      SynchronizeAbleWalletAccount activeAccount = (SynchronizeAbleWalletAccount) getAccount(receivingAcc);
      startSynchronizationThread(new Synchronizer(SyncMode.NORMAL, activeAccount));
   }

   /**
    * Make the wallet manager synchronize only a subset of some addresses of a specific account
    * <p>
    * Synchronization occurs in the background. To get feedback register an
    * observer.
    */
   /*
   public void startFastSynchronization(AbstractAccount forAccount, Collection<Address> addressesToWatch) {
      // Launch fastSynchronizer thread
      Synchronizer fastSynchronizer = new FastSynchronizer(forAccount, addressesToWatch);
      startSynchronizationThread(fastSynchronizer);
   }
   */
   private synchronized void startSynchronizationThread(Synchronizer synchronizer) {
      if (_synchronizationThread != null) {
         // Already running
         return;
      }
      // Launch fastSynchronizer thread
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
         if (_walletAccounts.get(id) instanceof Bip44Account) {
            Bip44Accounts++;
         } else if (_walletAccounts.get(id) instanceof SingleAddressAccount) {
            simpleAccounts++;
         }
      }
      sb.append("Accounts: ").append(_walletAccounts.size()).append(" Active: ").append(getActiveAccounts().size())
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
    * @return the first account UUID if found.
    */
   public synchronized Optional<UUID> getAccountByAddress(Address address) {
      for (WalletAccount account : getAllAccounts()) {
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
      // don't use getAccountByAddress here, as we might have the same address in an pub-only account and a normal account too
      for (WalletAccount account : getAllAccounts()) {
         if (account.canSpend() && account.isMine(address)) {
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

         switch (context.getAccountType()) {
            case ACCOUNT_TYPE_FROM_MASTERSEED:
               // Normal account - derived from masterseed
               keyManager = new Bip44AccountKeyManager(context.getAccountIndex(), _network, _secureKeyValueStore);
               account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);
               break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB:
               // Imported xPub-based account
               SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
               keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStore);
               account = new Bip44PubOnlyAccount(context, keyManager, _network, accountBacking, _wapi);
               break;
            case ACCOUNT_TYPE_UNRELATED_X_PRIV:
               // Imported xPriv-based account
               SecureKeyValueStore subKeyStoreXpriv = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
               keyManager = new Bip44AccountKeyManager(context.getAccountIndex(), _network, subKeyStoreXpriv);
               account = new Bip44Account(context, keyManager, _network, accountBacking, _wapi);
               break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR:
               SecureKeyValueStore subKeyStoreTrezor = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
               keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStoreTrezor);
               account = new Bip44AccountExternalSignature(
                     context,
                     keyManager,
                     _network,
                     accountBacking,
                     _wapi,
                     _signatureProviders.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR)
               );
               break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER:
               SecureKeyValueStore subKeyStoreLedger = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
               keyManager = new Bip44PubOnlyAccountKeyManager(context.getAccountIndex(), _network, subKeyStoreLedger);
               account = new Bip44AccountExternalSignature(
                     context,
                     keyManager,
                     _network,
                     accountBacking,
                     _wapi,
                     _signatureProviders.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER)
               );
               break;
            default:
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
      synchronized (_walletAccounts) {
         if (account instanceof AbstractAccount) {
            AbstractAccount abstractAccount = (AbstractAccount) account;
            abstractAccount.setEventHandler(_accountEventManager);
         }
         _walletAccounts.put(account.getId(), account);
         _logger.logInfo("Account Added: " + account.getId());
      }
   }

   private class Synchronizer implements Runnable {
      private final SyncMode syncMode;
      private final SynchronizeAbleWalletAccount currentAccount;

      private Synchronizer(SyncMode syncMode, SynchronizeAbleWalletAccount currentAccount) {
         this.syncMode = syncMode;
         this.currentAccount = currentAccount;
      }

      // use this constructor if you dont care about the current active account
      private Synchronizer(SyncMode syncMode) {
         this.syncMode = syncMode;
         // ensure to scan all accounts
         this.currentAccount = null;
      }

      @Override
      public void run() {
         try {
            setStateAndNotify(State.SYNCHRONIZING);
            synchronized (_walletAccounts) {
               if (!syncMode.ignoreMinerFeeFetch) {
                  // only fetch the fee estimations if the latest available fee is older than half of its max-age
                  if (_lastFeeEstimations != null) {
                     final long feeAge = _lastFeeEstimations.getValidFor().getTime() - new Date().getTime();
                     if (feeAge > MAX_AGE_FEE_ESTIMATION / 2){
                        fetchFeeEstimation();
                     }
                  } else {
                     fetchFeeEstimation();
                  }
               }

               // If we have any lingering outgoing transactions broadcast them now
               // this function goes over all accounts - it is reasonable to
               // exclude this from SyncMode.onlyActiveAccount behaviour
               if (!broadcastOutgoingTransactions()) {
                  return;
               }

               // Synchronize selected accounts with the blockchain
               if (!synchronize()) {
                  return;
               }
            }
         } finally {
            _synchronizationThread = null;
            setStateAndNotify(State.READY);
         }
      }


      private boolean fetchFeeEstimation() {
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
         for (WalletAccount account : getAllAccounts()) {
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
         if (syncMode.onlyActiveAccount) {
            if (currentAccount != null && !currentAccount.isArchived()) {
               return currentAccount.synchronize(syncMode);
            }
         } else {
            for (WalletAccount account : getAllAccounts()) {
               if (!account.isArchived()) {
                  if (!account.synchronize(syncMode)) {
                     // We failed to sync due to API error, we will have to try
                     // again later
                     return false;
                  }
               }
            }
         }
         return true;
      }
   }

   private Iterable<WalletAccount> getAllAccounts() {
      return Iterables.concat(_walletAccounts.values(), _extraAccounts.values());
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

   public void setActiveAccount(UUID accountId) {
      _activeAccountId = accountId;
      if (hasAccount(accountId)) {
         WalletAccount account = getAccount(_activeAccountId);
         if (account != null) {
            // this account might not be synchronized - start a background sync
            startSynchronization(SyncMode.NORMAL);
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

   private static final Predicate<WalletAccount> IS_ARCHIVE = new Predicate<WalletAccount>() {
      @Override
      public boolean apply(WalletAccount input) {
         return input.isArchived();
      }
   };

   private static final Predicate<WalletAccount> ACTIVE_CAN_SPEND = new Predicate<WalletAccount>() {
      @Override
      public boolean apply(WalletAccount input) {
         return input.isActive() && input.canSpend();
      }
   };

   private static final Predicate<WalletAccount> MAIN_SEED_HD_ACCOUNT = new Predicate<WalletAccount>() {
      @Override
      public boolean apply(WalletAccount input) {
         // todo: if relevant also check if this account is derived from the main-masterseed
         return input instanceof Bip44Account &&
               input.isDerivedFromInternalMasterseed();
      }
   };

   private static final Predicate<WalletAccount> HAS_BALANCE = new Predicate<WalletAccount>() {
      @Override
      public boolean apply(WalletAccount input) {
         return input.getBalance().getSpendableBalance() > 0;
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
      if (last.hasHadActivity()) {
         return false;
      }
      //if its unused, we can remove it from the manager
      synchronized (_walletAccounts) {
         _bip44Accounts.remove(last);
         _walletAccounts.remove(last.getId());
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
      Bip39.MasterSeed masterSeed = getMasterSeed(cipher);

      // Generate the root private key
      HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed());

      synchronized (_walletAccounts) {
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

   public SecureKeyValueStore getSecureStorage() {
      return _secureKeyValueStore;
   }

   public IdentityAccountKeyManager getIdentityAccountKeyManager(KeyCipher cipher) throws InvalidKeyCipher {
      if (null != _identityAccountKeyManager) {
         return _identityAccountKeyManager;
      }
      if (!hasBip32MasterSeed()) {
         throw new RuntimeException("accessed identity account with no master seed configured");
      }
      HdKeyNode rootNode = HdKeyNode.fromSeed(getMasterSeed(cipher).getBip32Seed());
      _identityAccountKeyManager = IdentityAccountKeyManager.createNew(rootNode, _secureKeyValueStore, cipher);
      return _identityAccountKeyManager;
   }

   public FeeEstimation getLastFeeEstimations() {
      if (_lastFeeEstimations != null && (new Date().getTime() - _lastFeeEstimations.getValidFor().getTime()) < MAX_AGE_FEE_ESTIMATION) {
         return _lastFeeEstimations;
      } else {
         return FeeEstimation.DEFAULT;
      }
   }
}

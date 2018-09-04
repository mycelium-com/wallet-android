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

package com.mycelium.wapi.wallet.btc.single;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.ScriptOutput;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse;
import com.mycelium.wapi.model.BalanceSatoshis;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.BtcTransaction;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;

import java.util.*;

public class SingleAddressAccount extends AbstractBtcAccount implements ExportableAccount {
   private SingleAddressAccountContext _context;
   private List<Address> _addressList;
   private volatile boolean _isSynchronizing;
   private PublicPrivateKeyStore _keyStore;
   private SingleAddressAccountBacking _backing;

   public SingleAddressAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore,
                               NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi) {
      super(backing, network, wapi);
      _backing = backing;
      type = WalletBtcAccount.Type.BTCSINGLEADDRESS;
      _context = context;
      _addressList = new ArrayList<>(3);
      _keyStore = keyStore;
      persistAddresses();
      _addressList.addAll(context.getAddresses().values());
      _cachedBalance = _context.isArchived() ? new BalanceSatoshis(0, 0, 0, 0, 0, 0, false, _allowZeroConfSpending) : calculateLocalBalance();
   }

   private void persistAddresses() {
      try {
         InMemoryPrivateKey privateKey = getPrivateKey(AesKeyCipher.defaultKeyCipher());
         if (privateKey != null) {
            Map<AddressType, Address> allPossibleAddresses = privateKey.getPublicKey().getAllSupportedAddresses(_network);
            if (allPossibleAddresses.size() != _context.getAddresses().size()) {
               for (Address address : allPossibleAddresses.values()) {
                  if (!address.equals(_context.getAddresses().get(address.getType()))) {
                     _keyStore.setPrivateKey(address, privateKey, AesKeyCipher.defaultKeyCipher());
                  }
               }
               _context.setAddresses(allPossibleAddresses);
               _context.persist(_backing);
            }
         }
      } catch (InvalidKeyCipher invalidKeyCipher) {
         _logger.logError(invalidKeyCipher.getMessage());
      }
   }

   public static UUID calculateId(Address address) {
      return addressToUUID(address);
   }

   @Override
   public synchronized void archiveAccount() {
      if (_context.isArchived()) {
         return;
      }
      clearInternalStateInt(true);
      _context.persistIfNecessary(_backing);
   }


   @Override
   public synchronized void activateAccount() {
      if (!_context.isArchived()) {
         return;
      }
      clearInternalStateInt(false);
      _context.persistIfNecessary(_backing);
   }

   @Override
   public void dropCachedData() {
      if (_context.isArchived()) {
         return;
      }
      clearInternalStateInt(false);
      _context.persistIfNecessary(_backing);
   }

   @Override
   public boolean isValidEncryptionKey(KeyCipher cipher) {
      return _keyStore.isValidEncryptionKey(cipher);
   }

   @Override
   public boolean isDerivedFromInternalMasterseed() {
      return false;
   }

   private void clearInternalStateInt(boolean isArchived) {
      _backing.clear();
      _context = new SingleAddressAccountContext(_context.getId(), _context.getAddresses(), isArchived, 0);
      _context.persist(_backing);
      _cachedBalance = null;
      if (isActive()) {
         _cachedBalance = calculateLocalBalance();
      }
   }

   @Override
   public synchronized boolean doSynchronization(SyncMode mode) {
      checkNotArchived();
      _isSynchronizing = true;
      syncTotalRetrievedTransactions = 0;
      try {

         if (synchronizeUnspentOutputs(_addressList) == -1) {
            return false;
         }

         // Monitor young transactions
         if (!monitorYoungTransactions()) {
            return false;
         }

         //lets see if there are any transactions we need to discover
         if (!mode.ignoreTransactionHistory) {
            if (!discoverTransactions()) {
               return false;
            }
         }

         // recalculate cached BalanceSatoshis
         updateLocalBalance();

         _context.persistIfNecessary(_backing);
         return true;
      } finally {
         _isSynchronizing = false;
         syncTotalRetrievedTransactions = 0;
      }

   }

   private boolean discoverTransactions() {
      // Get the latest transactions
      List<Sha256Hash> discovered;
      try {
         final QueryTransactionInventoryResponse result = _wapi.queryTransactionInventory(new QueryTransactionInventoryRequest(Wapi.VERSION, _addressList, 30))
                 .getResult();
         setBlockChainHeight(result.height);
         discovered = result.txIds;
      } catch (WapiException e) {
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         postEvent(Event.SERVER_CONNECTION_ERROR);
         return false;
      }

      // Figure out whether there are any transactions we need to fetch
      List<Sha256Hash> toFetch = new LinkedList<>();
      for (Sha256Hash id : discovered) {
         if (!_backing.hasTransaction(id)) {
            toFetch.add(id);
         }
      }

      // Fetch any missing transactions
      if (!toFetch.isEmpty()) {
         try {
            GetTransactionsResponse response;
            response = getTransactionsBatched(toFetch).getResult();
            handleNewExternalTransactions(response.transactions);
         } catch (WapiException e) {
            _logger.logError("Server connection failed with error code: " + e.errorCode, e);
            postEvent(Event.SERVER_CONNECTION_ERROR);
            return false;
         }
      }
      return true;
   }

   @Override
   public Optional<Address> getReceivingAddress() {
      //removed checkNotArchived, cause we wont to know the address for archived acc
      //to display them as archived accounts in "Accounts" tab
      return Optional.of(getAddress());
   }

   @Override
   public boolean canSpend() {
      return _keyStore.hasPrivateKey(getAddress());
   }

   @Override
   public boolean isMine(Address address) {
      return _addressList.contains(address);
   }

   @Override
   public int getBlockChainHeight() {
      checkNotArchived();
      return _context.getBlockHeight();
   }

   @Override
   protected void setBlockChainHeight(int blockHeight) {
      checkNotArchived();
      _context.setBlockHeight(blockHeight);
   }

   @Override
   protected void persistContextIfNecessary() {
      _context.persistIfNecessary(_backing);
   }

   @Override
   public boolean isArchived() {
      // public method that needs no synchronization
      return _context.isArchived();
   }

   @Override
   public boolean isActive() {
      // public method that needs no synchronization
      return !isArchived();
   }

   @Override
   protected void onNewTransaction(Transaction t) {
      // Nothing to do for this account type
   }

   @Override
   public boolean isOwnInternalAddress(Address address) {
      return isMine(address);
   }

   @Override
   public boolean isOwnExternalAddress(Address address) {
      return isMine(address);
   }

   @Override
   public UUID getId() {
      return _context.getId();
   }

   @Override
   protected Address getChangeAddress() {
      return getAddress();
   }

   @Override
   protected InMemoryPrivateKey getPrivateKey(PublicKey publicKey, KeyCipher cipher) throws InvalidKeyCipher {
      if (getPublicKey().equals(publicKey)) {
         return getPrivateKey(cipher);
      }
      return null;
   }

   @Override
   protected InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher) throws InvalidKeyCipher {
      if (_addressList.contains(address)) {
         return getPrivateKey(cipher);
      } else {
         return null;
      }
   }

   @Override
   protected PublicKey getPublicKeyForAddress(Address address) {
      if (_addressList.contains(address)) {
         return getPublicKey();
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Simple ID: ").append(getId());
      if (isArchived()) {
         sb.append(" Archived");
      } else {
         if (_cachedBalance == null) {
            sb.append(" Balance: not known");
         } else {
            sb.append(" Balance: ").append(_cachedBalance);
         }
         Optional<Address> receivingAddress = getReceivingAddress();
         sb.append(" Receiving Address: ").append(receivingAddress.isPresent() ? receivingAddress.get().toString() : "");
         sb.append(" Spendable Outputs: ").append(getSpendableOutputs(0).size());
      }
      return sb.toString();
   }

   @Override
   public boolean isSynchronizing() {
      return _isSynchronizing;
   }

   public void forgetPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      _keyStore.forgetPrivateKey(getAddress(), cipher);
   }

   public InMemoryPrivateKey getPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      return _keyStore.getPrivateKey(getAddress(), cipher);
   }

   public void setPrivateKey(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
      persistAddresses();
      _keyStore.setPrivateKey(getAddress(), privateKey, cipher);
   }

   public PublicKey getPublicKey() {
      return _keyStore.getPublicKey(getAddress());
   }

   /**
    * @return default address
    */
   public Address getAddress() {
      if (getAddress(AddressType.P2SH_P2WPKH) != null) {
         return getAddress(AddressType.P2SH_P2WPKH);
      } else {
         return _context.getAddresses().values().iterator().next();
      }
   }

   public Address getAddress(AddressType type) {
      return _context.getAddresses().get(type);
   }

   @Override
   public Data getExportData(KeyCipher cipher) {
      Optional<String> privKey = Optional.absent();

      if (canSpend()) {
         try {
            privKey = Optional.of(_keyStore.getPrivateKey(getAddress(), cipher).getBase58EncodedPrivateKey(getNetwork()));
         } catch (InvalidKeyCipher ignore) {
         }
      }

      Optional<String> pubKey = Optional.of(getAddress().toString());
      return new Data(privKey, pubKey);
   }

   @Override
   protected boolean doDiscoveryForAddresses(List<Address> lookAhead) throws WapiException {
      // not needed for SingleAddressAccount
      return true;
   }

   @Override
   public void completeAndSignTx(SendRequest<BtcTransaction> request) throws WalletAccountException {
   }

   @Override
   public void completeTransaction(SendRequest<BtcTransaction> request) throws WalletAccountException {
   }

   @Override
   public void signTransaction(SendRequest<BtcTransaction> request) throws WalletAccountException {
   }

   @Override
   public void broadcastTx(BtcTransaction tx) throws TransactionBroadcastException {
   }

}

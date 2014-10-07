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

package com.mycelium.wapi.wallet.single;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.WalletManager.Event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class SingleAddressAccount extends AbstractAccount {

   private SingleAddressAccountContext _context;
   private List<Address> _addressList;
   private volatile boolean _isSynchronizing;
   private PublicPrivateKeyStore _keyStore;
   private SingleAddressAccountBacking _backing;

   public SingleAddressAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore,
                               NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi) {
      super(backing, network, wapi);
      _backing = backing;
      _context = context;
      _addressList = new ArrayList<Address>(1);
      _addressList.add(_context.getAddress());
      _keyStore = keyStore;
      _cachedBalance = _context.isArchived() ? new Balance(0, 0, 0, 0, 0, 0, false, _allowZeroConfSpending) : calculateLocalBalance();
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
   public boolean isValidEncryptionKey(KeyCipher cipher) {
      return _keyStore.isValidEncryptionKey(cipher);
   }

   private void clearInternalStateInt(boolean isArchived) {
      _backing.clear();
      _context = new SingleAddressAccountContext(_context.getId(), _context.getAddress(), isArchived, 0);
      _context.persist(_backing);
      _cachedBalance = null;
      if (isActive()) {
         _cachedBalance = calculateLocalBalance();
      }
   }

   public synchronized boolean synchronize(boolean synchronizeTransactionHistory) {
      checkNotArchived();
      _isSynchronizing = true;
      try {

         if (!synchronizeUnspentOutputs(_addressList)) {
            return false;
         }

         if (synchronizeTransactionHistory) {
            // Monitor young transactions
            if (!monitorYoungTransactions()) {
               return false;
            }
         }

         if (updateLocalBalance()) {
            // The balance has changed, lets see if there are any transactions
            // we need to discover
            if (synchronizeTransactionHistory) {
               if (!discoverTransactions()) {
                  return false;
               }
            }
         }

         _context.persistIfNecessary(_backing);
         return true;
      } finally {
         _isSynchronizing = false;
      }

   }

   private boolean discoverTransactions() {
      // Get the latest transactions
      List<Sha256Hash> discovered;
      try {
         discovered = _wapi.queryTransactionInventory(new QueryTransactionInventoryRequest(Wapi.VERSION, _addressList, 30))
               .getResult().txIds;
      } catch (WapiException e) {
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         postEvent(Event.SERVER_CONNECTION_ERROR);
         return false;
      }

      // Figure out whether there are any transactions we need to fetch
      List<Sha256Hash> toFetch = new LinkedList<Sha256Hash>();
      for (Sha256Hash id : discovered) {
         if (!_backing.hasTransaction(id)) {
            toFetch.add(id);
         }
      }

      // Fetch any missing transactions
      if (!toFetch.isEmpty()) {
         try {
            GetTransactionsResponse response;
            response = _wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, toFetch)).getResult();
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
   public Address getReceivingAddress() {
      //removed checkNotArchived, cause we wont to know the address for archived acc
      //to display them as archived accounts in "Accounts" tab
      return getAddress();
   }

   @Override
   public boolean canSpend() {
      return _keyStore.hasPrivateKey(getAddress());
   }

   @Override
   protected boolean isMine(Address address) {
      return getAddress().equals(address);
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
   protected void onNewTransaction(TransactionEx tex, Transaction t) {
      // Nothing to do for this account type
   }

   @Override
   public UUID getId() {
      return _context.getId();
   }

   @Override
   protected Address getChangeAddress() {
      return _context.getAddress();
   }

   @Override
   protected InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher) throws InvalidKeyCipher {
      if (getAddress().equals(address)) {
         return getPrivateKey(cipher);
      } else {
         return null;
      }
   }

   @Override
   protected PublicKey getPublicKeyForAddress(Address address) {
      if (getAddress().equals(address)) {
         return getPublicKey();
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Simple ");
      sb.append("ID: ").append(getId());
      if (isArchived()) {
         sb.append(" Archived");
      } else {
         if (_cachedBalance == null) {
            sb.append(" Balance: not known");
         } else {
            sb.append(" Balance: ").append(_cachedBalance);
         }
         sb.append(" Receiving Address: ").append(getReceivingAddress());
         sb.append(" Spendable Outputs: ").append(getSpendableOutputs().size());
      }
      return sb.toString();
   }

   @Override
   protected boolean isSynchronizing() {
      return _isSynchronizing;
   }

   public void forgetPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      _keyStore.forgetPrivateKey(getAddress(), cipher);
   }

   public InMemoryPrivateKey getPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      return _keyStore.getPrivateKey(getAddress(), cipher);
   }

   public void setPrivateKey(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
      _keyStore.setPrivateKey(getAddress(), privateKey, cipher);
   }

   public PublicKey getPublicKey() {
      return _keyStore.getPublicKey(getAddress());
   }

   public Address getAddress() {
      return _context.getAddress();
   }

}

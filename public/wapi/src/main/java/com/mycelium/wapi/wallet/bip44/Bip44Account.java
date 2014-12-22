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

package com.mycelium.wapi.wallet.bip44;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.Bip44AccountBacking;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Bip44Account extends AbstractAccount {

   private static final int EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20;
   private static final int INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 4;
   private static final int EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 1;
   private static final int INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 1;
   private static final long FORCED_DISCOVERY_INTERVAL_MS = 1000 * 60 * 60 * 24;

   protected Bip44AccountBacking _backing;
   protected Bip44AccountContext _context;
   protected Bip44AccountKeyManager _keyManager;
   protected BiMap<Address, Integer> _externalAddresses;
   protected BiMap<Address, Integer> _internalAddresses;
   private Address _currentReceivingAddress;
   protected volatile boolean _isSynchronizing;

   public Bip44Account(Bip44AccountContext context, Bip44AccountKeyManager keyManager,
                       NetworkParameters network, Bip44AccountBacking backing, Wapi wapi) {
      super(backing, network, wapi);
      _backing = backing;
      _keyManager = keyManager;
      _context = context;
      initAddressCache();

      if (isArchived()) {
         return;
      }
      ensureAddressIndexes(false);
      _cachedBalance = calculateLocalBalance();
   }

   protected void initAddressCache() {
      _externalAddresses = HashBiMap.create();
      _internalAddresses = HashBiMap.create();
   }

   @Override
   public UUID getId() {
      return _context.getId();
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
      return _keyManager.isValidEncryptionKey(cipher);
   }

   private void clearInternalStateInt(boolean isArchived) {
      _backing.clear();
      _context = new Bip44AccountContext(_context.getId(), _context.getAccountIndex(), isArchived);
      _context.persist(_backing);
      _externalAddresses.clear();
      _internalAddresses.clear();
      _currentReceivingAddress = null;
      _cachedBalance = null;
      if (isActive()) {
         ensureAddressIndexes(false);
         _cachedBalance = calculateLocalBalance();
      }
   }

   /**
    * Figure out whether this account has ever had any activity.
    * <p/>
    * An account has had activity if it has one or more external addresses with
    * transaction history.
    *
    * @return true if this account has ever had any activity, false otherwise
    */
   public boolean hasHadActivity() {
      // public method that needs no synchronization
      return _context.getLastExternalIndexWithActivity() != -1;
   }

   public int getAccountIndex() {
      // public method that needs no synchronization
      return _context.getAccountIndex();
   }

   private void ensureAddressIndexes(boolean full_look_ahead) {
      ensureAddressIndexes(true, full_look_ahead);
      ensureAddressIndexes(false, full_look_ahead);
      // The current receiving address is the next external address just above
      // the last
      // external address with activity
      Address receivingAddress = _externalAddresses.inverse().get(_context.getLastExternalIndexWithActivity() + 1);
      if (receivingAddress != null && !receivingAddress.equals(_currentReceivingAddress)) {
         _currentReceivingAddress = receivingAddress;
         postEvent(Event.RECEIVING_ADDRESS_CHANGED);
      }
   }

   protected void ensureAddressIndexes(boolean isChangeChain, boolean full_look_ahead) {
      int index;
      BiMap<Address, Integer> addressMap;
      if (isChangeChain) {
         index = _context.getLastInternalIndexWithActivity();
         if (full_look_ahead) {
            index += INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH;
         } else {
            index += INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH;
         }
         addressMap = _internalAddresses;
      } else {
         index = _context.getLastExternalIndexWithActivity();
         if (full_look_ahead) {
            index += EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH;
         } else {
            index += +EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH;
         }
         addressMap = _externalAddresses;
      }
      while (index >= 0) {
         if (addressMap.inverse().containsKey(index)) {
            return;
         }
         addressMap.put(_keyManager.getAddress(isChangeChain, index), index);
         index--;
      }
   }

   @Override
   public synchronized boolean synchronize(boolean synchronizeTransactionHistory) {
      checkNotArchived();
      _isSynchronizing = true;
      try {

         // Discover new addresses once in a while
         if (needsDiscovery()) {
            if (!discovery()) {
               return false;
            }
         }

         // Update unspent outputs
         if (!updateUnspentOutputs()) {
            return false;
         }
         return true;
      } finally {
         _isSynchronizing = false;
      }
   }

   private boolean needsDiscovery() {
      if (isArchived()) {
         return false;
      }
      return _context.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System.currentTimeMillis();
   }

   private synchronized boolean discovery() {
      try {
         while (doDiscovery()) {
            // Nothing
         }
      } catch (WapiException e) {
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         postEvent(Event.SERVER_CONNECTION_ERROR);
         return false;
      }
      _context.setLastDiscovery(System.currentTimeMillis());
      _context.persistIfNecessary(_backing);
      return true;
   }

   /**
    * Do a look ahead on the external address chain. If any transactions were
    * found the external and internal last active addresses are updated, and the
    * transactions and their parent transactions stored.
    *
    * @return true if something was found and the call should be repeated.
    * @throws com.mycelium.wapi.api.WapiException
    */
   private boolean doDiscovery() throws WapiException {
      // Ensure that all addresses in the look ahead window have been created
      ensureAddressIndexes(true);

      // Make look ahead address list
      List<Address> lookAhead = new ArrayList<Address>(EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH);
      for (int i = 0; i < EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH; i++) {
         Address externalAddress = _externalAddresses.inverse().get(_context.getLastExternalIndexWithActivity() + 1 + i);
         if (externalAddress != null) lookAhead.add(externalAddress);
      }
      for (int i = 0; i < INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH; i++) {
         lookAhead.add(_internalAddresses.inverse().get(_context.getLastInternalIndexWithActivity() + 1 + i));
      }
      // Do look ahead query
      List<Sha256Hash> ids = _wapi.queryTransactionInventory(
            new QueryTransactionInventoryRequest(Wapi.VERSION, lookAhead, Wapi.MAX_TRANSACTION_INVENTORY_LIMIT)).getResult().txIds;
      if (ids.isEmpty()) {
         // nothing found
         return false;
      }
      int lastExternalIndex = _context.getLastExternalIndexWithActivity();
      int lastInternalIndex = _context.getLastInternalIndexWithActivity();

      Collection<TransactionEx> transactions = _wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, ids))
            .getResult().transactions;
      handleNewExternalTransactions(transactions);
      // Return true if the last external or internal index has changed
      return lastExternalIndex != _context.getLastExternalIndexWithActivity() || lastInternalIndex != _context.getLastInternalIndexWithActivity();
   }

   private boolean updateUnspentOutputs() {
      // Get the list of addresses to monitor
      Collection<Address> combined = new ArrayList<Address>(_externalAddresses.keySet().size()
            + _context.getLastInternalIndexWithActivity() - _context.getFirstMonitoredInternalIndex() + 1);
      // Add all external addresses
      combined.addAll(_externalAddresses.keySet());

      // Add the change addresses we monitor
      for (int i = _context.getFirstMonitoredInternalIndex(); i < _internalAddresses.keySet().size(); i++) {
         combined.add(_internalAddresses.inverse().get(i));
      }

      if (!synchronizeUnspentOutputs(combined)) {
         return false;
      }

      // Monitor young transactions
      if (!monitorYoungTransactions()) {
         return false;
      }

      updateLocalBalance();

      _context.persistIfNecessary(_backing);
      return true;
   }

   private void tightenInternalAddressScanRange() {
      // Find the lowest internal index at which we have an unspent output
      Collection<TransactionOutputEx> unspent = _backing.getAllUnspentOutputs();
      int minInternalIndex = Integer.MAX_VALUE;
      for (TransactionOutputEx output : unspent) {
         ScriptOutput outputScript = ScriptOutput.fromScriptBytes(output.script);
         if (outputScript == null) {
            // never happens, we have parsed it before
            continue;
         }
         Address address = outputScript.getAddress(_network);
         Integer index = _internalAddresses.get(address);
         if (index != null) {
            minInternalIndex = Math.min(minInternalIndex, index);
         }
      }

      // XXX also, from all the outgoing unconfirmed transactions we have, check
      // if any of them have outputs that we send from our change chain. If the
      // related address is lower than the one we had above, use its index as
      // the first monitored one.

      if (minInternalIndex == Integer.MAX_VALUE) {
         // there are no unspent outputs in our change chain
         _context.setFirstMonitoredInternalIndex(Math.max(0, _context.getLastInternalIndexWithActivity()));
      } else {
         _context.setFirstMonitoredInternalIndex(minInternalIndex);
      }
   }

   protected Address getChangeAddress() {
      // Get the next internal address just above the last address with activity
      return _internalAddresses.inverse().get(_context.getLastInternalIndexWithActivity() + 1);
   }

   public Address getReceivingAddress() {
      // public method that needs no synchronization
      checkNotArchived();
      return _currentReceivingAddress;
   }

   //used for message signing picker
   public List<Address> getAllAddresses() {
      List<Address> addresses = new ArrayList<Address>();

      //get all used external plus the next unused
      BiMap<Integer, Address> external = _externalAddresses.inverse();
      Integer externalIndex = _context.getLastExternalIndexWithActivity() + 1;
      for (Integer i = 0; i <= externalIndex; i++) {
         addresses.add(external.get(i));
      }

      //get all used internal
      BiMap<Integer, Address> internal = _internalAddresses.inverse();
      Integer internalIndex = _context.getLastInternalIndexWithActivity();
      for (Integer i = 0; i <= internalIndex; i++) {
         addresses.add(internal.get(i));
      }

      return addresses;
   }

   @Override
   protected boolean isMine(Address address) {
      return _internalAddresses.containsKey(address) || _externalAddresses.containsKey(address);
   }

   @Override
   protected void onNewTransaction(TransactionEx tex, Transaction t) {
      // check whether we need to update our last index for activity
      updateLastIndexWithActivity(t);
   }

   @Override
   protected void onTransactionsBroadcasted(List<Sha256Hash> txids) {
      // See if we can reduce the internal scan range
      tightenInternalAddressScanRange();
      _context.persistIfNecessary(_backing);
   }

   private void updateLastIndexWithActivity(Transaction t) {
      // Investigate whether the transaction sends us any coins
      for (int i = 0; i < t.outputs.length; i++) {
         TransactionOutput out = t.outputs[i];
         Address receivingAddress = out.script.getAddress(_network);
         Integer externalIndex = _externalAddresses.get(receivingAddress);
         if (externalIndex != null) {
            updateLastExternalIndex(externalIndex);
         } else {
            updateLastInternalIndex(receivingAddress);
         }
      }
      ensureAddressIndexes(false);
   }

   protected void updateLastExternalIndex(Integer externalIndex) {
      // Sends coins to an external address, update internal max index if
      // necessary
      _context.setLastExternalIndexWithActivity(Math.max(_context.getLastExternalIndexWithActivity(),
            externalIndex));
   }

   protected void updateLastInternalIndex(Address receivingAddress) {
      Integer internalIndex = _internalAddresses.get(receivingAddress);
      if (internalIndex != null) {
         // Sends coins to an internal address, update internal max index
         // if necessary
         _context.setLastInternalIndexWithActivity(Math.max(_context.getLastInternalIndexWithActivity(),
               internalIndex));
      }
   }

   @Override
   public InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher) throws InvalidKeyCipher {
      boolean isChange = false;
      Integer index = _externalAddresses.get(address);
      if (index == null) {
         index = _internalAddresses.get(address);
         isChange = true;
      }
      if (index == null) {
         return null;
      }
      return _keyManager.getPrivateKey(isChange, index, cipher);
   }

   @Override
   protected PublicKey getPublicKeyForAddress(Address address) {
      boolean isChange = false;
      Integer index = _externalAddresses.get(address);
      if (index == null) {
         index = _internalAddresses.get(address);
         isChange = true;
      }
      if (index == null) {
         return null;
      }
      return _keyManager.getPublicKey(isChange, index);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("HD ");
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
         toStringMonitoredAddresses(sb);
         sb.append(" Spendable Outputs: ").append(getSpendableOutputs().size());
      }
      return sb.toString();
   }

   protected void toStringMonitoredAddresses(StringBuilder sb) {
      sb.append(" Monitored Addresses: external=").append(_context.getLastExternalIndexWithActivity() + 2)
            .append(" internal=")
            .append(_context.getLastInternalIndexWithActivity() + 1 - _context.getFirstMonitoredInternalIndex());
   }


   public int getPrivateKeyCount() {
      return _context.getLastExternalIndexWithActivity() + 2 + _context.getLastInternalIndexWithActivity() + 1;
   }

   public boolean canSpend() {
      // For now we do not support read-only Bip44 accounts
      return true;
   }

   @Override
   public int getBlockChainHeight() {
      // public method that needs no synchronization
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
   protected boolean isSynchronizing() {
      return _isSynchronizing;
   }

}

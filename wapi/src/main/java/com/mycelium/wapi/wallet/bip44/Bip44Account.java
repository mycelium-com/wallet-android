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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.lib.TransactionExApi;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;

import java.util.*;

public class Bip44Account extends AbstractAccount implements ExportableAccount {

   private static final int EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20;
   private static final int INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH = 20;
   private static final int EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH = 4;
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
      ensureAddressIndexes();
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

   @Override
   public boolean isDerivedFromInternalMasterseed() {
      return (getAccountType() == Bip44AccountContext.ACCOUNT_TYPE_FROM_MASTERSEED);
   }

   private void clearInternalStateInt(boolean isArchived) {
      _backing.clear();
      _externalAddresses.clear();
      _internalAddresses.clear();
      _currentReceivingAddress = null;
      _cachedBalance = null;
      initContext(isArchived);
      if (isActive()) {
         ensureAddressIndexes();
         _cachedBalance = calculateLocalBalance();
      }
   }

   protected void initContext(boolean isArchived) {
      _context = new Bip44AccountContext(_context.getId(), _context.getAccountIndex(), isArchived, _context.getAccountType(), _context.getAccountSubId());
      _context.persist(_backing);
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

   private void ensureAddressIndexes() {
      ensureAddressIndexes(true, true);
      ensureAddressIndexes(false, true);
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
            index += EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH;
         }
         addressMap = _externalAddresses;
      }
      while (index >= 0) {
         if (addressMap.inverse().containsKey(index)) {
            return;
         }
         addressMap.put(Preconditions.checkNotNull(_keyManager.getAddress(isChangeChain, index)), index);
         index--;
      }
   }

   private List<Address> getAddressesToSync(SyncMode mode){
      List<Address> ret;
      int currentInternalAddressId = _context.getLastInternalIndexWithActivity() + 1;
      int currentExternalAddressId = _context.getLastExternalIndexWithActivity() + 1;
      if (mode.mode.equals(SyncMode.Mode.FULL_SYNC)){
         // check the full change-chain and external-chain
         ret = Lists.newArrayList(
            getAddressRange(true, 0, currentInternalAddressId + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH)
         );
         ret.addAll(
               getAddressRange(false, 0, currentExternalAddressId + EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH)
         );
      }else if (mode.mode.equals(SyncMode.Mode.NORMAL_SYNC)){
         // check the current change address plus small lookahead;
         // plus the current external address plus a small range before and after it
         ret = Lists.newArrayList(
               getAddressRange(true, currentInternalAddressId, currentInternalAddressId + INTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH)
         );
         ret.addAll(
               getAddressRange(false, currentExternalAddressId - 3, currentExternalAddressId + EXTERNAL_MINIMAL_ADDRESS_LOOK_AHEAD_LENGTH)
         );

      }else if (mode.mode.equals(SyncMode.Mode.FAST_SYNC)){
         // check only the current change address
         // plus the current external plus small lookahead
         ret = Lists.newArrayList((Address)_keyManager.getAddress(true, currentInternalAddressId + 1));
         ret.addAll(
               getAddressRange(false, currentExternalAddressId, currentExternalAddressId + 2)
         );
      }else if (mode.mode.equals(SyncMode.Mode.ONE_ADDRESS) && mode.addressToSync != null){
         // only check for the supplied address
         if (isMine(mode.addressToSync)) {
            ret = Lists.newArrayList(mode.addressToSync);
         } else {
            throw new IllegalArgumentException("Address " + mode.addressToSync + " is not part of my account addresses");
         }
      } else {
         throw new IllegalArgumentException("Unexpected SyncMode");
      }
      return ImmutableList.copyOf(ret);
   }

   private List<Address> getAddressRange(boolean isChangeChain, int fromIndex, int toIndex){
      fromIndex = Math.max(0, fromIndex); // clip at zero
      ArrayList<Address> ret = new ArrayList<Address>(toIndex - fromIndex + 1);
      for(int i=fromIndex; i<=toIndex; i++){
         ret.add(_keyManager.getAddress(isChangeChain, i));
      }
      return ret;
   }

   @Override
   public synchronized boolean doSynchronization(SyncMode mode) {
      checkNotArchived();
      _isSynchronizing = true;
      _logger.logInfo("Starting sync: " + mode);
      if (needsDiscovery()) {
         mode = SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED;
      }
      try {
         if (mode.mode == SyncMode.Mode.FULL_SYNC){
            // Discover new addresses once in a while
            if (!discovery()) {
               return false;
            }
         }

         // Update unspent outputs
         return updateUnspentOutputs(mode);
      } finally {
         _isSynchronizing = false;
      }

   }

   private boolean needsDiscovery() {
      return !isArchived() && _context.getLastDiscovery() + FORCED_DISCOVERY_INTERVAL_MS < System.currentTimeMillis();
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
      ensureAddressIndexes();

      // Make look ahead address list
      List<Address> lookAhead = new ArrayList<Address>(EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH + INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH);

      final BiMap<Integer, Address> extInverse = _externalAddresses.inverse();
      final BiMap<Integer, Address> intInverse = _internalAddresses.inverse();

      for (int i = 0; i < EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH; i++) {
         Address externalAddress = extInverse.get(_context.getLastExternalIndexWithActivity() + 1 + i);
         if (externalAddress != null) lookAhead.add(externalAddress);
      }
      for (int i = 0; i < INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH; i++) {
         lookAhead.add(intInverse.get(_context.getLastInternalIndexWithActivity() + 1 + i));
      }
      return doDiscoveryForAddresses(lookAhead);
   }

   @Override
   protected boolean doDiscoveryForAddresses(List<Address> lookAhead) throws WapiException {
      // Do look ahead query
      List<Sha256Hash> ids = _wapi.queryTransactionInventory(
            new QueryTransactionInventoryRequest(Wapi.VERSION, lookAhead, Wapi.MAX_TRANSACTION_INVENTORY_LIMIT)).getResult().txIds;
      if (ids.isEmpty()) {
         // nothing found
         return false;
      }
      int lastExternalIndex = _context.getLastExternalIndexWithActivity();
      int lastInternalIndex = _context.getLastInternalIndexWithActivity();

      Collection<TransactionExApi> transactions = getTransactionsBatched(ids).getResult().transactions;
      handleNewExternalTransactions(transactions);
      // Return true if the last external or internal index has changed
      return lastExternalIndex != _context.getLastExternalIndexWithActivity() || lastInternalIndex != _context.getLastInternalIndexWithActivity();
   }

   private boolean updateUnspentOutputs(SyncMode mode) {
      List<Address> checkAddresses = getAddressesToSync(mode);

      final int newUtxos = synchronizeUnspentOutputs(checkAddresses);

      if (newUtxos == -1) {
         return false;
      }

      if (newUtxos > 0 && !mode.mode.equals(SyncMode.Mode.FULL_SYNC)){
         // we got new UTXOs but did not made a full sync. The UTXO might be coming
         // from change outputs spending from addresses we are currently not checking
         // -> rerun the synchronizeUnspentOutputs for a FULL_SYNC
         checkAddresses = getAddressesToSync(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
         if (synchronizeUnspentOutputs(checkAddresses) == -1){
            return false;
         }
      }


      // update state of recent received transaction to update their confirmation state
      if (mode.mode != SyncMode.Mode.ONE_ADDRESS) {
         // Monitor young transactions
         if (!monitorYoungTransactions()) {
            return false;
         }
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

   public Optional<Address> getReceivingAddress() {
      // if this account is archived, we cant ensure that we have the most recent ReceivingAddress (or any at all)
      // so return absent.
      if (isArchived()) {
         return Optional.absent();
      } else {
         return Optional.of(_currentReceivingAddress);
      }
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
   public boolean isMine(Address address) {
      Preconditions.checkNotNull(address);
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
      ensureAddressIndexes();
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
      IndexLookUp indexLookUp = IndexLookUp.forAddress(address, _externalAddresses, _internalAddresses);
      if (indexLookUp == null) {
         // we did not find it - to be sure, generate all addresses and search again
         ensureAddressIndexes();
         indexLookUp = IndexLookUp.forAddress(address, _externalAddresses, _internalAddresses);
      }
      if (indexLookUp == null) {
         // still not found? give up...
         return null;
      }
      return _keyManager.getPrivateKey(indexLookUp.isChange(), indexLookUp.getIndex(), cipher);
   }

   @Override
   protected PublicKey getPublicKeyForAddress(Address address) {
      IndexLookUp indexLookUp = IndexLookUp.forAddress(address, _externalAddresses, _internalAddresses);
      if (indexLookUp == null) {
         // we did not find it - to be sure, generate all addresses and search again
         ensureAddressIndexes();
         indexLookUp = IndexLookUp.forAddress(address, _externalAddresses, _internalAddresses);
      }
      if (indexLookUp == null) {
         // still not found? give up...
         return null;
      }
      return Preconditions.checkNotNull(_keyManager.getPublicKey(indexLookUp.isChange(), indexLookUp.getIndex()));
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
         Optional<Address> receivingAddress = getReceivingAddress();
         sb.append(" Receiving Address: ").append(receivingAddress.isPresent() ? receivingAddress.get().toString() : "");
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

   public Optional<Integer[]> getAddressId(Address address){
      if (_externalAddresses.containsKey(address)){
         return Optional.of(new Integer[]{0, _externalAddresses.get(address) });
      }else if (_internalAddresses.containsKey(address)){
         return Optional.of(new Integer[]{1, _internalAddresses.get(address) });
      }
      return Optional.absent();
   }

   // returns true if this is one of our already used or monitored internal (="change") addresses
   @Override
   public boolean isOwnInternalAddress(Address address){
      Optional<Integer[]> addressId = getAddressId(address);
      return addressId.isPresent() && addressId.get()[0] == 1;
   }

   // returns true if this is one of our already used or monitored external (=normal receiving) addresses
   @Override
   public boolean isOwnExternalAddress(Address address){
      Optional<Integer[]> addressId = getAddressId(address);
      return addressId.isPresent() && addressId.get()[0] == 0;
   }

   @Override
   public boolean canSpend() {
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

   @Override
   public Data getExportData(KeyCipher cipher) {
      Optional<String> privKey = Optional.absent();

      if (canSpend()) {
         try {
            privKey = Optional.of(_keyManager.getPrivateAccountRoot(cipher).serialize(_network));
         } catch (InvalidKeyCipher ignore) {
         }
      }

      Optional<String> pubKey = Optional.of(_keyManager.getPublicAccountRoot().serialize(getNetwork()));
      return new Data(privKey, pubKey);
   }


   public int getAccountType(){
      return _context.getAccountType();
   }

   // deletes everything account related from the backing
   // this method is only allowed for accounts that use a SubValueKeystore
   public void clearBacking() {
      _keyManager.deleteSubKeyStore();
   }

   // Helper class to find the mapping for a Address in the internal or external chain
   private static class IndexLookUp {
      private final boolean isChange;
      private final Integer index;

      public static IndexLookUp forAddress(Address address, Map<Address, Integer> external, Map<Address, Integer> internal) {
         Integer index = external.get(address);
         if (index == null) {
            index = internal.get(address);
            if (index == null) {
               return null;
            } else {
               // found it in the internal(=change)-chain
               return new IndexLookUp(true, index);
            }
         } else {
            // found it in the external chain
            return new IndexLookUp(false, index);
         }
      }

      private IndexLookUp(boolean isChange, Integer index) {
         this.isChange = isChange;
         this.index = index;
      }

      public boolean isChange() {
         return isChange;
      }

      public Integer getIndex() {
         return index;
      }

   }
}

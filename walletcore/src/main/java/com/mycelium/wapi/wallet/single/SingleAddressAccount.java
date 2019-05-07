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

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode;

import java.util.*;

public class SingleAddressAccount extends AbstractAccount implements ExportableAccount {
   private SingleAddressAccountContext _context;
   private List<Address> _addressList;
   private volatile boolean _isSynchronizing;
   private PublicPrivateKeyStore _keyStore;
   private PublicKey publicKey;
   private SingleAddressAccountBacking _backing;
   private Reference<ChangeAddressMode> changeAddressModeReference;

   public SingleAddressAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore,
                               NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi,
                               Reference<ChangeAddressMode> changeAddressModeReference) {
      this(context, keyStore, network, backing, wapi, changeAddressModeReference, true);
   }

   public SingleAddressAccount(SingleAddressAccountContext context, PublicPrivateKeyStore keyStore,
                               NetworkParameters network, SingleAddressAccountBacking backing, Wapi wapi,
                               Reference<ChangeAddressMode> changeAddressModeReference, boolean shouldPersistAddress) {
      super(backing, network, wapi);
      this.changeAddressModeReference = changeAddressModeReference;
      _backing = backing;
      type = WalletAccount.Type.BTCSINGLEADDRESS;
      _context = context;
      _addressList = new ArrayList<>(3);
      _keyStore = keyStore;
      publicKey = _keyStore.getPublicKey(getAddress());
      if (shouldPersistAddress) {
         persistAddresses();
      }
      _addressList.addAll(context.getAddresses().values());
      _cachedBalance = _context.isArchived()
              ? new Balance(0, 0, 0, 0, 0, 0, false, _allowZeroConfSpending)
              : calculateLocalBalance();
   }

   private void persistAddresses() {
      try {
         InMemoryPrivateKey privateKey = getPrivateKey(AesKeyCipher.defaultKeyCipher());
         if (privateKey != null) {
            Map<AddressType, Address> allPossibleAddresses = privateKey.getPublicKey().getAllSupportedAddresses(_network, true);
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
      _context = new SingleAddressAccountContext(_context.getId(), _context.getAddresses(), isArchived, 0,
              _context.getDefaultAddressType());
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

         // recalculate cached Balance
         updateLocalBalance();

         _context.persistIfNecessary(_backing);
         return true;
      } finally {
         _isSynchronizing = false;
         syncTotalRetrievedTransactions = 0;
      }
   }

   @Override
   public List<AddressType> getAvailableAddressTypes() {
      return new ArrayList<>(_context.getAddresses().keySet());
   }

   @Override
   public Address getReceivingAddress(AddressType addressType) {
      return getAddress(addressType);
   }

   @Override
   public void setDefaultAddressType(AddressType addressType) {
      _context.setDefaultAddressType(addressType);
      _context.persistIfNecessary(_backing);
   }

   private boolean discoverTransactions() {
      // Get the latest transactions
      List<Sha256Hash> discovered;
      try {
         final QueryTransactionInventoryResponse result = _wapi.queryTransactionInventory(new QueryTransactionInventoryRequest(Wapi.VERSION, _addressList))
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
      int chunkSize = 50;
      for (int fromIndex = 0; fromIndex < toFetch.size(); fromIndex += chunkSize) {
         try {
            int toIndex = Math.min(fromIndex + chunkSize, toFetch.size());
            GetTransactionsResponse response = getTransactionsBatched(toFetch.subList(fromIndex, toIndex)).getResult();
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
   protected Address getChangeAddress(Address destinationAddress) {
      Address result;
      switch (changeAddressModeReference.get()) {
         case P2WPKH:
            result = getAddress(AddressType.P2WPKH);
            break;
         case P2SH_P2WPKH:
            result = getAddress(AddressType.P2SH_P2WPKH);
            break;
         case PRIVACY:
            result = getAddress(destinationAddress.getType());
            break;
         default:
            throw new IllegalStateException();
      }
      if (result == null) {
         result = getAddress();
      }
      return result;
   }

   @Override
   protected Address getChangeAddress(List<Address> destinationAddresses) {
      Map<AddressType, Integer> mostUsedTypesMap = new HashMap<>();
      for (Address address : destinationAddresses) {
         Integer currentValue = mostUsedTypesMap.get(address.getType());
         if (currentValue == null) {
            currentValue = 0;
         }
         mostUsedTypesMap.put(address.getType(), currentValue + 1);
      }
      int max = 0;
      AddressType maxedOn = null;
      for (AddressType addressType : mostUsedTypesMap.keySet()) {
         if (mostUsedTypesMap.get(addressType) > max) {
            max = mostUsedTypesMap.get(addressType);
            maxedOn = addressType;
         }
      }
      Address result;
      switch (changeAddressModeReference.get()) {
         case P2WPKH:
            result = getAddress(AddressType.P2WPKH);
            break;
         case P2SH_P2WPKH:
            result = getAddress(AddressType.P2SH_P2WPKH);
            break;
         case PRIVACY:
            result = getAddress(maxedOn);
            break;
         default:
            throw new IllegalStateException();
      }
      if (result == null) {
         result = getAddress();
      }
      return result;
   }

   @Override
   protected InMemoryPrivateKey getPrivateKey(PublicKey publicKey, KeyCipher cipher) throws InvalidKeyCipher {
      if (getPublicKey().equals(publicKey) || new PublicKey(publicKey.getPubKeyCompressed()).equals(publicKey)) {
         InMemoryPrivateKey privateKey = getPrivateKey(cipher);
         if (publicKey.isCompressed()) {
            return new InMemoryPrivateKey(privateKey.getPrivateKeyBytes(), true);
         } else {
            return privateKey;
         }
      }

      return null;
   }

   @Override
   protected InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher) throws InvalidKeyCipher {
      if (_addressList.contains(address)) {
         InMemoryPrivateKey privateKey = getPrivateKey(cipher);
         if (address.getType() == AddressType.P2SH_P2WPKH || address.getType() == AddressType.P2WPKH) {
            return new InMemoryPrivateKey(privateKey.getPrivateKeyBytes(), true);
         } else {
            return privateKey;
         }
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
   protected boolean isSynchronizing() {
      return _isSynchronizing;
   }

   public void forgetPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      if (getPublicKey() == null) {
         _keyStore.forgetPrivateKey(getAddress(), cipher);
      } else {
         for (Address address : getPublicKey().getAllSupportedAddresses(_network, true).values()) {
            _keyStore.forgetPrivateKey(address, cipher);
         }
      }
   }

   public InMemoryPrivateKey getPrivateKey(KeyCipher cipher) throws InvalidKeyCipher {
      return _keyStore.getPrivateKey(getAddress(), cipher);
   }

   /**
    * This method is used for Colu account, so method should NEVER persist addresses as only P2PKH addresses are used for Colu
    */
   public void setPrivateKey(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
      _keyStore.setPrivateKey(getAddress(), privateKey, cipher);
   }

   public PublicKey getPublicKey() {
      return _keyStore.getPublicKey(getAddress());
   }

   /**
    * @return default address
    */
   public Address getAddress() {
      Address defaultAddress = getAddress(_context.getDefaultAddressType());
      if (defaultAddress != null) {
         return defaultAddress;
      } else {
         return _context.getAddresses().values().iterator().next();
      }
   }

   public Address getAddress(AddressType type) {
      if (publicKey != null && !publicKey.isCompressed()) {
         if (type == AddressType.P2SH_P2WPKH || type == AddressType.P2WPKH) {
            return null;
         }
      }
      return _context.getAddresses().get(type);
   }

   @Override
   public Data getExportData(KeyCipher cipher) {
      Optional<String> privKey = Optional.absent();
      Map<BipDerivationType, String> publicDataMap = new HashMap<>();
      if (canSpend()) {
         try {
            InMemoryPrivateKey privateKey = _keyStore.getPrivateKey(getAddress(), cipher);
            privKey = Optional.of(privateKey.getBase58EncodedPrivateKey(getNetwork()));
         } catch (InvalidKeyCipher ignore) {
         }
      }
      for (AddressType type : getAvailableAddressTypes()) {
         Address address = getAddress(type);
         if (address != null) {
            publicDataMap.put(BipDerivationType.Companion.getDerivationTypeByAddressType(type),
                    address.toString());
         }
      }
      return new Data(privKey, publicDataMap);
   }

   @Override
   protected Map<BipDerivationType, Boolean> doDiscoveryForAddresses(List<Address> lookAhead) throws WapiException {
      // not needed for SingleAddressAccount
      return Collections.emptyMap();
   }
}

/*
 * Copyright 2013 Megion Research and Development GmbH
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mrd.bitlib.crypto.PrivateKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.ScriptOutput;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mycelium.wallet.persistence.PersistedOutput;

public class Wallet implements Serializable {

   private static final long serialVersionUID = 1L;
   private static final int COINBASE_MIN_CONFIRMATIONS = 120;

   public interface WalletUpdateHandler {
      public void walletUpdatedCallback(Wallet wallet, boolean success);
   }

   public static class SpendableOutputs implements Serializable {
      private static final long serialVersionUID = 1L;

      public Set<UnspentTransactionOutput> unspent;
      public Set<UnspentTransactionOutput> change;
      public Set<UnspentTransactionOutput> receiving;

      public SpendableOutputs(Set<UnspentTransactionOutput> unspent, Set<UnspentTransactionOutput> change,
            Set<UnspentTransactionOutput> receiving) {
         this.unspent = unspent;
         this.change = change;
         this.receiving = receiving;
      }
   }

   private Set<Record> _records;
   private LinkedHashSet<Address> _addresses;
   private Record _receiving;

   public Wallet(Record record) {
      _records = new LinkedHashSet<Record>();
      _records.add(record);
      _addresses = new LinkedHashSet<Address>();
      _addresses.add(record.address);
      _receiving = record;
   }

   public Wallet(List<Record> records, Record receiving) {
      _records = new LinkedHashSet<Record>();
      _records.addAll(records);
      _addresses = new LinkedHashSet<Address>();
      for (Record record : records) {
         _addresses.add(record.address);
      }
      _receiving = receiving;
   }

   public Address getReceivingAddress() {
      return _receiving.address;
   }

   /**
    * Change the receiving address but only if it is part of the wallet's record
    * set
    */
   public void changeReceivingAddress(Address address) {
      for (Record record : _records) {
         if (record.address.equals(address)) {
            _receiving = record;
            System.out.println("New Address: " + _receiving.toString());
            break;
         }
      }
   }

   public boolean hasPrivateKeyForReceivingAddress() {
      return _receiving.hasPrivateKey();
   }

   /**
    * Tell whether a wallet is backed up by checking whether all contained
    * records with private keys have had their backup verified
    */
   public boolean isWalletBackedUp() {
      for (Record record : _records) {
         if (record.needsBackupVerification()) {
            return false;
         }
      }
      return true;
   }

   public Set<Address> getAddressSet() {
      return Collections.unmodifiableSet(_addresses);
   }

   /**
    * Get addresses in the same order as the records the wallet was created from
    */
   public List<Address> getAddresses() {
      return new ArrayList<Address>(_addresses);
   }

   public int getIndexOfReceivingAddress() {
      int index = 0;
      for (Address address : _addresses) {
         if (address.equals(_receiving.address)) {
            return index;
         }
         index++;
      }
      // Should not happen
      throw new RuntimeException("Receiving address is not in wallet");
   }

   private Set<Address> getAddressSetWithKeys() {
      Set<Address> set = new HashSet<Address>();
      for (Record record : _records) {
         if (record.hasPrivateKey()) {
            set.add(record.address);
         }
      }
      return set;
   }

   public PrivateKeyRing getPrivateKeyRing() {
      PrivateKeyRing ring = new PrivateKeyRing();
      for (Record record : _records) {
         if (record.hasPrivateKey()) {
            ring.addPrivateKey(record.key, record.key.getPublicKey(), record.address);
         }
      }
      return ring;
   }

   public BalanceInfo getLocalBalance(BlockChainAddressTracker blockChainAddressTracker) {
      TransactionOutputInfo outputs = blockChainAddressTracker.getOutputInfo(_addresses);

      // When calculating the sending sum we want to subtract the unmodified
      // change sum, as we don't want the change to be in both the sending and
      // change balance
      long sendingSum = sumOutputs(outputs.sending) - sumOutputs(outputs.receivingChange);

      // We want to remove confirmed, change, and foreign coins that we are in
      // the process of sending before calculating the balance
      outputs.confirmed.removeAll(outputs.sending);
      outputs.receivingChange.removeAll(outputs.sending);
      outputs.receivingForeign.removeAll(outputs.sending);

      // Because the output set may have been retrieved in the middle of
      // updating a block some outputs may be present in confirmed and not yet
      // removed from receiving. Fix that by eliminating them from the
      // confirmed
      // set
      outputs.confirmed.removeAll(outputs.receivingChange);
      outputs.confirmed.removeAll(outputs.receivingForeign);

      long confirmedSum = sumOutputs(outputs.confirmed);
      long changeSum = sumOutputs(outputs.receivingChange);
      long foreignSum = sumOutputs(outputs.receivingForeign);

      BalanceInfo balance = new BalanceInfo(confirmedSum, foreignSum, sendingSum, changeSum, outputs.lowestUpdateTime,
            outputs.highestUpdateTime, outputs.lastObservedBlockHeight);
      return balance;
   }

   public SpendableOutputs getLocalSpendableOutputs(BlockChainAddressTracker blockChainAddressTracker) {
      TransactionOutputInfo outputs = blockChainAddressTracker.getOutputInfo(getAddressSetWithKeys());

      // We want to remove confirmed, change, and foreign coins that we are in
      // the process of sending before calculating the balance
      outputs.confirmed.removeAll(outputs.sending);
      outputs.receivingChange.removeAll(outputs.sending);
      outputs.receivingForeign.removeAll(outputs.sending);

      // Because the output set may have been retrieved in the middle of
      // updating a block some outputs may be present in confirmed and not yet
      // removed from receiving. Fix that by eliminating them from the
      // confirmed
      // set
      outputs.confirmed.removeAll(outputs.receivingChange);
      outputs.confirmed.removeAll(outputs.receivingForeign);

      // Prune confirmed outputs for coinbase outputs that are not old enough
      // for spending
      int blockChainHeight = blockChainAddressTracker.getLastObservedBlockHeight();
      Iterator<PersistedOutput> it = outputs.confirmed.iterator();
      while (it.hasNext()) {
         PersistedOutput output = it.next();
         if (output.isCoinBase) {
            int confirmations = blockChainHeight - output.height;
            if (confirmations < COINBASE_MIN_CONFIRMATIONS) {
               it.remove();
            }
         }
      }

      SpendableOutputs spendable = new SpendableOutputs(transform(outputs.confirmed),
            transform(outputs.receivingChange), transform(outputs.receivingForeign));
      return spendable;
   }

   private static Set<UnspentTransactionOutput> transform(Set<PersistedOutput> source) {
      Set<UnspentTransactionOutput> outputs = new HashSet<UnspentTransactionOutput>();
      for (PersistedOutput s : source) {
         ScriptOutput script = ScriptOutput.fromScriptBytes(s.script);
         outputs.add(new UnspentTransactionOutput(s.outPoint, s.height, s.value, script));
      }
      return outputs;
   }

   public boolean canSpend() {
      for (Record record : _records) {
         if (record.hasPrivateKey()) {
            return true;
         }
      }
      return false;
   }

   private long sumOutputs(Collection<PersistedOutput> outputs) {
      long sum = 0;
      for (PersistedOutput output : outputs) {
         sum += output.value;
      }
      return sum;
   }

   @Override
   public int hashCode() {
      // Simply use the hash code of the first address
      return _addresses.iterator().next().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof Wallet)) {
         return false;
      }
      Wallet other = (Wallet) obj;
      if (_addresses.size() != other._addresses.size()) {
         return false;
      }
      // Compare each address in the two wallets, order is maintained in the two
      // lists
      List<Address> first = getAddresses();
      List<Address> second = other.getAddresses();
      for (int i = 0; i < first.size(); i++) {
         if (!first.get(i).equals(second.get(i))) {
            return false;
         }
      }
      return true;
   }

   public void requestUpdate(BlockChainAddressTracker blockChainAddressTracker) {
      blockChainAddressTracker.updateAddresses(_addresses);
   }

}

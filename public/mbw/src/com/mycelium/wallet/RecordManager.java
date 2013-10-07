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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.Record.Source;
import com.mycelium.wallet.Record.Tag;

public class RecordManager {

   private final RandomSource randomSource = new AndroidRandomSource();

   private Context _applicationContext;
   private List<Record> _records = null;
   private Record _defaultSpendingRecord = null;
   private Address _selectedAddress = null;

   public RecordManager(Context applicationContext) {
      _applicationContext = applicationContext;
      loadRecords();
   }

   public synchronized int numRecords() {
      return _records.size();
   }

   public synchronized int numRecords(Tag tag) {
      int num = 0;
      for (Record r : _records) {
         if (r.tag == tag) {
            num++;
         }
      }
      return num;
   }

   public List<String> getActiveWalletNames() {
      Set<String> names = new HashSet<String>();
      for (Record record : _records) {
         if (record.tag == Tag.ACTIVE) {
            names.add(record.walletName);
         }
      }
      List<String> list = new ArrayList<String>(names);
      Collections.sort(list);
      return list;
   }

   public synchronized Wallet getWallet(WalletMode mode) {
      Record selected = getSelectedRecord();
      if (mode.equals(WalletMode.Segregated)) {
         // User is in segregated mode, make a wallet for the selected record
         // only
         return new Wallet(selected);
      } else {
         if (selected.tag == Tag.ARCHIVE) {
            // User has chosen an archived record, make a wallet for the
            // selected record only
            return new Wallet(selected);
         } else {
            return new Wallet(getRecords(Tag.ACTIVE), selected);
         }
      }
   }

   public synchronized Record getSelectedRecord() {
      Record record = getRecord(_selectedAddress);
      if (record != null) {
         return record;
      }
      // If we didn't find a record it may have been deleted, select a random
      // one from our list
      if (_records.isEmpty()) {
         // This should never happen
         throw new RuntimeException("We have no records while finding default selection");
      }
      record = _records.get(0);
      _selectedAddress = record.address;
      saveSelected(_applicationContext, _selectedAddress);
      return record;
   }

   // public synchronized String getSelectedWalletName() {
   // // See if there are any keys in the selected wallet
   // if (!getActiveRecordsByWalletName(_selectedWalletName).isEmpty()) {
   // // Everything normal
   // return _selectedWalletName;
   // }
   //
   // // We did not have any keys in the currently selected wallet
   // // Select the first active key and use its wallet name
   // List<String> list = getActiveWalletNames();
   // if (list.isEmpty()) {
   // // This should never happen. Make sure that we have a new random key in
   // // the default wallet
   // Record newRecord = Record.createRandom(randomSource, _defaultWalletName);
   // Toast.makeText(_applicationContext, R.string.created_new_random_key,
   // Toast.LENGTH_LONG).show();
   // _records.add(newRecord);
   // _selectedWalletName = newRecord.walletName;
   // } else {
   // _selectedWalletName = list.get(0);
   // }
   // saveSelected(_applicationContext, _selectedAddress, _selectedWalletName);
   // return _selectedWalletName;
   // }

   public synchronized void setSelectedRecord(Address address) {
      Record r = getRecordInt(address);
      if (r != null) {
         _selectedAddress = r.address;
         saveSelected(_applicationContext, _selectedAddress);
      }
   }

   /**
    * Get a copy of all the records with a specific tag
    */
   public synchronized List<Record> getRecords(Tag tag) {
      List<Record> list = new ArrayList<Record>(_records.size());
      for (Record r : _records) {
         if (r.tag == tag) {
            list.add(r.copy());
         }
      }
      return list;
   }

   /**
    * Get a list of records with private keys
    */
   public synchronized List<Record> getRecordsWithPrivateKeys(Tag tag) {
      List<Record> list = new ArrayList<Record>(_records.size());
      for (Record r : _records) {
         if (r.tag == tag && r.hasPrivateKey()) {
            list.add(r.copy());
         }
      }
      return list;
   }

   /**
    * Get a copy of a record identified by address or null if none is found.
    */
   public synchronized Record getRecord(Address address) {
      Record r = getRecordInt(address);
      if (r == null) {
         return null;
      }
      return r.copy();
   }

   /**
    * Get a copy of a single record identified by string address or null if none
    * is found
    */
   public synchronized Record getRecord(String stringAddress) {
      Record r = getRecordInt(stringAddress);
      if (r == null) {
         return null;
      }
      return r.copy();
   }

   /**
    * Add a record
    */
   public synchronized boolean addRecord(Record record) {
      // Make a copy to prevent changes from the outside
      record = record.copy();

      // See if we have a record with that address already
      Record existing = null;
      for (Record r : _records) {
         if (r.address.equals(record.address)) {
            existing = r;
         }
      }

      if (existing == null) {
         // We have a new record, add it
         _records.add(record);
         saveRecords();
         return true;
      }

      // The record has the same address as one we have already
      if (existing.hasPrivateKey() && record.hasPrivateKey()) {
         // Nothing to do as we already have the record with the private key
         return false;
      } else if (!existing.hasPrivateKey() && !record.hasPrivateKey()) {
         // Nothing to do as none of the records have the private key
         return false;
      } else if (!existing.hasPrivateKey() && record.hasPrivateKey()) {
         // We upgrade to a record with a private key
         _records.remove(existing);
         _records.add(record);
         saveRecords();
         return true;
      } else if (existing.hasPrivateKey() && !record.hasPrivateKey()) {
         // The new record does not have a private key, do nothing as we do not
         // do downgrades
         return false;
      }
      // We never get here
      return false;
   }

   public synchronized boolean deleteRecord(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            _records.remove(r);
            if (!hasActiveRecord()) {
               // The last record was deleted, create a new one
               Record newRecord = Record.createRandom(randomSource);
               Toast.makeText(_applicationContext, R.string.created_new_random_key, Toast.LENGTH_LONG).show();
               _records.add(newRecord);
            }
            saveRecords();
            return true;
         }
      }
      return false;
   }

   public synchronized Record forgetPrivateKeyForRecordByAddress(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.forgetPrivateKey();
            saveRecords();
            return r.copy();
         }
      }
      return null;
   }

   public synchronized Record setSourceForRecordByAddress(Address address, Source source) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.source = source;
            saveRecords();
            return r.copy();
         }
      }
      return null;
   }

   public synchronized Record setWalletNameForRecordByAddress(Address address, String walletName) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.walletName = walletName;
            saveRecords();
            return r.copy();
         }
      }
      return null;
   }

   public synchronized Record activateRecordByAddress(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.tag = Tag.ACTIVE;
            saveRecords();
            return r.copy();
         }
      }
      return null;
   }

   public synchronized Record archiveRecordByAddress(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.tag = Tag.ARCHIVE;
            saveRecords();
            return r.copy();
         }
      }
      return null;
   }

   public List<Record> getWeakActiveKeys() {
      List<Record> active = getRecords(Tag.ACTIVE);
      List<Record> weak = new LinkedList<Record>();
      for (Record r : active) {
         if (r.source == Source.VERSION_1 && r.hasPrivateKey()) {
            weak.add(r.copy());
         }
      }
      return weak;
   }

   private boolean hasActiveRecord() {
      for (Record r : _records) {
         if (r.tag == Tag.ACTIVE) {
            return true;
         }
      }
      return false;
   }

   /**
    * Get a record identified by an address or null if none is found
    */
   private Record getRecordInt(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            return r;
         }
      }
      return null;
   }

   /**
    * Get a record identified by a string address or null if none is found
    */
   private Record getRecordInt(String stringAddress) {
      for (Record r : _records) {
         if (r.address.toString().equals(stringAddress)) {
            return r;
         }
      }
      return null;
   }

   private void loadRecords() {
      boolean doSave = false;
      SharedPreferences prefs = _applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE);
      _records = new LinkedList<Record>();

      // Load records
      String records = prefs.getString("records", "");
      for (String one : records.split(",")) {
         one = one.trim();
         if (one.length() == 0) {
            continue;
         }
         Record record = Record.fromSerializedString(one);
         if (record != null) {
            _records.add(record);
         }
      }

      // If no records exist we create a new random one
      if (!hasActiveRecord()) {
         Record newRecord = Record.createRandom(randomSource);
         Toast.makeText(_applicationContext, R.string.created_new_random_key, Toast.LENGTH_LONG).show();
         _records.add(newRecord);
         doSave = true;
      }

      // Sort all records
      Collections.sort(_records);

      // Load default spending address if we have it
      String defaultSpendingAddress = prefs.getString("defaultSpendingAddress", "");
      Record r = getRecordInt(defaultSpendingAddress);
      if (r != null && r.hasPrivateKey()) {
         _defaultSpendingRecord = r;
      }

      // Load the currently selected record
      loadSelected();

      if (doSave) {
         saveRecords();
      }
   }

   private void loadSelected() {
      SharedPreferences prefs = _applicationContext.getSharedPreferences("selected", Context.MODE_PRIVATE);

      boolean save = false;

      // Load selected address
      _selectedAddress = null;
      String lastAddress = prefs.getString("last", null);
      Record record = getRecordInt(lastAddress);
      if (record != null) {
         _selectedAddress = record.address;
      }
      // If we don't have a selected address we automatically select one
      if (_selectedAddress == null) {
         // We do not have a current record, maybe we start up for the first
         // time or the last record was deleted
         // We know that there is always at least one active record, check
         // anyway
         if (getRecords(Tag.ACTIVE).isEmpty()) {
            throw new RuntimeException("There are no active records");
         }
         _selectedAddress = getRecords(Tag.ACTIVE).get(0).address;
         save = true;
      }
      if (save) {
         saveSelected(_applicationContext, _selectedAddress);
      }

   }

   private static void saveSelected(Context context, Address selectedAddress) {
      SharedPreferences prefs = context.getSharedPreferences("selected", Context.MODE_PRIVATE);
      if (prefs.getString("last", "").equals(selectedAddress.toString())) {
         // We already got it, no need to save
         return;
      }
      Editor editor = prefs.edit();
      editor.putString("last", selectedAddress == null ? "" : selectedAddress.toString());
      editor.commit();
   }

   private void saveRecords() {
      SharedPreferences prefs = _applicationContext.getSharedPreferences("data", Context.MODE_PRIVATE);
      Editor editor = prefs.edit();

      // Sort all records
      Collections.sort(_records);

      // Save records
      List<String> records = new LinkedList<String>();
      for (Record record : _records) {
         records.add(record.serialize());
      }
      editor.putString("records", Joiner.on(",").join(records));

      // Update default spending record if necessary
      if (_defaultSpendingRecord != null) {
         // Look it up to see if we still have it. This may set it to null if
         // the record was deleted.
         _defaultSpendingRecord = getRecordInt(_defaultSpendingRecord.address);
      }
      if (_defaultSpendingRecord != null && !_defaultSpendingRecord.hasPrivateKey()) {
         // The default spending record no longer has a private key
         _defaultSpendingRecord = null;
      }
      if (_defaultSpendingRecord == null) {
         // Automatically select a new record (if we have one with a key)
         for (Record r : _records) {
            if (r.hasPrivateKey()) {
               _defaultSpendingRecord = r;
               break;
            }
         }
      }

      // Save address of default spending record
      String defaultSpendingAddress = _defaultSpendingRecord == null ? "" : _defaultSpendingRecord.toString();
      editor.putString("defaultSpendingAddress", defaultSpendingAddress);

      editor.commit();
   }

   public RandomSource getRandomSource() {
      return randomSource;
   }
}

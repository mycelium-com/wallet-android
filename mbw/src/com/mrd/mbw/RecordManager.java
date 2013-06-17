/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbw;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.StringUtils;

public class RecordManager {

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

   public synchronized Record getSelectedRecord() {
      Record record = getRecord(_selectedAddress);
      if (record != null) {
         return record;
      }
      // If we didn't find a record it may have been deleted, select a new one
      if (_defaultSpendingRecord != null) {
         // Use the default spending record
         _selectedAddress = _defaultSpendingRecord.address;
         saveSelected(_applicationContext, _selectedAddress);
      } else {
         // If we have no default spending record, choose the first from the
         // list. We always have at least one as we automatically create one if
         // the last one is deleted
         _selectedAddress = _records.get(0).address;
         saveSelected(_applicationContext, _selectedAddress);
      }
      return getRecord(_selectedAddress);

   }

   public synchronized void setSelectedRecord(Address address) {
      Record r = getRecordInt(address);
      if (r != null) {
         _selectedAddress = r.address;
         saveSelected(_applicationContext, _selectedAddress);
      }
   }

   /**
    * Get a copy of all the records
    */
   public synchronized List<Record> getRecords() {
      List<Record> list = new ArrayList<Record>(_records.size());
      for (Record r : _records) {
         list.add(new Record(r.key, r.address, r.timestamp));
      }
      return list;
   }

   /**
    * Get a list of records with private keys
    */
   public synchronized List<Record> getRecordsWithPrivateKeys() {
      List<Record> list = new ArrayList<Record>(_records.size());
      for (Record r : _records) {
         if (r.hasPrivateKey()) {
            list.add(new Record(r.key, r.address, r.timestamp));
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
      return new Record(r.key, r.address, r.timestamp);
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
      return new Record(r.key, r.address, r.timestamp);
   }

   /**
    * Get the record we use for spending by default, or null if no record exists
    * with a private keys
    */
   public Record getDefaultSpendingRecord() {
      return getRecord(_defaultSpendingRecord.address);
   }

   /**
    * Add a record
    */
   public synchronized boolean addRecord(Record record) {
      // Make a copy to prevent changes from the outside
      record = new Record(record.key, record.address, record.timestamp);

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
            if (_records.isEmpty()) {
               // The last record was deleted, create a new one
               Record newRecord = new Record(new InMemoryPrivateKey(new SecureRandom(), true), System.currentTimeMillis());
               Toast.makeText(_applicationContext, R.string.created_new_random_key, Toast.LENGTH_LONG).show();
               _records.add(newRecord);
            }
            saveRecords();
            return true;
         }
      }
      return false;
   }

   public synchronized Record forgetPrivateKeyForRcordByAddress(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            r.forgetPrivateKey();
            saveRecords();
            return new Record(r.key, r.address, r.timestamp);
         }
      }
      return null;
   }

   /**
    * Get a record identified by an address or null if none is found
    */
   private Record getRecordInt(Address address) {
      for (Record r : _records) {
         if (r.address.equals(address)) {
            return new Record(r.key, r.address, r.timestamp);
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
            return new Record(r.key, r.address, r.timestamp);
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
      if (_records.isEmpty()) {
         Record newRecord = new Record(new InMemoryPrivateKey(new SecureRandom(), true), System.currentTimeMillis());
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
      String lastAddress = prefs.getString("last", "");
      Record record = getRecordInt(lastAddress);
      if (record != null) {
         _selectedAddress = record.address;
      }

      // If we don't have a selected address we automatically select one
      if (_selectedAddress == null) {
         // We do not have a current record, maybe we start up for the first
         // time or the last record was deleted
         if (_defaultSpendingRecord != null) {
            // Use the default spending record
            _selectedAddress = _defaultSpendingRecord.address;
            saveSelected(_applicationContext, _selectedAddress);
         } else if (_records.size() != 0) {
            // Alternatively use the first one on the list
            _selectedAddress = _records.get(0).address;
            saveSelected(_applicationContext, _selectedAddress);
         } else {
            // We have no records to select, this should not happen as we always
            // create one if necessary
         }
      }

   }

   private static void saveSelected(Context context, Address selected) {
      SharedPreferences prefs = context.getSharedPreferences("selected", Context.MODE_PRIVATE);
      if (prefs.getString("last", "").equals(selected.toString())) {
         // We already got it, no need to save
         return;
      }
      Editor editor = prefs.edit();
      editor.putString("last", selected == null ? "" : selected.toString());
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
      editor.putString("records", StringUtils.join(records, ","));

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

}

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

package com.mycelium.wallet.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.ApiObject;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mycelium.wallet.api.ApiCache.TransactionInventory.Item;

public class AndroidApiCache extends ApiCache {

   private static final String TABLE_TRANSACTION_SUMMARY = "ts";
   private static final String TABLE_TRANSACTION_INVENTORY = "tinv";
   private static final int MAX_TRANSACTION_INVENTORIES = 30;

   private class OpenHelper extends SQLiteOpenHelper {

      private static final String DATABASE_NAME = "cache.db";
      private static final int DATABASE_VERSION = 6;

      public OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("create table " + TABLE_TRANSACTION_SUMMARY + " (key text primary key, value text);");
         db.execSQL("create table " + TABLE_TRANSACTION_INVENTORY + " (key text primary key, value text);");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION_SUMMARY);
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION_INVENTORY);
         onCreate(db);
      }
   }

   private OpenHelper _openHelper;
   private SQLiteDatabase _database;

   public AndroidApiCache(Context context) {
      _openHelper = new OpenHelper(context);
      _database = _openHelper.getWritableDatabase();

   }

   @Override
   public void close() {
      _openHelper.close();
   }

   @Override
   public QueryTransactionSummaryResponse getTransactionSummaryList(Collection<Address> addresses) {
      TransactionInventory inv = getTransactionInventory(addresses);
      if (inv == null) {
         return null;
      }
      List<TransactionSummary> transactions = new LinkedList<TransactionSummary>();
      for (Item item : inv.transactions) {
         TransactionSummary summary = getTransactionSummary(item.hash.toString());
         if (summary == null) {
            return null;
         }
         transactions.add(summary);
      }
      return new QueryTransactionSummaryResponse(transactions, inv.chainHeight);
   }

   @Override
   TransactionSummary getTransactionSummary(String txHash) {
      String value = get(TABLE_TRANSACTION_SUMMARY, txHash);
      if (value == null) {
         return null;
      }
      byte[] bytes = HexUtils.toBytes(value);
      try {
         return ApiObject.deserialize(TransactionSummary.class, new ByteReader(bytes));
      } catch (ApiException e) {
         // Something is wrong with the serialization, delete the cache entry
         delete(TABLE_TRANSACTION_SUMMARY, txHash);
         return null;
      }
   }

   @Override
   void addTransactionSummary(TransactionSummary transaction) {
      String txHash = transaction.hash.toString();
      set(TABLE_TRANSACTION_SUMMARY, txHash, transactionSummaryToHex(transaction));
   }

   /**
    * Calculate a key that uniquely identifies a collection of addresses
    */
   private static final String getAddressCollectionKey(Collection<Address> addresses) {
      List<Address> list = new ArrayList<Address>(addresses);
      Collections.sort(list);
      byte[] toHash = new byte[Address.NUM_ADDRESS_BYTES * list.size()];
      for (int i = 0; i < list.size(); i++) {
         byte[] addressBytes = list.get(i).getAllAddressBytes();
         System.arraycopy(addressBytes, 0, toHash, i * Address.NUM_ADDRESS_BYTES, Address.NUM_ADDRESS_BYTES);
      }

      return HexUtils.toHex(HashUtils.sha256(toHash));
   }

   @Override
   TransactionInventory getTransactionInventory(Collection<Address> addresses) {
      String key = getAddressCollectionKey(addresses);
      String string = get(TABLE_TRANSACTION_INVENTORY, key);
      if (string == null) {
         return null;
      }

      // Hex to bytes
      byte[] bytes;
      try {
         bytes = HexUtils.toBytes(string);
      } catch (RuntimeException e) {
         return null;
      }

      // Parse
      TransactionInventory inv;
      try {
         inv = new TransactionInventory(new ByteReader(bytes));
      } catch (InsufficientBytesException e) {
         return null;
      }

      return inv;
   }

   @Override
   void setTransactionInventory(Collection<Address> addresses, TransactionInventory inv) {
      // A new inventory is stored for every unique address collection.
      // We need to make sure that we don't store too many inventories, so when
      // we reach the limit we delete a random entry
      int size = count(TABLE_TRANSACTION_INVENTORY);
      if (size > MAX_TRANSACTION_INVENTORIES) {
         // We are above the maximum, pick a random key to delete
         List<String> keys = getKeys(TABLE_TRANSACTION_INVENTORY);
         String randomKey = keys.get(new Random().nextInt(keys.size()));
         delete(TABLE_TRANSACTION_INVENTORY, randomKey);
      }

      String key = getAddressCollectionKey(addresses);
      byte[] bytes = inv.toByteWriter(new ByteWriter(2048)).toBytes();
      String hex = HexUtils.toHex(bytes);
      set(TABLE_TRANSACTION_INVENTORY, key, hex);
   }

   private String transactionSummaryToHex(TransactionSummary transaction) {
      ByteWriter writer = new ByteWriter(512);
      transaction.serialize(writer);
      return HexUtils.toHex(writer.toBytes());
   }

   private String get(String table, String key) {
      Cursor cursor = null;
      try {
         cursor = _database.query(table, new String[] { "value" }, "key = \"" + key + "\"", null, null, null, null);
         if (!cursor.moveToFirst()) {
            return null;
         }
         String value = cursor.getString(0);
         return value;
      } catch (Exception e) {
         return null;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   private List<String> getKeys(String table) {
      List<String> keys = new LinkedList<String>();
      Cursor cursor = null;
      try {
         cursor = _database.query(table, new String[] { "key" }, null, null, null, null, null);
         while (cursor.moveToNext()) {
            String key = cursor.getString(0);
            keys.add(key);
         }
         return keys;
      } catch (Exception e) {
         // XXX: Use Andreas' error reporting
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
      return keys;
   }

   private void set(String table, String key, String value) {
      String old = get(table, key);
      if (old == null) {
         // Insert
         ContentValues values = new ContentValues();
         values.put("key", key);
         values.put("value", value);
         _database.insert(table, null, values);
      } else if (!old.equals(value)) {
         // Update
         ContentValues values = new ContentValues();
         values.put("value", value);
         _database.update(table, values, "key = \"" + key + "\"", null);
      } else {
         // No change
      }
   }

   private void delete(String table, String key) {
      _database.execSQL("DELETE FROM " + table + " where key = \"" + key + "\"");
   }

   private int count(String table) {
      Cursor cursor = _database.rawQuery("SELECT COUNT(*) FROM " + table, new String[] {});
      if (cursor.moveToNext()) {
         return (int) cursor.getLong(0);
      }
      return -1;
   }

}

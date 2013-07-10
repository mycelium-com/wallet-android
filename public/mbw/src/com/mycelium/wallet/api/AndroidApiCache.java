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

package com.mycelium.wallet.api;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wallet.api.ApiCache.TransactionInventory.Item;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.ApiObject;
import com.mrd.mbwapi.api.Balance;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;

public class AndroidApiCache extends ApiCache {

   private static final String TABLE_BALANCE = "balance";
   private static final String TABLE_TRANSACTION_SUMMARY = "ts";
   private static final String TABLE_TRANSACTION_INVENTORY = "tinv";

   private class OpenHelper extends SQLiteOpenHelper {

      private static final String DATABASE_NAME = "cache.db";
      private static final int DATABASE_VERSION = 6;

      public OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("create table " + TABLE_BALANCE + " (key text primary key, value text);");
         db.execSQL("create table " + TABLE_TRANSACTION_SUMMARY + " (key text primary key, value text);");
         db.execSQL("create table " + TABLE_TRANSACTION_INVENTORY + " (key text primary key, value text);");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         db.execSQL("DROP TABLE IF EXISTS " + TABLE_BALANCE);
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
   public Balance getBalance(Address address) {
      String value = get(TABLE_BALANCE, address.toString());
      if (value == null) {
         return null;
      }
      byte[] bytes = HexUtils.toBytes(value);
      try {
         return ApiObject.deserialize(Balance.class, new ByteReader(bytes));
      } catch (ApiException e) {
         // Something is wrong with the serialization, delete the cache entry
         delete(TABLE_BALANCE, address.toString());
         return null;
      }
   }

   @Override
   public QueryTransactionSummaryResponse getTransactionSummaryList(Address address) {
      TransactionInventory inv = getTransactionInventory(address);
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
   void setBalance(Address address, Balance balance) {
      set(TABLE_BALANCE, address.toString(), balanceToHex(balance));
   }

   private String balanceToHex(Balance balance) {
      ByteWriter writer = new ByteWriter(512);
      balance.serialize(writer);
      return HexUtils.toHex(writer.toBytes());
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

   @Override
   TransactionInventory getTransactionInventory(Address address) {
      String string = get(TABLE_TRANSACTION_INVENTORY, address.toString());
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
   void setTransactionInventory(Address address, TransactionInventory inv) {
      byte[] bytes = inv.toByteWriter(new ByteWriter(2048)).toBytes();
      String hex = HexUtils.toHex(bytes);
      set(TABLE_TRANSACTION_INVENTORY, address.toString(), hex);
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
      _database.execSQL("DLETE FROM " + table + " where key = \"" + key + "\"");
   }

}

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

package com.mycelium.wallet.persistence;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.SourcedTransactionOutput;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbwapi.api.AddressOutputState;

public class TxOutDb {

   private static final String LOG_TAG = "TxOutDb";

   private static final String TABLE_CONFIRMED = "confirmed";
   private static final String TABLE_RECEIVING = "receiving";
   private static final String TABLE_SENDING = "sending";
   private static final String TABLE_ADDRESS = "address";

   private class OpenHelper extends SQLiteOpenHelper {

      private static final String DATABASE_NAME = "txout.db";
      private static final int DATABASE_VERSION = 8;

      public OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      // @formatter:off
      //
      // Table unspent:
      // - text outpoint (primary key,
      // <hex-dump-of-transaction-hash>:<hex-dump-of-two-byte-index>)
      // - text address (index)
      // - integer height (block chain height)
      // - integer value (value measured in satoshis)
      // - integer coinbase (if 0 this output is not from a coinbase
      // transaction)
      // - blob script (transaction script)
      //
      // @formatter:on

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE confirmed (outpoint BLOB PRIMARY KEY, address BLOB, height INTEGER, value INTEGER, isCoinBase INTEGER, script BLOB);");
         db.execSQL("CREATE INDEX confirmedIndex ON confirmed (address);");
         db.execSQL("CREATE TABLE receiving (outpoint BLOB PRIMARY KEY, address BLOB, value INTEGER, senders BLOB, script BLOB);");
         db.execSQL("CREATE INDEX receivingIndex ON receiving (address);");
         db.execSQL("CREATE TABLE sending (outpoint BLOB PRIMARY KEY, address BLOB, height INTEGER, value INTEGER, isCoinBase INTEGER, script BLOB);");
         db.execSQL("CREATE INDEX sendingIndex ON sending (address);");
         db.execSQL("CREATE TABLE address (address BLOB PRIMARY KEY, time INTEGER);");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         db.execSQL("DROP TABLE IF EXISTS confirmed");
         db.execSQL("DROP INDEX IF EXISTS confirmedIndex");
         db.execSQL("DROP TABLE IF EXISTS receiving");
         db.execSQL("DROP INDEX IF EXISTS receivingIndex");
         db.execSQL("DROP TABLE IF EXISTS sending");
         db.execSQL("DROP INDEX IF EXISTS sendingIndex");
         db.execSQL("DROP TABLE IF EXISTS address");
         onCreate(db);
      }
   }

   private OpenHelper _openHelper;
   private SQLiteDatabase _database;
   private SQLiteStatement _insertOrReplaceConfirmed;
   private SQLiteStatement _deleteConfirmed;
   private SQLiteStatement _insertOrReplaceReceiving;
   private SQLiteStatement _deleteReceiving;
   private SQLiteStatement _insertOrReplaceSending;
   private SQLiteStatement _deleteSending;
   private SQLiteStatement _insertOrReplaceAddress;

   public TxOutDb(Context context) {
      _openHelper = new OpenHelper(context);
      _database = _openHelper.getWritableDatabase();
      _insertOrReplaceConfirmed = _database.compileStatement("INSERT OR REPLACE INTO confirmed VALUES (?,?,?,?,?,?)");
      _insertOrReplaceReceiving = _database.compileStatement("INSERT OR REPLACE INTO receiving VALUES (?,?,?,?,?)");
      _insertOrReplaceSending = _database.compileStatement("INSERT OR REPLACE INTO sending VALUES (?,?,?,?,?,?)");
      _insertOrReplaceAddress = _database.compileStatement("INSERT OR REPLACE INTO address VALUES (?,?)");
      _deleteConfirmed = _database.compileStatement("DELETE FROM confirmed WHERE outpoint = ?");
      _deleteReceiving = _database.compileStatement("DELETE FROM receiving WHERE outpoint = ?");
      _deleteSending = _database.compileStatement("DELETE FROM sending WHERE outpoint = ?");
   }

   public void close() {
      _openHelper.close();
   }

   public Set<PersistedOutput> getConfirmedByAddress(Address address) {
      Set<PersistedOutput> result = new HashSet<PersistedOutput>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_CONFIRMED, new String[] { "outpoint", "height", "value", "isCoinBase",
               "script" }, "address = ?", null, null, null, null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            int height = cursor.getInt(1);
            long value = cursor.getLong(2);
            boolean isCoinBase = cursor.getInt(3) != 0;
            byte[] script = cursor.getBlob(4);
            PersistedOutput confirmed = new PersistedOutput(outPoint, address, height, value, script, isCoinBase);
            result.add(confirmed);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getConfirmedByAddress", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public PersistedOutput getConfirmed(OutPoint outPoint) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, outPointToBytes(outPoint));
         cursor = blobQuery.query(false, TABLE_CONFIRMED, new String[] { "address", "height", "value", "isCoinBase",
               "script" }, "outpoint = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            Address address = new Address(cursor.getBlob(0));
            int height = cursor.getInt(1);
            long value = cursor.getLong(2);
            boolean isCoinBase = cursor.getInt(3) != 0;
            byte[] script = cursor.getBlob(4);
            PersistedOutput confirmed = new PersistedOutput(outPoint, address, height, value, script, isCoinBase);
            return confirmed;
         }
         return null;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getConfirmed", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public Set<SourcedTransactionOutput> getReceivingByAddress(Address address) {
      Set<SourcedTransactionOutput> result = new HashSet<SourcedTransactionOutput>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_RECEIVING, new String[] { "outpoint", "value", "senders", "script" },
               "address = ?", null, null, null, null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            long value = cursor.getLong(1);
            byte[] senderBytes = cursor.getBlob(2);
            byte[] script = cursor.getBlob(3);

            Set<Address> senders = new HashSet<Address>();
            ByteReader reader = new ByteReader(senderBytes);
            while (reader.available() > 0) {
               senders.add(new Address(reader.getBytes(Address.NUM_ADDRESS_BYTES)));
            }
            SourcedTransactionOutput receiving = new SourcedTransactionOutput(outPoint, value, address, senders, script);
            result.add(receiving);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getReceivingByAddress", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public SourcedTransactionOutput getReceiving(OutPoint outPoint) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, outPointToBytes(outPoint));
         cursor = blobQuery.query(false, TABLE_RECEIVING, new String[] { "outpoint", "value", "senders", "script" },
               "address = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            Address address = new Address(cursor.getBlob(0));
            long value = cursor.getLong(1);
            byte[] senderBytes = cursor.getBlob(2);
            byte[] script = cursor.getBlob(3);

            Set<Address> senders = new HashSet<Address>();
            ByteReader reader = new ByteReader(senderBytes);
            while (reader.available() > 0) {
               senders.add(new Address(reader.getBytes(Address.NUM_ADDRESS_BYTES)));
            }
            SourcedTransactionOutput receiving = new SourcedTransactionOutput(outPoint, value, address, senders, script);
            return receiving;
         }
         return null;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getReceiving", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public Set<PersistedOutput> getSendingByAddress(Address address) {

      Set<PersistedOutput> result = new HashSet<PersistedOutput>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_SENDING, new String[] { "outpoint", "height", "value", "isCoinBase",
               "script" }, "address = ?", null, null, null, null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            int height = cursor.getInt(1);
            long value = cursor.getLong(2);
            boolean isCoinBase = cursor.getInt(3) != 0;
            byte[] script = cursor.getBlob(4);
            PersistedOutput confirmed = new PersistedOutput(outPoint, address, height, value, script, isCoinBase);
            result.add(confirmed);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getSendingByAddress", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public AddressOutputState getAddressOutputState(Address address) {
      Set<OutPoint> confirmed = getConfirmedOutputState(address);
      Set<OutPoint> receiving = getReceivingOutputState(address);
      Set<OutPoint> sending = getSendingOutputState(address);
      AddressOutputState state = new AddressOutputState(address, confirmed, receiving, sending);
      return state;
   }

   public Set<OutPoint> getConfirmedOutputState(Address address) {
      Set<OutPoint> result = new HashSet<OutPoint>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_CONFIRMED, new String[] { "outpoint" }, "address = ?", null, null, null,
               null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            result.add(outPoint);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getConfirmedOutputState", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public Set<OutPoint> getReceivingOutputState(Address address) {
      Set<OutPoint> result = new HashSet<OutPoint>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_RECEIVING, new String[] { "outpoint" }, "address = ?", null, null, null,
               null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            result.add(outPoint);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getReceivingOutputState", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public Set<OutPoint> getSendingOutputState(Address address) {
      Set<OutPoint> result = new HashSet<OutPoint>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_SENDING, new String[] { "outpoint" }, "address = ?", null, null, null,
               null, null);
         while (cursor.moveToNext()) {
            OutPoint outPoint = bytesToOutPoint(cursor.getBlob(0));
            result.add(outPoint);
         }
         return result;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getSendingOutputState", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public long getUpdateTimeForAddress(Address address) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, address.getAllAddressBytes());
         cursor = blobQuery.query(false, TABLE_ADDRESS, new String[] { "time" }, "address = ?", null, null, null,
               null, null);
         if (cursor.moveToNext()) {
            return cursor.getLong(0);
         }
         return Long.MIN_VALUE;
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in getUpdateTimeForAddress", e);
         throw new RuntimeException(e);
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public synchronized void insertOrReplaceAddress(Address address, long time) {
      try {
         _insertOrReplaceAddress.bindBlob(1, address.getAllAddressBytes());
         _insertOrReplaceAddress.bindLong(2, time);
         _insertOrReplaceAddress.executeInsert();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in insertOrReplaceAddress", e);
         throw new RuntimeException(e);
      }
   }

   public synchronized void insertOrReplaceConfirmed(PersistedOutput output) {
      try {
         _insertOrReplaceConfirmed.bindBlob(1, outPointToBytes(output.outPoint));
         _insertOrReplaceConfirmed.bindBlob(2, output.address.getAllAddressBytes());
         _insertOrReplaceConfirmed.bindLong(3, output.height);
         _insertOrReplaceConfirmed.bindLong(4, output.value);
         _insertOrReplaceConfirmed.bindLong(5, output.isCoinBase ? 1 : 0);
         _insertOrReplaceConfirmed.bindBlob(6, output.script);
         _insertOrReplaceConfirmed.executeInsert();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in insertOrReplaceConfirmed", e);
         throw new RuntimeException(e);
      }
   }

   public synchronized void insertOrReplaceReceiving(SourcedTransactionOutput output) {
      try {
         _insertOrReplaceReceiving.bindBlob(1, outPointToBytes(output.outPoint));
         _insertOrReplaceReceiving.bindBlob(2, output.address.getAllAddressBytes());
         _insertOrReplaceReceiving.bindLong(3, output.value);
         ByteWriter writer = new ByteWriter(output.senders.size() * Address.NUM_ADDRESS_BYTES);
         for (Address sender : output.senders) {
            writer.putBytes(sender.getAllAddressBytes());
         }
         _insertOrReplaceReceiving.bindBlob(4, writer.toBytes());
         _insertOrReplaceReceiving.bindBlob(5, output.script);
         _insertOrReplaceReceiving.executeInsert();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in insertOrReplaceReceiving", e);
         throw new RuntimeException(e);
      }
   }

   public synchronized void insertOrReplaceSending(PersistedOutput output) {
      try {
         _insertOrReplaceSending.bindBlob(1, outPointToBytes(output.outPoint));
         _insertOrReplaceSending.bindBlob(2, output.address.getAllAddressBytes());
         _insertOrReplaceSending.bindLong(3, output.height);
         _insertOrReplaceSending.bindLong(4, output.value);
         _insertOrReplaceSending.bindLong(5, output.isCoinBase ? 1 : 0);
         _insertOrReplaceSending.bindBlob(6, output.script);
         _insertOrReplaceSending.executeInsert();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in insertOrReplaceSending", e);
         throw new RuntimeException(e);
      }
   }

   @SuppressWarnings("unused")
   private long countConfirmed() {
      Cursor cursor = _database.rawQuery("SELECT COUNT(*) FROM confirmed", new String[] {});
      if (cursor.moveToNext()) {
         return cursor.getLong(0);
      }
      return -1;
   }

   @SuppressWarnings("unused")
   private long countReceiving() {
      Cursor cursor = _database.rawQuery("SELECT COUNT(*) FROM receiving", new String[] {});
      if (cursor.moveToNext()) {
         return cursor.getLong(0);
      }
      return -1;
   }

   @SuppressWarnings("unused")
   private long countSending() {
      Cursor cursor = _database.rawQuery("SELECT COUNT(*) FROM sending", new String[] {});
      if (cursor.moveToNext()) {
         return cursor.getLong(0);
      }
      return -1;
   }

   public synchronized void deleteConfirmed(OutPoint outPoint) {
      try {
         _deleteConfirmed.bindBlob(1, outPointToBytes(outPoint));
         _deleteConfirmed.execute();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in deleteConfirmed", e);
         throw new RuntimeException(e);
      }
   }

   public synchronized void deleteReceiving(OutPoint outPoint) {
      try {
         _deleteReceiving.bindBlob(1, outPointToBytes(outPoint));
         _deleteReceiving.execute();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in deleteReceiving", e);
         throw new RuntimeException(e);
      }
   }

   public synchronized void deleteSending(OutPoint outPoint) {
      try {
         _deleteSending.bindBlob(1, outPointToBytes(outPoint));
         _deleteSending.execute();
      } catch (Exception e) {
         Log.e(LOG_TAG, "Exception in deleteSending", e);
         throw new RuntimeException(e);
      }
   }

   private byte[] outPointToBytes(OutPoint outPoint) {
      byte[] bytes = new byte[34];
      System.arraycopy(outPoint.hash.getBytes(), 0, bytes, 0, Sha256Hash.HASH_LENGTH);
      bytes[32] = (byte) (outPoint.index & 0xFF);
      bytes[33] = (byte) ((outPoint.index >> 8) & 0xFF);
      return bytes;
   }

   private OutPoint bytesToOutPoint(byte[] bytes) {
      Sha256Hash hash = new Sha256Hash(bytes, 0, false);
      int index = ((bytes[32] & 0xFF) << 0) | ((bytes[33] & 0xFF) << 8);
      return new OutPoint(hash, index);
   }

}

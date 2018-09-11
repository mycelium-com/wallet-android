/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.Sha256Hash;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;

// TODO: 31.08.18 Document what exactly is the purpose of this class. Is it not a performance overhead in most cases to use it?
public class SQLiteQueryWithBlobs {
   private static class GenericIndex<T> {
      public T value;
      public int index;

      public GenericIndex(T blob, int index) {
         this.value = blob;
         this.index = index;
      }
   }

   private static class BlobIndex  extends  GenericIndex<byte[]>{
      public BlobIndex(byte[] blob, int index) {
         super(blob, index);
      }
   }

   private static class LongIndex  extends  GenericIndex<Long>{
      public LongIndex(Long blob, int index) {
         super(blob, index);
      }
   }

   private class MyCursorFactory implements CursorFactory {
      @Override
      public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
         for (BlobIndex blobIndex : _blobIndex) {
            query.bindBlob(blobIndex.index, blobIndex.value);
         }
         for (LongIndex longIndex : _longIndex) {
            query.bindLong(longIndex.index, longIndex.value);
         }
         return new SQLiteCursor(masterQuery, editTable, query);
      }
   }

   private SQLiteDatabase _db;
   private List<BlobIndex> _blobIndex;
   private List<LongIndex> _longIndex;
   MyCursorFactory _cursorFactory;

   public SQLiteQueryWithBlobs(SQLiteDatabase db) {
      _db = db;
      _blobIndex = new LinkedList<>();
      _longIndex = new LinkedList<>();
      _cursorFactory = new MyCursorFactory();
   }

   public void bindBlob(int index, byte[] blob) {
      _blobIndex.add(new BlobIndex(blob, index));
   }
   public void bindLong(int index, Long value) {
      _longIndex.add(new LongIndex(value, index));
   }

   public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
         String groupBy, String having, String orderBy, String limit) {
      return _db.queryWithFactory(_cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having,
            orderBy, limit);
   }
   
   public Cursor raw(String sql, String table){
      return _db.rawQueryWithFactory(_cursorFactory, sql, null, table);
   }
   
   public static byte[] uuidToBytes(UUID id) {
      byte[] b = new byte[16];
      BitUtils.uint64ToByteArrayLE(id.getMostSignificantBits(), b, 0);
      BitUtils.uint64ToByteArrayLE(id.getLeastSignificantBits(), b, 8);
      return b;
   }

   public static UUID uuidFromBytes(byte[] b) {
      Preconditions.checkArgument(b.length == 16);
      return new UUID(BitUtils.uint64ToLong(b, 0), BitUtils.uint64ToLong(b, 8));
   }

   public static byte[] outPointToBytes(OutPoint outPoint) {
      byte[] bytes = new byte[34];
      System.arraycopy(outPoint.txid.getBytes(), 0, bytes, 0, Sha256Hash.HASH_LENGTH);
      bytes[32] = (byte) (outPoint.index & 0xFF);
      bytes[33] = (byte) ((outPoint.index >> 8) & 0xFF);
      return bytes;
   }

   public static OutPoint outPointFromBytes(byte[] bytes) {
      Preconditions.checkArgument(bytes != null && bytes.length == 34);
      Sha256Hash hash = Sha256Hash.copyOf(bytes, 0);
      int index = (bytes[32] & 0xFF) | ((bytes[33] & 0xFF) << 8);
      return new OutPoint(hash, index);
   }
   
   public static void bindBlobWithNull(SQLiteStatement statement, int index, byte[] value){
      if(value == null) {
         statement.bindNull(index);
      } else {
         statement.bindBlob(index, value);
      }
   }
}

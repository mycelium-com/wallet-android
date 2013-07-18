package com.mycelium.wallet.persistence;

import java.util.LinkedList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;

public class SQLiteQueryWithBlobs {

   private static class BlobIndex {
      public byte[] blob;
      public int index;

      public BlobIndex(byte[] blob, int index) {
         this.blob = blob;
         this.index = index;
      }
   }

   private class MyCursorFactory implements CursorFactory {

      @Override
      public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
         for (BlobIndex blobIndex : _blobIndex) {
            query.bindBlob(blobIndex.index, blobIndex.blob);
         }
         return new SQLiteCursor(db, masterQuery, editTable, query);
      }

   }

   private SQLiteDatabase _db;
   private List<BlobIndex> _blobIndex;
   MyCursorFactory _cursorFactory;

   public SQLiteQueryWithBlobs(SQLiteDatabase db) {
      _db = db;
      _blobIndex = new LinkedList<BlobIndex>();
      _cursorFactory = new MyCursorFactory();
   }

   public void bindBlob(int index, byte[] blob) {
      _blobIndex.add(new BlobIndex(blob, index));
   }

   public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
         String groupBy, String having, String orderBy, String limit) {
      return _db.queryWithFactory(_cursorFactory, distinct, table, columns, selection, selectionArgs, groupBy, having,
            orderBy, limit);
   }
   
   public Cursor raw(String sql, String table){
      return _db.rawQueryWithFactory(_cursorFactory, sql, null, table);
   }
}

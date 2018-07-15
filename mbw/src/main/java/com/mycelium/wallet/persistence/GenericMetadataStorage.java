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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import com.google.common.base.Optional;


import java.util.*;

/**
 * Most of Mycelium's meta data is stored this way.
 */
class GenericMetadataStorage {
   // legacy table names, only to be deleted
   private static final String TABLE_ACCOUNT_LABELS = "accountlabels";
   private static final String TABLE_BACKUP_STATUS = "backupstatus";
   private static final String TABLE_TRANSACTION_LABELS = "transactionlabels";
   // the actual and only table
   private static final String TABLE_KEY_VALUE_STORE = "keyValueStore";
   private final SQLiteStatement getKeyCategoryQuery;

   private class OpenHelper extends SQLiteOpenHelper {
      private static final String DATABASE_NAME = "mds.db";
      private static final int DATABASE_VERSION = 3;

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE " + TABLE_KEY_VALUE_STORE + " (key TEXT, category TEXT, value TEXT, PRIMARY KEY (key, category) );");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         if (oldVersion < 3) {
            db.execSQL("DROP TABLE " + TABLE_ACCOUNT_LABELS + ";");
            db.execSQL("DROP TABLE " + TABLE_BACKUP_STATUS + ";");
            db.execSQL("DROP TABLE " + TABLE_TRANSACTION_LABELS + ";");
            db.execSQL("CREATE TABLE " + TABLE_KEY_VALUE_STORE + " (key TEXT, category TEXT, value TEXT, PRIMARY KEY (key, category) );");
         }
      }
   }

   private SQLiteDatabase _db;
   private SQLiteStatement _insertOrReplaceKeyValueEntry;

   GenericMetadataStorage(Context context) {
      OpenHelper openHelper = new OpenHelper(context);
      _db = openHelper.getWritableDatabase();
      _insertOrReplaceKeyValueEntry = _db.compileStatement("INSERT OR REPLACE INTO " + TABLE_KEY_VALUE_STORE + " VALUES (?,?,?)");
      getKeyCategoryQuery = _db.compileStatement("SELECT value FROM " + TABLE_KEY_VALUE_STORE + " WHERE (key = ? and category = ?) LIMIT 1");
   }

   void storeKeyCategoryValueEntry(final MetadataKeyCategory keyCategory, final String value){
      storeKeyCategoryValueEntry(keyCategory.key, keyCategory.category, value);
   }

   private void storeKeyCategoryValueEntry(final String key, final String category, final String value){
      _insertOrReplaceKeyValueEntry.bindString(1, key);
      _insertOrReplaceKeyValueEntry.bindString(2, category);
      _insertOrReplaceKeyValueEntry.bindString(3, value);
      _insertOrReplaceKeyValueEntry.executeInsert();
   }

   private String getKeyValueEntry(final String key, final String defaultValue){
      return getKeyCategoryValueEntry(key, "", defaultValue);
   }

   private String getKeyCategoryValueEntry(final String key, final String category, final String defaultValue){
      Optional<String> ret = getKeyCategoryValueEntry(key, category);
      if (ret.isPresent()) {
         return ret.get();
      }else{
         return defaultValue;
      }
   }

   String getKeyCategoryValueEntry(final MetadataKeyCategory keyCategory, final String defaultValue){
      return getKeyCategoryValueEntry(keyCategory.key, keyCategory.category, defaultValue);
   }

   Optional<String> getKeyCategoryValueEntry(final MetadataKeyCategory keyCategory){
      return getKeyCategoryValueEntry(keyCategory.key, keyCategory.category);
   }

   private synchronized Optional<String> getKeyCategoryValueEntry(final String key, final String category){
      try {
         getKeyCategoryQuery.bindString(1, key);
         getKeyCategoryQuery.bindString(2, category);
         return Optional.of(getKeyCategoryQuery.simpleQueryForString());
      } catch (SQLiteDoneException ignore){
         return Optional.absent();
      }
   }

   void deleteByKeyCategory(final MetadataKeyCategory keyCategory){
      deleteByKeyCategory(keyCategory.key, keyCategory.category);
   }

   private void deleteByKeyCategory(final String key, final String category){
      _db.delete(TABLE_KEY_VALUE_STORE, "key = ? and category = ?", new String[]{key, category});
   }

   void deleteAllByKey(final String key){
      _db.delete(TABLE_KEY_VALUE_STORE, "key = ?", new String[]{key});
   }

   Map<String, String> getKeysAndValuesByCategory(final MetadataCategory category){
      return getKeysAndValuesByCategory(category.category);
   }

   private Map<String, String> getKeysAndValuesByCategory(final String category){
      Cursor cursor = null;
      try {
         Map<String, String> entries = new HashMap<String, String>();
         cursor = _db.query(false, TABLE_KEY_VALUE_STORE, new String[]{"key", "value"}, " category = ?", new String[]{category}, null, null, null, null);
         while (cursor.moveToNext()) {
            entries.put(cursor.getString(0), cursor.getString(1));
         }
         return entries;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   Optional<String> getFirstKeyForCategoryValue(final MetadataCategory category, final String value){
      return getFirstKeyForCategoryValue(category.category, value);
   }

   private Optional<String> getFirstKeyForCategoryValue(final String category, final String value){
      Cursor cursor = null;
      try {
         cursor = _db.query(false, TABLE_KEY_VALUE_STORE, new String[]{"key"}, " value = ? and category = ?", new String[]{value, category}, null, null, null, "1");
         if (cursor.moveToNext()) {
            return Optional.of(cursor.getString(0));
         }
         return Optional.absent();
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   private void storeKeyValueEntry(final String key, final String value) {
      storeKeyCategoryValueEntry(key, "", value);
   }
}

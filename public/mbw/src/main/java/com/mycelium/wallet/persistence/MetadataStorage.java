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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import com.google.common.base.Optional;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;

import java.nio.ByteBuffer;
import java.util.UUID;

public class MetadataStorage {

   private static final String TABLE_ACCOUNT_LABELS = "accountlabels";
   private static final String TABLE_BACKUP_STATUS = "backupstatus";
   private static final String TABLE_TRANSACTION_LABELS = "transactionlabels";
   private static final byte[] MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990");

   private class OpenHelper extends SQLiteOpenHelper {

      private static final String DATABASE_NAME = "mds.db";
      private static final int DATABASE_VERSION = 1;

      public OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE accountlabels (id BLOB PRIMARY KEY, label TEXT);");
         db.execSQL("CREATE TABLE backupstatus (id BLOB PRIMARY KEY, status INTEGER);");
         db.execSQL("CREATE TABLE transactionlabels  (id BLOB PRIMARY KEY, label TEXT);");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         throw new RuntimeException("Database upgrade not supported");
      }
   }

   private OpenHelper _openHelper;
   private SQLiteDatabase _db;
   private SQLiteStatement _insertOrReplaceAccountLabel;
   private SQLiteStatement _insertOrReplaceBackupData;
   private SQLiteStatement _insertOrReplaceTransaction;

   public MetadataStorage(Context context) {
      _openHelper = new OpenHelper(context);
      _db = _openHelper.getWritableDatabase();
      _insertOrReplaceAccountLabel = _db.compileStatement("INSERT OR REPLACE INTO accountlabels VALUES (?,?)");
      _insertOrReplaceBackupData = _db.compileStatement("INSERT OR REPLACE INTO backupstatus VALUES (?,?)");
      _insertOrReplaceTransaction = _db.compileStatement("INSERT OR REPLACE INTO transactionlabels VALUES (?,?)");
   }

   private byte[] getBytesFromId(UUID uuid)
   {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(uuid.getMostSignificantBits());
      bb.putLong(uuid.getLeastSignificantBits());
      return bb.array();
   }

   private UUID getIdFromBytes(byte[] data) {
      ByteBuffer bb = ByteBuffer.wrap(data);
      long firstLong = bb.getLong();
      long secondLong = bb.getLong();
      return new UUID(firstLong, secondLong);
   }

   public String getLabelByTransaction(Sha256Hash txid) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
         blobQuery.bindBlob(1, txid.getBytes());
         cursor = blobQuery.query(false, TABLE_TRANSACTION_LABELS, new String[]{"label"}, "id = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            return cursor.getString(0);
         }
         return "";
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public void storeTransactionLabel(Sha256Hash txid, String label) {
      _insertOrReplaceTransaction.bindBlob(1, txid.getBytes());
      _insertOrReplaceTransaction.bindString(2, label);
      _insertOrReplaceTransaction.executeInsert();
   }


   public String getLabelByAccount(UUID account) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
         blobQuery.bindBlob(1, getBytesFromId(account));
         cursor = blobQuery.query(false, TABLE_ACCOUNT_LABELS, new String[]{"label"}, "id = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            return cursor.getString(0);
         }
         return "";
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public Optional<UUID> getAccountByLabel(String label) {
      Cursor cursor = null;
      try {
         cursor = _db.query(false, TABLE_ACCOUNT_LABELS, new String[]{"id"}, "label = '" + label + "'", null, null, null, null, null);
         if (cursor.moveToNext()) {
            return Optional.of(getIdFromBytes(cursor.getBlob(0)));
         }
         return Optional.absent();
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public void storeAccountLabel(UUID account, String label) {
         _insertOrReplaceAccountLabel.bindBlob(1, getBytesFromId(account));
         _insertOrReplaceAccountLabel.bindString(2, label);
         _insertOrReplaceAccountLabel.executeInsert();
   }

   public BackupState getBackupState(WalletAccount account) {
      if (account instanceof Bip44Account) {
         return getMasterSeedBackupState();
      }
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
         blobQuery.bindBlob(1, getBytesFromId(account.getId()));
         cursor = blobQuery.query(false, TABLE_BACKUP_STATUS, new String[]{"status"}, "id = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            int state = cursor.getInt(0);
            return BackupState.fromInt(state);
         }
         return BackupState.UNKNOWN;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public BackupState getMasterSeedBackupState() {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
         blobQuery.bindBlob(1, MASTER_SEED_ID);
         cursor = blobQuery.query(false, TABLE_BACKUP_STATUS, new String[]{"status"}, "id = ?", null, null, null, null, null);
         if (cursor.moveToNext()) {
            int state = cursor.getInt(0);
            return BackupState.fromInt(state);
         }
         return BackupState.UNKNOWN;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public void setBackupState(UUID account, BackupState state) {
      _insertOrReplaceBackupData.bindBlob(1, getBytesFromId(account));
      _insertOrReplaceBackupData.bindLong(2, state.toInt());
      _insertOrReplaceBackupData.execute();
   }

   public void setMasterKeyBackupState(BackupState state) {
      _insertOrReplaceBackupData.bindBlob(1, MASTER_SEED_ID);
      _insertOrReplaceBackupData.bindLong(2, state.toInt());
      _insertOrReplaceBackupData.execute();
   }

   public enum BackupState {
      UNKNOWN(0), VERIFIED(1), IGNORED(2);

      private final int _index;
      private BackupState(int index) {
         _index = index;
      }
      public int toInt() {
         return _index;
      }
      public static BackupState fromInt(int integer) {
         switch (integer) {
            case 0:
               return BackupState.UNKNOWN;
            case 1:
               return BackupState.VERIFIED;
            case 2:
               return BackupState.IGNORED;
            default:
               return BackupState.UNKNOWN;
         }
      }
   }
}

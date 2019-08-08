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

package com.mycelium.wallet.colu;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs;
import com.mycelium.wapi.api.exception.DbCorruptedException;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.CommonAccountBacking;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.SecureKeyValueStoreBacking;
import com.mycelium.wapi.wallet.WalletBacking;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.ColuAccountBacking;
import com.mycelium.wapi.wallet.colu.ColuAccountContext;
import com.mycelium.wapi.wallet.colu.ColuUtils;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.colu.json.Tx;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mycelium.wallet.persistence.SQLiteQueryWithBlobs.uuidToBytes;

public class SqliteColuManagerBacking implements WalletBacking<ColuAccountContext>, SecureKeyValueStoreBacking {
   private static final String LOG_TAG = "SqliteColuManagerBackin";
   private static final String TABLE_KV = "kv";
   private static final int DEFAULT_SUB_ID = 0;
   private SQLiteDatabase _database;
   private final Gson gson = new GsonBuilder().create();
   private  final JsonFactory JSON_FACTORY = new JacksonFactory();
   private Map<UUID, SqliteColuAccountBacking> _backings;
   private final SQLiteStatement _insertOrReplaceSingleAddressAccount;
   private final SQLiteStatement _updateSingleAddressAccount;
   private final SQLiteStatement _deleteSingleAddressAccount;
   private final SQLiteStatement _insertOrReplaceKeyValue;
   private final SQLiteStatement _deleteKeyValue;
   private final SQLiteStatement _deleteSubId;
   private final SQLiteStatement _getMaxSubId;
   private final NetworkParameters networkParameters;

   private static final String LAST_FEE_ESTIMATE = "_LAST_FEE_ESTIMATE";


   public SqliteColuManagerBacking(Context context, NetworkParameters networkParameters) {
      this.networkParameters = networkParameters;
      OpenHelper _openHelper = new OpenHelper(context);
      _database = _openHelper.getWritableDatabase();

      _insertOrReplaceSingleAddressAccount = _database.compileStatement("INSERT OR REPLACE INTO single (id, addresses, archived, blockheight, coinId) VALUES (?,?,?,?,?)");
      _updateSingleAddressAccount = _database.compileStatement("UPDATE single SET archived=?,blockheight=?,addresses=? WHERE id=?");
      _deleteSingleAddressAccount = _database.compileStatement("DELETE FROM single WHERE id = ?");
      _insertOrReplaceKeyValue = _database.compileStatement("INSERT OR REPLACE INTO kv VALUES (?,?,?,?)");
      _getMaxSubId = _database.compileStatement("SELECT max(subId) FROM kv");
      _deleteKeyValue = _database.compileStatement("DELETE FROM kv WHERE k = ?");
      _deleteSubId = _database.compileStatement("DELETE FROM kv WHERE subId = ?");
      _backings = new HashMap<>();
      for (UUID id : getAccountIds(_database)) {
         _backings.put(id, new SqliteColuAccountBacking(id, _database));
      }
   }

   @Override
   public List<ColuAccountContext> loadAccountContexts() {
      List<ColuAccountContext> list = new ArrayList<>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         cursor = blobQuery.query(false, "single", new String[]{"id", "addresses", "archived", "blockheight", "coinId"}, null, null,
                 null, null, null, null);
         while (cursor.moveToNext()) {
            UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            boolean isArchived = cursor.getInt(2) == 1;
            int blockHeight = cursor.getInt(3);
            String coinId = cursor.getString(4);

            if (coinId == null) {
               Log.w(LOG_TAG, "Asset not registered in system, and not imported, skipping...");
               continue;
            }
            ColuMain coinType = ColuUtils.getColuCoin(coinId);
            if (coinType == null) {
               Log.w(LOG_TAG, String.format("Asset with id=%s, skipping...", coinId));
               continue;
            }

            Type type = new TypeToken<Collection<String>>() {}.getType();
            Collection<String> addressStringsList = gson.fromJson(cursor.getString(1), type);
            Map<AddressType, BtcAddress> addresses = new ArrayMap<>(3);
            if (addressStringsList != null) {
               for (String addressString : addressStringsList) {
                  Address address = Address.fromString(addressString);
                  addresses.put(address.getType(), new BtcAddress(coinType, address));
               }
            }
            list.add(new ColuAccountContext(id, coinType, addresses, isArchived, blockHeight));
         }
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
      return list;
   }

   @Override
   public void createAccountContext(ColuAccountContext context) {
      _database.beginTransaction();
      try {
         // Create accountBacking tables
         SqliteColuAccountBacking backing = _backings.get(context.getId());
         if (backing == null) {
            createAccountBackingTables(context.getId(), _database);
            backing = new SqliteColuAccountBacking(context.getId(), _database);
            _backings.put(context.getId(), backing);
         }

         // Create context
         _insertOrReplaceSingleAddressAccount.bindBlob(1, uuidToBytes(context.getId()));
         List<String> addresses = new ArrayList<>();
         if(context.getAddress() != null) {
            for (BtcAddress address : context.getAddress().values()) {
               addresses.add(address.toString());
            }
            _insertOrReplaceSingleAddressAccount.bindString(2, gson.toJson(addresses));
         }
         _insertOrReplaceSingleAddressAccount.bindLong(3, context.isArchived() ? 1 : 0);
         _insertOrReplaceSingleAddressAccount.bindLong(4, context.getBlockHeight());
         _insertOrReplaceSingleAddressAccount.bindString(5, context.getCoinType().getId());
         _insertOrReplaceSingleAddressAccount.executeInsert();
         _database.setTransactionSuccessful();
      } finally {
         _database.endTransaction();
      }
   }

   @Override
   public void deleteAccountContext(UUID accountId) {
      // "DELETE FROM single WHERE id = ?"
      beginTransaction();
      try {
         SqliteColuAccountBacking backing = _backings.get(accountId);
         if (backing == null) {
            return;
         }
         _deleteSingleAddressAccount.bindBlob(1, uuidToBytes(accountId));
         _deleteSingleAddressAccount.execute();
         backing.dropTables();
         _backings.remove(accountId);
         setTransactionSuccessful();
      } finally {
         endTransaction();
      }
   }

   @Override
   public CommonAccountBacking getAccountBacking(UUID accountId) {
      return checkNotNull(_backings.get(accountId));
   }

   private List<UUID> getAccountIds(SQLiteDatabase db) {
      Cursor cursor = null;
      List<UUID> accounts = new ArrayList<>();
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
         cursor = blobQuery.query(false, "single", new String[]{"id"}, null, null, null, null, null, null);
         while (cursor.moveToNext()) {
            UUID uuid = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            accounts.add(uuid);
         }
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
      return accounts;
   }

   public void beginTransaction() {
      _database.beginTransaction();
   }

   public void setTransactionSuccessful() {
      _database.setTransactionSuccessful();
   }

   public void endTransaction() {
      _database.endTransaction();
   }

   @Override
   public void updateAccountContext(ColuAccountContext context) {
      _database.beginTransaction();
      try {
         // "UPDATE single SET archived=?,blockheight=?,addresses=? WHERE id=?"
         _updateSingleAddressAccount.bindLong(1, context.isArchived() ? 1 : 0);
         _updateSingleAddressAccount.bindLong(2, context.getBlockHeight());

         List<String> addresses = new ArrayList<>();
         if(context.getAddress() != null) {
            for (BtcAddress address : context.getAddress().values()) {
               addresses.add(address.toString());
            }
            _updateSingleAddressAccount.bindString(3, gson.toJson(addresses));
         }
         _updateSingleAddressAccount.bindBlob(4, uuidToBytes(context.getId()));
         _updateSingleAddressAccount.execute();
         _database.setTransactionSuccessful();
      }
      finally {
         _database.endTransaction();
      }
   }

   @Override
   public byte[] getValue(byte[] id) {
      return getValue(id, DEFAULT_SUB_ID);
   }

   @Override
   public byte[] getValue(byte[] id, int subId) {
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         blobQuery.bindBlob(1, id);
         blobQuery.bindLong(2, (long) subId);
         cursor = blobQuery.query(false, TABLE_KV, new String[]{"v", "checksum"}, "k = ? and subId = ?", null, null, null,
               null, null);
         if (cursor.moveToNext()) {
            byte[] retVal = cursor.getBlob(0);
            byte[] checkSumDb = cursor.getBlob(1);

            // checkSumDb might be null for older data, where we hadn't had a checksum
            if (checkSumDb != null && !Arrays.equals(checkSumDb, calcChecksum(id, retVal))) {
               // mismatch in checksum - the DB might be corrupted
               Log.e(LOG_TAG, "Checksum failed - SqliteDB might be corrupted");
               throw new DbCorruptedException("Checksum failed while reading from DB. Your file storage might be corrupted");
            }

            return retVal;
         }
         return null;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   @Override
   public void setValue(byte[] key, byte[] value) {
      setValue(key, DEFAULT_SUB_ID, value);
   }

   @Override
   public int getMaxSubId() {
      return (int) _getMaxSubId.simpleQueryForLong();
   }

   @Override
   public void setValue(byte[] key, int subId, byte[] value) {
      _insertOrReplaceKeyValue.bindBlob(1, key);
      SQLiteQueryWithBlobs.bindBlobWithNull(_insertOrReplaceKeyValue, 2, value);
      _insertOrReplaceKeyValue.bindBlob(3, calcChecksum(key, value));
      _insertOrReplaceKeyValue.bindLong(4, subId);

      _insertOrReplaceKeyValue.executeInsert();
   }

   private byte[] calcChecksum(byte[] key, byte[] value) {
      byte[] toHash = BitUtils.concatenate(key, value);
      return HashUtils.sha256(toHash).firstNBytes(8);
   }

   @Override
   public void deleteValue(byte[] id) {
      _deleteKeyValue.bindBlob(1, id);
      _deleteKeyValue.execute();
   }

   @Override
   public void deleteSubStorageId(int subId) {
      _deleteSubId.bindLong(1, subId);
      _deleteSubId.execute();
   }

   private static void createTxTable(String tableSuffix, SQLiteDatabase db) {
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getTxTableName(tableSuffix)
              + " (id BLOB PRIMARY KEY, height INTEGER, time INTEGER, txData BLOB);");
   }

   private static void createAccountBackingTables(UUID id, SQLiteDatabase db) {
      String tableSuffix = uuidToTableSuffix(id);
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getUtxoTableName(tableSuffix)
            + " (outpoint BLOB PRIMARY KEY, height INTEGER, value INTEGER, isCoinbase INTEGER, script BLOB);");
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getPtxoTableName(tableSuffix)
            + " (outpoint BLOB PRIMARY KEY, height INTEGER, value INTEGER, isCoinbase INTEGER, script BLOB);");
      createTxTable(tableSuffix, db);
      db.execSQL("CREATE INDEX IF NOT EXISTS heightIndex ON " + getTxTableName(tableSuffix) + " (height);");
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getOutgoingTxTableName(tableSuffix)
            + " (id BLOB PRIMARY KEY, raw BLOB);");
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getTxRefersPtxoTableName(tableSuffix)
            + " (txid BLOB, input BLOB, PRIMARY KEY (txid, input) );");
   }

   private static String uuidToTableSuffix(UUID uuid) {
      return HexUtils.toHex(uuidToBytes(uuid));
   }

   private static String getUtxoTableName(String tableSuffix) {
      return "utxo_" + tableSuffix;
   }

   private static String getPtxoTableName(String tableSuffix) {
      return "ptxo_" + tableSuffix;
   }

   private static String getTxRefersPtxoTableName(String tableSuffix) {
      return "txtoptxo_" + tableSuffix;
   }

   private static String getTxTableName(String tableSuffix) {
      return "tx_" + tableSuffix;
   }

   private static String getOutgoingTxTableName(String tableSuffix) {
      return "outtx_" + tableSuffix;
   }

   private class SqliteColuAccountBacking implements ColuAccountBacking {
      private UUID _id;
      private final String utxoTableName;
      private final String ptxoTableName;
      private final String txTableName;
      private final String outTxTableName;
      private final String txRefersParentTxTableName;
      private final SQLiteDatabase _db;

      private class FeeEstimationSerialized implements Serializable {
         private long low;
         private long economy;
         private long normal;
         private long high;
         private long lastCheck;

         FeeEstimationSerialized(long low, long economy, long normal, long high, long lastCheck) {
            this.low = low;
            this.economy = economy;
            this.normal = normal;
            this.high = high;
            this.lastCheck = lastCheck;
         }
      }

      private SqliteColuAccountBacking(UUID id, SQLiteDatabase db) {
         _id = id;
         _db = db;
         String tableSuffix = uuidToTableSuffix(id);
         utxoTableName = getUtxoTableName(tableSuffix);
         ptxoTableName = getPtxoTableName(tableSuffix);
         txTableName = getTxTableName(tableSuffix);
         outTxTableName = getOutgoingTxTableName(tableSuffix);
         txRefersParentTxTableName = getTxRefersPtxoTableName(tableSuffix);
      }

      private void dropTables() {
         String tableSuffix = uuidToTableSuffix(_id);
         _db.execSQL("DROP TABLE IF EXISTS " + getUtxoTableName(tableSuffix));
         _db.execSQL("DROP TABLE IF EXISTS " + getTxTableName(tableSuffix));
      }

      @Override
      public void beginTransaction() {
         SqliteColuManagerBacking.this.beginTransaction();
      }

      @Override
      public void setTransactionSuccessful() {
         SqliteColuManagerBacking.this.setTransactionSuccessful();
      }

      @Override
      public void endTransaction() {
         SqliteColuManagerBacking.this.endTransaction();
      }

      @Override
      public void clear() {
         _db.execSQL("DELETE FROM " + utxoTableName);
         _db.execSQL("DELETE FROM " + ptxoTableName);
         _db.execSQL("DELETE FROM " + txTableName);
         _db.execSQL("DELETE FROM " + outTxTableName);
         _db.execSQL("DELETE FROM " + txRefersParentTxTableName);
      }

      @Override
      public void saveLastFeeEstimation(FeeEstimationsGeneric feeEstimation, GenericAssetInfo assetType) {
         String assetTypeName = assetType.getName();
         byte[] key = (assetTypeName + LAST_FEE_ESTIMATE).getBytes();
         FeeEstimationSerialized feeValues = new FeeEstimationSerialized(feeEstimation.getLow().value,
                 feeEstimation.getEconomy().value,
                 feeEstimation.getNormal().value,
                 feeEstimation.getHigh().value,
                 feeEstimation.getLastCheck());
         byte[] value = gson.toJson(feeValues).getBytes();
         setValue(key, value);
      }

      @Override
      public FeeEstimationsGeneric loadLastFeeEstimation(GenericAssetInfo assetType) {
         String key = assetType.getName() + LAST_FEE_ESTIMATE;
         FeeEstimationSerialized feeValues;
         try {
            feeValues = gson.fromJson(key, FeeEstimationSerialized.class);
         }
         catch(Exception ignore) { return null; }

         return new FeeEstimationsGeneric(Value.valueOf(assetType, feeValues.low),
                 Value.valueOf(assetType, feeValues.economy),
                 Value.valueOf(assetType, feeValues.normal),
                 Value.valueOf(assetType, feeValues.high),
                 feeValues.lastCheck);
      }

      @Override
      public Tx.Json getTx(Sha256Hash hash) {
         Cursor cursor = null;
         Tx.Json result = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, hash.getBytes());
            cursor = blobQuery.raw( "SELECT height, time, txData FROM " + txTableName + " WHERE id = ?" , txTableName);
            if (cursor.moveToNext()) {
               String json = new String(cursor.getBlob(2), StandardCharsets.UTF_8);
               result = getTransactionFromJson(json);
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return result;
      }

      private Tx.Json getTransactionFromJson(String string) {
         try {
            return JSON_FACTORY.fromString(string, Tx.Json.class);
         } catch (IOException ex) {
             Log.e("colu accountBacking", "Parse error", ex);
         }
         return null;
      }

      @Override
      public List<TransactionOutputEx> getUnspentOutputs() {
         return new ArrayList<>();
      }

      @Override
      public void putUnspentOutputs(List<TransactionOutputEx> unspentOutputs) {

      }

      @Override
      public List<Tx.Json> getTransactions(int offset, int limit) {
         Cursor cursor = null;
         List<Tx.Json> result = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT height, txData FROM " + txTableName
                            + " ORDER BY height desc limit ? offset ?",
                    new String[]{Integer.toString(limit), Integer.toString(offset)});
            while (cursor.moveToNext()) {
               String json = new String(cursor.getBlob(1), StandardCharsets.UTF_8);
               Tx.Json tex = getTransactionFromJson(json);
               result.add(tex);
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return result;
      }

      @Override
      public void putTransactions(List<Tx.Json> transactions) {
         if (transactions.isEmpty()) {
            return;
         }
         _database.beginTransaction();
         String updateQuery = "INSERT OR REPLACE INTO " + txTableName + " VALUES "
                 + TextUtils.join(",", Collections.nCopies(transactions.size(), " (?,?,?,?) "));
         SQLiteStatement updateStatement = _database.compileStatement(updateQuery);
         try {
            int i = 0;
            for (Tx.Json transaction: transactions) {
               int index = i * 4;
               updateStatement.bindBlob(index + 1, Sha256Hash.fromString(transaction.txid).getBytes());
               updateStatement.bindLong(index + 2, transaction.blockheight == -1 ? Integer.MAX_VALUE : transaction.blockheight);
               updateStatement.bindLong(index + 3, transaction.time / 1000);
               updateStatement.bindBlob(index + 4, transaction.toString().getBytes());
               transaction.setFactory(JSON_FACTORY);
               i++;
            }
            updateStatement.executeInsert();

            _database.setTransactionSuccessful();
         } finally {
            _database.endTransaction();
         }
      }

      @Override
      public List<Tx.Json> getTransactionsSince(long since) {
         List<Tx.Json> result = new LinkedList<>();
         try (Cursor cursor = _db.rawQuery("SELECT height, time, txData FROM " + txTableName
                         + " WHERE time >= ?"
                         + " ORDER BY height desc",
                 new String[]{Long.toString(since / 1000)})) {

            while (cursor.moveToNext()) {
               String json = new String(cursor.getBlob(2), StandardCharsets.UTF_8);
               Tx.Json tex = getTransactionFromJson(json);
               result.add(tex);
            }
         }
         return result;
      }
   }

   private class OpenHelper extends SQLiteOpenHelper {
      private static final String DATABASE_NAME = "columanagerbacking.db";
      private static final int DATABASE_VERSION = 10;
      private Context context;

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
         this.context = context;

         // The backings tables should already exists, but try to recreate them anyhow, as the CREATE TABLE
         // uses the "IF NOT EXISTS" switch
         for (UUID account : getAccountIds(getWritableDatabase())) {
            createAccountBackingTables(account, getWritableDatabase());
         }
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE single (id TEXT PRIMARY KEY, addresses TEXT, archived INTEGER"
                 + ", blockheight INTEGER, coinId TEXT" +
                 ");");
         db.execSQL("CREATE TABLE kv (k BLOB NOT NULL, v BLOB, checksum BLOB, subId INTEGER NOT NULL, PRIMARY KEY (k, subId) );");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         if (oldVersion < 2) {
            db.execSQL("ALTER TABLE kv ADD COLUMN checksum BLOB");
         }
         if (oldVersion < 3) {
            // add column to the secure kv table to indicate sub-stores
            // use a temporary table to migrate the table, as sqlite does not allow to change primary keys constraints
            db.execSQL("CREATE TABLE kv_new (k BLOB NOT NULL, v BLOB, checksum BLOB, subId INTEGER NOT NULL, PRIMARY KEY (k, subId) );");
            db.execSQL("INSERT INTO kv_new SELECT k, v, checksum, 0 FROM kv");
            db.execSQL("ALTER TABLE kv RENAME TO kv_old");
            db.execSQL("ALTER TABLE kv_new RENAME TO kv");
            db.execSQL("DROP TABLE kv_old");
         }
         if (oldVersion < 4) {
            try (Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'tx^_%' ESCAPE '^'", new String[]{})) {
               while (cursor.moveToNext()) {
                  String tableName = cursor.getString(0);
                  String newPostfix = "_new";
                  String oldPostfix = "_old";
                  db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + newPostfix
                          + " (id BLOB PRIMARY KEY, hash BLOB, height INTEGER, time INTEGER, binary BLOB)");
                  db.execSQL("INSERT INTO " + tableName + newPostfix + " SELECT id, id, height, time, binary FROM " + tableName);
                  db.execSQL("ALTER TABLE " + tableName + " RENAME TO " + tableName + oldPostfix);
                  db.execSQL("ALTER TABLE " + tableName + newPostfix + " RENAME TO " + tableName);
                  db.execSQL("DROP TABLE " + tableName + oldPostfix);
               }
            }
         }
         if (oldVersion < 5) {
            // Migrate SA
            List<SingleAddressAccountContext> list = new ArrayList<>();
            Cursor cursor = null;
            try {
               SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
               cursor = blobQuery.query(false, "single", new String[]{"id", "address", "addressstring", "archived", "blockheight"}, null, null,
                       null, null, null, null);
               MetadataStorage metadataStorage = new MetadataStorage(context);

               Map<UUID, ColuMain> coluUUIDs = new ArrayMap<>();
               for (ColuMain coin : ColuUtils.allColuCoins(BuildConfig.FLAVOR)) {
                  if (!Strings.isNullOrEmpty(coin.getId())) {
                     UUID[] uuids = metadataStorage.getColuAssetUUIDs(coin.getId());
                     for (UUID uuid : uuids) {
                        coluUUIDs.put(uuid, coin);
                     }
                  }
               }
               while (cursor.moveToNext()) {
                  UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
                  byte[] addressBytes = cursor.getBlob(1);
                  String addressString = cursor.getString(2);
                  Address address = new Address(addressBytes, addressString);
                  UUID newId = ColuUtils.getGuidForAsset(coluUUIDs.get(id), address.getAllAddressBytes());

                  metadataStorage.storeAccountLabel(newId, metadataStorage.getLabelByAccount(id));
                  metadataStorage.setOtherAccountBackupState(newId, metadataStorage.getOtherAccountBackupState(id));
                  metadataStorage.storeArchived(newId, metadataStorage.getArchived(id));
                  if (coluUUIDs.keySet().contains(id)) {
                     String assetId = coluUUIDs.get(id).getId();
                     metadataStorage.addColuAssetUUIDs(assetId, newId);
                     metadataStorage.removeColuAssetUUIDs(assetId, id);
                     Optional<String> coluBalance = metadataStorage.getColuBalance(id);
                     if (coluBalance.isPresent()) {
                        metadataStorage.storeColuBalance(newId, coluBalance.get());
                     }
                  }
                  metadataStorage.deleteAccountMetadata(id);
                  metadataStorage.deleteOtherAccountBackupState(id);

                  boolean isArchived = cursor.getInt(3) == 1;
                  int blockHeight = cursor.getInt(4);
                  list.add(new SingleAddressAccountContext(newId, ImmutableMap.of(address.getType(), address), isArchived, blockHeight, AddressType.P2SH_P2WPKH));
               }
            } finally {
               if (cursor != null) {
                  cursor.close();
               }
            }
            db.execSQL("CREATE TABLE single_new (id TEXT PRIMARY KEY, addresses TEXT, archived INTEGER, blockheight INTEGER, addressType TEXT);");
            SQLiteStatement statement = db.compileStatement("INSERT OR REPLACE INTO single_new VALUES (?,?,?,?,?)");
            for (SingleAddressAccountContext saaContext : list) {
               statement.bindBlob(1, uuidToBytes(saaContext.getId()));
               List<String> addresses = new ArrayList<>();
               for (Address address: saaContext.getAddresses().values()){
                  addresses.add(address.toString());
               }
               statement.bindString(2, gson.toJson(addresses));
               statement.bindLong(3, saaContext.isArchived() ? 1 : 0);
               statement.bindLong(4, saaContext.getBlockHeight());
               statement.bindString(5, gson.toJson(saaContext.getDefaultAddressType()));

               statement.executeInsert();
            }
            db.execSQL("ALTER TABLE single RENAME TO single_old");
            db.execSQL("ALTER TABLE single_new RENAME TO single");
            db.execSQL("DROP TABLE single_old");
         }

         if(oldVersion < 7) {
            List<UUID> listForRemove = new ArrayList<>();
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
            try (Cursor cursor = blobQuery.query(false, "single", new String[]{"id", "addresses"}, null, null,
                    null, null, null, null)) {
               while (cursor.moveToNext()) {
                  UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
                  listForRemove.add(id);
                  Type type = new TypeToken<Collection<String>>() {}.getType();
                  Collection<String> addressStringsList = gson.fromJson(cursor.getString(1), type);
                  for (String addressString : addressStringsList) {
                     Address address = Address.fromString(addressString);
                     if (address.getType() == AddressType.P2PKH) {
                        listForRemove.remove(id);
                        break;
                     }
                  }
               }
            }
            SQLiteStatement deleteSingleAddressAccount = db.compileStatement("DELETE FROM single WHERE id = ?");
            for (UUID uuid : listForRemove) {
               Log.d("SColuManagerBacking", "onUpgrade: deleting account " + uuid);
               deleteSingleAddressAccount.bindBlob(1, uuidToBytes(uuid));
               deleteSingleAddressAccount.execute();
            }
         }

         if(oldVersion < 9) {
            if (!columnExistsInTable(db, "single", "publicKey")) {
               db.execSQL("ALTER TABLE single ADD COLUMN publicKey TEXT");
            }

            if (!columnExistsInTable(db, "single", "coinId")) {
               db.execSQL("ALTER TABLE single ADD COLUMN coinId TEXT");

               SQLiteStatement updateCoinIdStatement = db.compileStatement("UPDATE single SET coinId=? WHERE id=?");
               MetadataStorage metadataStorage = new MetadataStorage(context);
               for (ColuMain coin : ColuUtils.allColuCoins(BuildConfig.FLAVOR)) {
                  if (!Strings.isNullOrEmpty(coin.getId())) {
                     UUID[] uuids = metadataStorage.getColuAssetUUIDs(coin.getId());
                     for (UUID uuid : uuids) {
                        updateCoinIdStatement.bindString(1, coin.getId());
                        updateCoinIdStatement.bindBlob(2, uuidToBytes(uuid));
                        updateCoinIdStatement.execute();
                     }
                  }
               }
            }

            // DROP previous table with transaction because txData field replaced old raw tx column
            // SQL COMMAND ALTER TABLE .. RENAME COLUMN is not used because it is not supported
            // by older SQLITE versions. Assuming not all devices have the latest SQLITE version
            // installed
            for (UUID account : getAccountIds(db)) {
               String tableSuffix = uuidToTableSuffix(account);
               db.execSQL("DROP TABLE IF EXISTS " + getTxTableName(tableSuffix));
               createTxTable(tableSuffix, db);
            }
         }

         if (oldVersion < 10) {
            // This migration is intended to fix a couple of issues
            // - Removes unnesessary 'publicKey' field
            // - Resolves the issue for Colu accounts created in 3.X when coinId and publicKey data
            //   were mixed up between each other because of incorrect order their insertion
            //   inside INSERT INTO query. So, coinId stored publicKey BLOB data and publicKey stored
            //   STRING coinId data.

            db.execSQL("CREATE TABLE single_new (id TEXT PRIMARY KEY, addresses TEXT, archived INTEGER"
                    + ", blockheight INTEGER, coinId TEXT" +
                    ");");

            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);

            try (Cursor cursor = blobQuery.query(false, "single", new String[]{"id", "addresses", "archived", "blockheight", "coinId", "publicKey"}, null, null,
                    null, null, null, null)) {
               SQLiteStatement statement = db.compileStatement("INSERT INTO single_new (id, addresses, archived, blockheight, coinId) VALUES (?,?,?,?,?)");

               while (cursor.moveToNext()) {
                  statement.bindBlob(1, cursor.getBlob(0));
                  statement.bindLong(3, cursor.getLong(2));
                  statement.bindLong(4, cursor.getLong(3));

                  boolean brokenCoinIdData = false;
                  String coinId = null;

                  try {
                     coinId = cursor.getString(4);

                     // We can meet a case when coinId and publicKey are mixed up,
                     // For read-only accounts publicKey field value is null in 3.0.0.5-3.0.0.8
                     // Due for Colu accounts created in versions 3.0.0.5-3.0.0.8,
                     // it could be written to coinId field, so it will be null.
                     // We can check what type hes publicKey field.
                     // If it contains a string, we have coinId there
                     if (coinId == null) {
                        try {
                           // In the normal case we should have BLOB publiKey data here
                           // but let's try to read string
                           cursor.getString(5);
                           // If we can read publicKey field data as string, our hypothesis about
                           // mixed-up data is correct
                           brokenCoinIdData = true;
                        } catch(Exception ignore) {
                           // expected
                        }
                     }
                  } catch (Exception ex) {
                     brokenCoinIdData = true; // Probably we could not read this field as String because there a BLOB record
                  }

                  if (!brokenCoinIdData) {
                     byte[] publicKeyBytes = cursor.getBlob(5);
                     if (publicKeyBytes != null) {
                        statement.bindString(2, transformPublicKeyToAddressesList(publicKeyBytes, networkParameters));
                     } else {
                        statement.bindString(2, cursor.getString(1));
                     }

                     if (coinId != null) {
                        statement.bindString(5, coinId);
                     }
                  } else {
                     // There was a bug when coinId and publicKey were mixed up between each other
                     // So here we try to read coinId saved in publicKey field
                     try {
                        coinId = cursor.getString(5);
                     } catch (Exception ignore) {
                        // expected
                     }

                     // If coinId is not null here,
                     if (coinId != null) {
                        // So here we try to read publicKey saved in coinId field
                        byte[] publicKeyBytes = cursor.getBlob(4);
                        if (publicKeyBytes != null) {
                           statement.bindString(2, transformPublicKeyToAddressesList(publicKeyBytes, networkParameters));
                        } else {
                           statement.bindString(2, cursor.getString(1));
                        }
                        statement.bindString(5, coinId);
                     }
                  }
                  statement.executeInsert();
               }
            }

            db.execSQL("ALTER TABLE single RENAME TO single_old");
            db.execSQL("ALTER TABLE single_new RENAME TO single");
            db.execSQL("DROP TABLE single_old");
         }
      }

      private String transformPublicKeyToAddressesList(byte[] publicKeyBytes,
                                                             NetworkParameters networkParameters) {
         PublicKey key = new PublicKey(publicKeyBytes);
         List<String> addresses = new ArrayList<>();
         for (Address address : key.getAllSupportedAddresses(networkParameters).values()) {
            addresses.add(address.toString());
         }
         return gson.toJson(addresses);
      }

      private boolean columnExistsInTable(SQLiteDatabase db, String table, String columnToCheck) {
         try (Cursor cursor = db.rawQuery("SELECT * FROM " + table + " LIMIT 0", null)) {
            // getColumnIndex()  will return the index of the column
            //in the table if it exists, otherwise it will return -1
            return cursor.getColumnIndex(columnToCheck) != -1;
         } catch (SQLiteException ex) {
            //Something went wrong with SQLite.
            //If the table exists and your query was good,
            //the problem is likely that the column doesn't exist in the table.
            return false;
         }
      }

      @Override
      public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         //We don't really support downgrade but some android devices need this empty method
      }
   }
}

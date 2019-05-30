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
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
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
import com.mycelium.wapi.wallet.GenericTransactionSummary;
import com.mycelium.wapi.wallet.SecureKeyValueStoreBacking;
import com.mycelium.wapi.wallet.WalletBacking;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.colu.ColuAccountBacking;
import com.mycelium.wapi.wallet.colu.ColuAccountContext;
import com.mycelium.wapi.wallet.colu.ColuUtils;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
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
   private Map<UUID, SqliteColuAccountBacking> _backings;
   private final SQLiteStatement _insertOrReplaceSingleAddressAccount;
   private final SQLiteStatement _updateSingleAddressAccount;
   private final SQLiteStatement _deleteSingleAddressAccount;
   private final SQLiteStatement _insertOrReplaceKeyValue;
   private final SQLiteStatement _deleteKeyValue;
   private final SQLiteStatement _deleteSubId;
   private final SQLiteStatement _getMaxSubId;


   public SqliteColuManagerBacking(Context context) {
      OpenHelper _openHelper = new OpenHelper(context);
      _database = _openHelper.getWritableDatabase();

      _insertOrReplaceSingleAddressAccount = _database.compileStatement("INSERT OR REPLACE INTO single VALUES (?,?,?,?,?,?,?)");
      _updateSingleAddressAccount = _database.compileStatement("UPDATE single SET archived=?,blockheight=?,addresses=?,addressType=? WHERE id=?");
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
         cursor = blobQuery.query(false, "single", new String[]{"id", "addresses", "archived", "blockheight", "coinId", "publicKey"}, null, null,
                 null, null, null, null);
         while (cursor.moveToNext()) {
            UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            boolean isArchived = cursor.getInt(2) == 1;
            int blockHeight = cursor.getInt(3);
            String coinId = cursor.getString(4);
            if (coinId == null) {
               Log.w(LOG_TAG,"Asset not registered in system, and not imported, skipping...");
               continue;
            }
            ColuMain coinType = ColuUtils.getColuCoin(coinId);
            if (coinType == null) {
               Log.w(LOG_TAG, String.format("Asset with id=%s, skipping...", coinId));
               continue;
            }
            PublicKey publicKey = null;
            if (cursor.getBlob(5) != null) {
               publicKey = new PublicKey(cursor.getBlob(5));
            }
            Type type = new TypeToken<Collection<String>>(){}.getType();
            Collection<String> addressStringsList = gson.fromJson(cursor.getString(1), type);
            Map<AddressType, BtcAddress> addresses = new ArrayMap<>(3);
            if(addressStringsList != null) {
               for (String addressString : addressStringsList) {
                  Address address = Address.fromString(addressString);
                  addresses.put(address.getType(), new BtcAddress(coinType, address));
               }
            }
            list.add(new ColuAccountContext(id, coinType, publicKey, addresses
                     , isArchived, blockHeight));
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
         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(context.getDefaultAddressType());
            _insertOrReplaceSingleAddressAccount.bindBlob(5, byteStream.toByteArray());
         } catch (IOException ignore) {
            // should never happen
         }
         _insertOrReplaceSingleAddressAccount.bindString(6, context.getCoinType().getId());
         if(context.getPublicKey()!= null) {
            _insertOrReplaceSingleAddressAccount.bindBlob(7, context.getPublicKey().getPublicKeyBytes());
         }
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
         // "UPDATE single SET archived=?,blockheight=? WHERE id=?"
         _updateSingleAddressAccount.bindLong(1, context.isArchived() ? 1 : 0);
         _updateSingleAddressAccount.bindLong(2, context.getBlockHeight());
//         _updateSingleAddressAccount.bindBlob(3, context.getAddress().getBytes());
         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(context.getDefaultAddressType());
         }
         _updateSingleAddressAccount.bindBlob(4, byteStream.toByteArray());
         _updateSingleAddressAccount.bindBlob(5, uuidToBytes(context.getId()));
         _updateSingleAddressAccount.execute();
         _database.setTransactionSuccessful();
      } catch (IOException ignore) {
         // ignore
      } finally {
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
      byte toHash[] = BitUtils.concatenate(key, value);
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

   private static void createAccountBackingTables(UUID id, SQLiteDatabase db) {
      String tableSuffix = uuidToTableSuffix(id);
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getUtxoTableName(tableSuffix)
            + " (outpoint BLOB PRIMARY KEY, height INTEGER, value INTEGER, isCoinbase INTEGER, script BLOB);");
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getPtxoTableName(tableSuffix)
            + " (outpoint BLOB PRIMARY KEY, height INTEGER, value INTEGER, isCoinbase INTEGER, script BLOB);");
      db.execSQL("CREATE TABLE IF NOT EXISTS " + getTxTableName(tableSuffix)
            + " (id BLOB PRIMARY KEY, hash BLOB, height INTEGER, time INTEGER, binary BLOB);");
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
      public GenericTransactionSummary getTxSummary(Sha256Hash hash) {
         Cursor cursor = null;
         GenericTransactionSummary result = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, hash.getBytes());
            cursor = blobQuery.raw( "SELECT hash, height, time, binary FROM " + txTableName + " WHERE id = ?" , txTableName);

            if (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               ByteArrayInputStream bis = new ByteArrayInputStream(cursor.getBlob(3));
               ObjectInput in = null;
               try {
                  in = new ObjectInputStream(bis);
                  result = (GenericTransactionSummary) in.readObject();
                  result.confirmationRiskProfile = Optional.absent();
               } catch (IOException | ClassNotFoundException e) {
                  e.printStackTrace();
               } finally {
                  try {
                     if (in != null) {
                        in.close();
                     }
                  } catch (IOException ignore) {
                  }
               }
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return result;
      }

      @Override
      public List<TransactionOutputEx> getUnspentOutputs() {
         return new ArrayList<>();
      }

      @Override
      public void putUnspentOutputs(List<TransactionOutputEx> unspentOutputs) {

      }

      @Override
      public List<GenericTransactionSummary> getTransactionSummaries(int offset, int limit) {
         Cursor cursor = null;
         List<GenericTransactionSummary> result = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                            + " ORDER BY height desc limit ? offset ?",
                    new String[]{Integer.toString(limit), Integer.toString(offset)});
            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               GenericTransactionSummary tex = null;
               ByteArrayInputStream bis = new ByteArrayInputStream(cursor.getBlob(4));
               ObjectInput in = null;
               try {
                  in = new ObjectInputStream(bis);
                  tex = (GenericTransactionSummary) in.readObject();
                  tex.confirmationRiskProfile = Optional.absent();
                  result.add(tex);
               } catch (IOException | ClassNotFoundException e) {
                  e.printStackTrace();
               } finally {
                  try {
                     if (in != null) {
                        in.close();
                     }
                  } catch (IOException ignore) {
                  }
               }
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return result;
      }

      @Override
      public void putTransactions(List<GenericTransactionSummary> transactions) {
         if (transactions.isEmpty()) {
            return;
         }
         _database.beginTransaction();
         String updateQuery = "INSERT OR REPLACE INTO " + txTableName + " VALUES "
                 + TextUtils.join(",", Collections.nCopies(transactions.size(), " (?,?,?,?,?) "));
         SQLiteStatement updateStatement = _database.compileStatement(updateQuery);
         try {
            int i = 0;
            for (GenericTransactionSummary transaction: transactions) {
               int index = i * 5;
               updateStatement.bindBlob(index + 1, transaction.getId());
               updateStatement.bindBlob(index + 2, transaction.getId());
               updateStatement.bindLong(index + 3, transaction.getHeight() == -1 ? Integer.MAX_VALUE : transaction.getHeight());
               updateStatement.bindLong(index + 4, transaction.getTime());

               byte[] txData = null;
               ByteArrayOutputStream bos = new ByteArrayOutputStream();
               ObjectOutput out;
               try {
                  out = new ObjectOutputStream(bos);
                  out.writeObject(transaction);
                  out.flush();
                  txData = bos.toByteArray();
               } catch (IOException e) {
                  Log.e("colu accountBacking", "", e);
               } finally {
                  try {
                     bos.close();
                  } catch (IOException ignore) {
                  }
               }
               updateStatement.bindBlob(index + 5, txData);
               i++;
            }
            updateStatement.executeInsert();

            _database.setTransactionSuccessful();
         } finally {
            _database.endTransaction();
         }

      }

      @Override
      public List<GenericTransactionSummary> getTransactionsSince(long since) {
         Cursor cursor = null;
         List<GenericTransactionSummary> result = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                            + " WHERE time >= ?"
                            + " ORDER BY height desc",
                    new String[]{Long.toString(since / 1000)});

            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               GenericTransactionSummary tex = null;
               ByteArrayInputStream bis = new ByteArrayInputStream(cursor.getBlob(4));
               ObjectInput in = null;
               try {
                  in = new ObjectInputStream(bis);
                  tex = (GenericTransactionSummary) in.readObject();
                  result.add(tex);
               } catch (IOException | ClassNotFoundException e) {
                  e.printStackTrace();
               } finally {
                  try {
                     if (in != null) {
                        in.close();
                     }
                  } catch (IOException ignore) {
                  }
               }
            }
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
         return result;
      }
   }

   private class OpenHelper extends SQLiteOpenHelper {
      private static final String DATABASE_NAME = "columanagerbacking.db";
      private static final int DATABASE_VERSION = 8;
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
         db.execSQL("CREATE TABLE single (id TEXT PRIMARY KEY, addresses BLOB, archived INTEGER"
                 + ", blockheight INTEGER, addressType TEXT, coinId TEXT, publicKey BLOB" +
                 ");");
         db.execSQL("CREATE TABLE kv (k BLOB NOT NULL, v BLOB, checksum BLOB, subId INTEGER NOT NULL, PRIMARY KEY (k, subId) );");
      }

      @Override
      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         if (oldVersion < DATABASE_VERSION) {
            db.execSQL("ALTER TABLE single ADD COLUMN coinId TEXT");
            db.execSQL("ALTER TABLE single ADD COLUMN publicKey BLOB");

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
      }

      @Override
      public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         //We don't really support downgrade but some android devices need this empty method
      }
   }
}

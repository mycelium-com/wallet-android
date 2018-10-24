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
import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.persistence.SQLiteQueryWithBlobs;
import com.mycelium.wapi.api.exception.DbCorruptedException;
import com.mycelium.wapi.api.lib.FeeEstimation;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.wallet.AccountBacking;
import com.mycelium.wapi.wallet.SecureKeyValueStoreBacking;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.WalletBacking;
import com.mycelium.wapi.wallet.btc.Bip44AccountBacking;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.btc.WalletManagerBacking;
import com.mycelium.wapi.wallet.btc.bip44.AccountIndexesContext;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext;
import com.mycelium.wapi.wallet.colu.ColuAccountContext;
import com.mycelium.wapi.wallet.colu.ColuTransaction;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
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

public class SqliteColuManagerBacking implements WalletBacking<ColuAccountContext, ColuTransaction>, SecureKeyValueStoreBacking {
   private static final String LOG_TAG = "SqliteColuManagerBackin";
   private static final String TABLE_KV = "kv";
   private static final int DEFAULT_SUB_ID = 0;
//   private static final byte[] LAST_FEE_ESTIMATE = new byte[]{42, 55};
   private SQLiteDatabase _database;
   private Map<UUID, SqliteColuAccountBacking> _backings;
//   private final SQLiteStatement _insertOrReplaceBip44Account;
//   private final SQLiteStatement _updateBip44Account;
   private final SQLiteStatement _insertOrReplaceSingleAddressAccount;
   private final SQLiteStatement _updateSingleAddressAccount;
   private final SQLiteStatement _deleteSingleAddressAccount;
//   private final SQLiteStatement _deleteBip44Account;
   private final SQLiteStatement _insertOrReplaceKeyValue;
   private final SQLiteStatement _deleteKeyValue;
   private final SQLiteStatement _deleteSubId;
   private final SQLiteStatement _getMaxSubId;


   public SqliteColuManagerBacking(Context context) {
      OpenHelper _openHelper = new OpenHelper(context);
      _database = _openHelper.getWritableDatabase();

//      _insertOrReplaceBip44Account = _database.compileStatement("INSERT OR REPLACE INTO bip44 VALUES (?,?,?,?,?,?,?,?,?)");
      _insertOrReplaceSingleAddressAccount = _database.compileStatement("INSERT OR REPLACE INTO single VALUES (?,?,?,?,?,?)");
//      _updateBip44Account = _database.compileStatement("UPDATE bip44 SET archived=?,blockheight=?, indexContexts=?, lastDiscovery=?,accountType=?,accountSubId=?,addressType=? WHERE id=?");
      _updateSingleAddressAccount = _database.compileStatement("UPDATE single SET archived=?,blockheight=?,addresses=?,addressType=? WHERE id=?");
      _deleteSingleAddressAccount = _database.compileStatement("DELETE FROM single WHERE id = ?");
//      _deleteBip44Account = _database.compileStatement("DELETE FROM bip44 WHERE id = ?");
      _insertOrReplaceKeyValue = _database.compileStatement("INSERT OR REPLACE INTO kv VALUES (?,?,?,?)");
      _getMaxSubId = _database.compileStatement("SELECT max(subId) FROM kv");
      _deleteKeyValue = _database.compileStatement("DELETE FROM kv WHERE k = ?");
      _deleteSubId = _database.compileStatement("DELETE FROM kv WHERE subId = ?");
      _backings = new HashMap<>();
      for (UUID id : getAccountIds(_database)) {
         _backings.put(id, new SqliteColuAccountBacking(id, _database));
      }
   }

//   @Override
//   public void saveLastFeeEstimation(FeeEstimation feeEstimation) {
//      Gson gson = new Gson();
//      byte[] value = gson.toJson(feeEstimation).getBytes();
//      setValue(LAST_FEE_ESTIMATE, value);
//   }

//   @Override
//   public FeeEstimation loadLastFeeEstimation() {
//      Gson gson = new Gson();
//      byte[] value = getValue(LAST_FEE_ESTIMATE);
//      FeeEstimation feeEstimation = FeeEstimation.DEFAULT;
//      try {
//         String valueString = new String(value);
//         feeEstimation = gson.fromJson(valueString, FeeEstimation.class);
//      } catch (Exception ignore) { }
//       return feeEstimation;
//   }

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
            byte[] addressBytes = cursor.getBlob(1);
//            final ByteArrayInputStream byteStream = new ByteArrayInputStream(addressMapBytes);
//            /*Map<AddressType, Address> */Address address  = null;
//            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteStream)) {
//               address = /*(Map<AddressType, Address>)*/  (Address) objectInputStream.readObject();
//            } catch (IOException ignore) {
//               // should never happen
//            } catch (ClassNotFoundException ignore) {
//               // should never happen
//            }
            boolean isArchived = cursor.getInt(2) == 1;
            int blockHeight = cursor.getInt(3);
            String coinId = cursor.getString(4);
            ColuMain coinType = null;
            if (MTCoin.INSTANCE.getId().equals(coinId)) {
               coinType = MTCoin.INSTANCE;
            } else if (MASSCoin.INSTANCE.getId().equals(coinId)) {
               coinType = MASSCoin.INSTANCE;
            } else if(RMCCoin.INSTANCE.getId().equals(coinId)) {
               coinType = RMCCoin.INSTANCE;
            }
            list.add(new ColuAccountContext(id, coinType, new BtcLegacyAddress(coinType, addressBytes)
                    , isArchived, blockHeight));
         }
         return list;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   @Override
   public void createAccountContext(ColuAccountContext context) {
      _database.beginTransaction();
      try {
         // Create backing tables
         SqliteColuAccountBacking backing = _backings.get(context.getId());
         if (backing == null) {
            createAccountBackingTables(context.getId(), _database);
            backing = new SqliteColuAccountBacking(context.getId(), _database);
            _backings.put(context.getId(), backing);
         }

         // Create context
         _insertOrReplaceSingleAddressAccount.bindBlob(1, uuidToBytes(context.getId()));
         _insertOrReplaceSingleAddressAccount.bindBlob(2, context.getAddress().getBytes());
//         final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
//            objectOutputStream.write(context.getAddress().getAllAddressBytes());
//            _insertOrReplaceSingleAddressAccount.bindBlob(2, byteStream.toByteArray());
//         } catch (IOException ignore) {
//            // should never happen
//         }
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
         _insertOrReplaceSingleAddressAccount.executeInsert();
         _database.setTransactionSuccessful();
      } finally {
         _database.endTransaction();
      }

   }

   @Override
   public void deleteAccountContext(UUID uuid) {
      deleteSingleAddressAccountContext(uuid);
   }

   @Override
   public AccountBacking getAccountBacking(UUID accountId) {
      return checkNotNull(_backings.get(accountId));
   }

   private List<UUID> getAccountIds(SQLiteDatabase db) {
      List<UUID> ids = new ArrayList<>();
      ids.addAll(getBip44AccountIds(db));
      ids.addAll(getSingleAddressAccountIds(db));
      return ids;
   }

   private List<UUID> getSingleAddressAccountIds(SQLiteDatabase db) {
      Cursor cursor = null;
      List<UUID> accounts = new ArrayList<>();
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
         cursor = blobQuery.query(false, "single", new String[]{"id"}, null, null, null, null, null, null);
         while (cursor.moveToNext()) {
            UUID uuid = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            accounts.add(uuid);
         }
         return accounts;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   private List<UUID> getBip44AccountIds(SQLiteDatabase db) {
      Cursor cursor = null;
      List<UUID> accounts = new ArrayList<>();
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
         cursor = blobQuery.query(false, "bip44", new String[]{"id"}, null, null, null, null, null, null);
         while (cursor.moveToNext()) {
            UUID uuid = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            accounts.add(uuid);
         }
         return accounts;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
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

//   @Override
//   public List<HDAccountContext> loadBip44AccountContexts() {
//      List<HDAccountContext> list = new ArrayList<>();
//      Cursor cursor = null;
//      try {
//         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
//         cursor = blobQuery.query(
//               false, "bip44",
//               new String[]{"id", "accountIndex", "archived", "blockheight",
//                     "indexContexts", "lastDiscovery", "accountType", "accountSubId", "addressType"},
//               null, null, null, null, "accountIndex", null);
//
//         while (cursor.moveToNext()) {
//            UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
//            int accountIndex = cursor.getInt(1);
//            boolean isArchived = cursor.getInt(2) == 1;
//            int blockHeight = cursor.getInt(3);
//            byte[] contextIndexesMapBytes = cursor.getBlob(4);
//            ByteArrayInputStream byteStream = new ByteArrayInputStream(contextIndexesMapBytes);
//            Map<BipDerivationType, AccountIndexesContext> indexesContextMap = null;
//            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteStream)) {
//               indexesContextMap = (Map<BipDerivationType, AccountIndexesContext> ) objectInputStream.readObject();
//            } catch (IOException ignore) {
//               // should never happen
//            } catch (ClassNotFoundException ignore) {
//               // should never happen
//            }
//            long lastDiscovery = cursor.getLong(5);
//            int accountType = cursor.getInt(6);
//            int accountSubId = (int) cursor.getLong(7);
//
//            byte[] defaultAddressTypeBytes = cursor.getBlob(8);
//            byteStream = new ByteArrayInputStream(defaultAddressTypeBytes);
//            AddressType defaultAddressType = null;
//            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteStream)) {
//               defaultAddressType = (AddressType) objectInputStream.readObject();
//            } catch (IOException ignore) {
//               // should never happen
//            } catch (ClassNotFoundException ignore) {
//               // should never happen
//            }
//
//            list.add(new HDAccountContext(id, accountIndex, isArchived, blockHeight, lastDiscovery, indexesContextMap,
//                    accountType, accountSubId, defaultAddressType));
//         }
//         return list;
//      } finally {
//         if (cursor != null) {
//            cursor.close();
//         }
//      }
//   }

//   @Override
//   public void createBip44AccountContext(HDAccountContext context) {
//      _database.beginTransaction();
//      try {
//
//         // Create backing tables
//         SqliteColuAccountBacking backing = _backings.get(context.getId());
//         if (backing == null) {
//            createAccountBackingTables(context.getId(), _database);
//            backing = new SqliteColuAccountBacking(context.getId(), _database);
//            _backings.put(context.getId(), backing);
//         }
//
//         // Create context
//         _insertOrReplaceBip44Account.bindBlob(1, uuidToBytes(context.getId()));
//         _insertOrReplaceBip44Account.bindLong(2, context.getAccountIndex());
//         _insertOrReplaceBip44Account.bindLong(3, context.isArchived() ? 1 : 0);
//         _insertOrReplaceBip44Account.bindLong(4, context.getBlockHeight());
//         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
//            objectOutputStream.writeObject(context.getIndexesMap());
//            _insertOrReplaceBip44Account.bindBlob(5, byteStream.toByteArray());
//         } catch (IOException ignore) {
//            // should never happen
//         }
//         _insertOrReplaceBip44Account.bindLong(6, context.getLastDiscovery());
//         _insertOrReplaceBip44Account.bindLong(7, context.getAccountType());
//         _insertOrReplaceBip44Account.bindLong(8, context.getAccountSubId());
//         byteStream = new ByteArrayOutputStream();
//         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
//            objectOutputStream.writeObject(context.getDefaultAddressType());
//            _insertOrReplaceBip44Account.bindBlob(9, byteStream.toByteArray());
//         } catch (IOException ignore) {
//            // should never happen
//         }
//         _insertOrReplaceBip44Account.executeInsert();
//
//         _database.setTransactionSuccessful();
//      } finally {
//         _database.endTransaction();
//      }
//   }
//
//   @Override
//   public void upgradeBip44AccountContext(HDAccountContext context) {
//      updateBip44AccountContext(context);
//   }

//   private void updateBip44AccountContext(HDAccountContext context) {
//      _database.beginTransaction();
//      //UPDATE bip44 SET archived=?,blockheight=?,lastExternalIndexWithActivity=?,lastInternalIndexWithActivity=?,firstMonitoredInternalIndex=?,lastDiscovery=?,accountType=?,accountSubId=? WHERE id=?
//      try {
//         _updateBip44Account.bindLong(1, context.isArchived() ? 1 : 0);
//         _updateBip44Account.bindLong(2, context.getBlockHeight());
//         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
//            objectOutputStream.writeObject(context.getIndexesMap());
//         }
//         _updateBip44Account.bindBlob(3, byteStream.toByteArray());
//         _updateBip44Account.bindLong(4, context.getLastDiscovery());
//         _updateBip44Account.bindLong(5, context.getAccountType());
//         _updateBip44Account.bindLong(6, context.getAccountSubId());
//         byteStream = new ByteArrayOutputStream();
//         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
//            objectOutputStream.writeObject(context.getDefaultAddressType());
//         }
//         _updateBip44Account.bindBlob(7, byteStream.toByteArray());
//         _updateBip44Account.bindBlob(8, uuidToBytes(context.getId()));
//         _updateBip44Account.execute();
//         _database.setTransactionSuccessful();
//      } catch (IOException ignore) {
//         // should never happen
//      } finally {
//         _database.endTransaction();
//      }
//   }


   public List<SingleAddressAccountContext> loadSingleAddressAccountContexts() {
      List<SingleAddressAccountContext> list = new ArrayList<>();
      Cursor cursor = null;
      try {
         SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_database);
         cursor = blobQuery.query(false, "single", new String[]{"id", "addresses", "archived", "blockheight, addressType"}, null, null,
               null, null, null, null);
         while (cursor.moveToNext()) {
            UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
            byte[] addressMapBytes = cursor.getBlob(1);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(addressMapBytes);
            Map<AddressType, Address> addresses  = null;
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteStream)) {
               addresses = (Map<AddressType, Address>) objectInputStream.readObject();
            } catch (IOException ignore) {
               // should never happen
            } catch (ClassNotFoundException ignore) {
               // should never happen
            }
            boolean isArchived = cursor.getInt(2) == 1;
            int blockHeight = cursor.getInt(3);
            byte[] defaultAddressTypeBytes = cursor.getBlob(4);
            byteStream = new ByteArrayInputStream(defaultAddressTypeBytes);
            AddressType defaultAddressType  = null;
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteStream)) {
               defaultAddressType = (AddressType) objectInputStream.readObject();
            } catch (IOException ignore) {
               // should never happen
            } catch (ClassNotFoundException ignore) {
               // should never happen
            }
            list.add(new SingleAddressAccountContext(id, addresses, isArchived, blockHeight, defaultAddressType));
         }
         return list;
      } finally {
         if (cursor != null) {
            cursor.close();
         }
      }
   }

   public void createSingleAddressAccountContext(SingleAddressAccountContext context) {
      _database.beginTransaction();
      try {

         // Create backing tables
         SqliteColuAccountBacking backing = _backings.get(context.getId());
         if (backing == null) {
            createAccountBackingTables(context.getId(), _database);
            backing = new SqliteColuAccountBacking(context.getId(), _database);
            _backings.put(context.getId(), backing);
         }

         // Create context
         _insertOrReplaceSingleAddressAccount.bindBlob(1, uuidToBytes(context.getId()));
         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(context.getAddresses());
            _insertOrReplaceSingleAddressAccount.bindBlob(2, byteStream.toByteArray());
         } catch (IOException ignore) {
            // should never happen
         }
         _insertOrReplaceSingleAddressAccount.bindLong(3, context.isArchived() ? 1 : 0);
         _insertOrReplaceSingleAddressAccount.bindLong(4, context.getBlockHeight());

         byteStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(context.getDefaultAddressType());
            _insertOrReplaceSingleAddressAccount.bindBlob(5, byteStream.toByteArray());
         } catch (IOException ignore) {
            // should never happen
         }

         _insertOrReplaceSingleAddressAccount.executeInsert();
         _database.setTransactionSuccessful();
      } finally {
         _database.endTransaction();
      }
   }

   private void updateSingleAddressAccountContext(SingleAddressAccountContext context) {
      _database.beginTransaction();
      try {
         // "UPDATE single SET archived=?,blockheight=? WHERE id=?"
         _updateSingleAddressAccount.bindLong(1, context.isArchived() ? 1 : 0);
         _updateSingleAddressAccount.bindLong(2, context.getBlockHeight());
         ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
         try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
            objectOutputStream.writeObject(context.getAddresses());
         }
         _updateSingleAddressAccount.bindBlob(3, byteStream.toByteArray());
         byteStream = new ByteArrayOutputStream();
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

   public void deleteSingleAddressAccountContext(UUID accountId) {
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

//   @Override
//   public void deleteBip44AccountContext(UUID accountId) {
//      // "DELETE FROM bip44 WHERE id = ?"
//      beginTransaction();
//      try {
//         SqliteColuAccountBacking backing = _backings.get(accountId);
//         if (backing == null) {
//            return;
//         }
//         _deleteBip44Account.bindBlob(1, uuidToBytes(accountId));
//         _deleteBip44Account.execute();
//         backing.dropTables();
//         _backings.remove(accountId);
//         setTransactionSuccessful();
//      } finally {
//         endTransaction();
//      }
//   }

//   @Override
//   public Bip44AccountBacking getBip44AccountBacking(UUID accountId) {
//      return null; //checkNotNull(_backings.get(accountId));
//   }

//   public SingleAddressAccountBacking getSingleAddressAccountBacking(UUID accountId) {
//      return null; //checkNotNull(_backings.get(accountId));
//   }

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

   private class SqliteColuAccountBacking implements AccountBacking<ColuTransaction> {
      private UUID _id;
      private final String utxoTableName;
      private final String ptxoTableName;
      private final String txTableName;
      private final String outTxTableName;
      private final String txRefersParentTxTableName;
      private final SQLiteStatement _insertOrReplaceUtxo;
      private final SQLiteStatement _deleteUtxo;
      private final SQLiteStatement _insertOrReplacePtxo;
      private final SQLiteStatement _insertOrReplaceTx;
      private final SQLiteStatement _deleteTx;
      private final SQLiteStatement _insertOrReplaceOutTx;
      private final SQLiteStatement _deleteOutTx;
      private final SQLiteStatement _insertTxRefersParentTx;
      private final SQLiteStatement _deleteTxRefersParentTx;
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
         _insertOrReplaceUtxo = db.compileStatement("INSERT OR REPLACE INTO " + utxoTableName + " VALUES (?,?,?,?,?)");
         _deleteUtxo = db.compileStatement("DELETE FROM " + utxoTableName + " WHERE outpoint = ?");
         _insertOrReplacePtxo = db.compileStatement("INSERT OR REPLACE INTO " + ptxoTableName + " VALUES (?,?,?,?,?)");
         _insertOrReplaceTx = db.compileStatement("INSERT OR REPLACE INTO " + txTableName + " VALUES (?,?,?,?,?)");
         _deleteTx = db.compileStatement("DELETE FROM " + txTableName + " WHERE id = ?");
         _insertOrReplaceOutTx = db.compileStatement("INSERT OR REPLACE INTO " + outTxTableName + " VALUES (?,?)");
         _deleteOutTx = db.compileStatement("DELETE FROM " + outTxTableName + " WHERE id = ?");
         _insertTxRefersParentTx = db.compileStatement("INSERT OR REPLACE INTO " + txRefersParentTxTableName + " VALUES (?,?)");
         _deleteTxRefersParentTx = db.compileStatement("DELETE FROM " + txRefersParentTxTableName + " WHERE txid = ?");
      }

      private void dropTables() {
         String tableSuffix = uuidToTableSuffix(_id);
         _db.execSQL("DROP TABLE IF EXISTS " + getUtxoTableName(tableSuffix));
         _db.execSQL("DROP TABLE IF EXISTS " + getPtxoTableName(tableSuffix));
         _db.execSQL("DROP TABLE IF EXISTS " + getTxTableName(tableSuffix));
         _db.execSQL("DROP TABLE IF EXISTS " + getOutgoingTxTableName(tableSuffix));
         _db.execSQL("DROP TABLE IF EXISTS " + getTxRefersPtxoTableName(tableSuffix));
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
      public synchronized void putUnspentOutput(TransactionOutputEx output) {
         _insertOrReplaceUtxo.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(output.outPoint));
         _insertOrReplaceUtxo.bindLong(2, output.height);
         _insertOrReplaceUtxo.bindLong(3, output.value);
         _insertOrReplaceUtxo.bindLong(4, output.isCoinBase ? 1 : 0);
         _insertOrReplaceUtxo.bindBlob(5, output.script);
         _insertOrReplaceUtxo.executeInsert();
      }

      @Override
      public Collection<TransactionOutputEx> getAllUnspentOutputs() {
         Cursor cursor = null;
         List<TransactionOutputEx> list = new LinkedList<>();
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            cursor = blobQuery.query(false, utxoTableName, new String[]{"outpoint", "height", "value", "isCoinbase",
                  "script"}, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
               TransactionOutputEx tex = new TransactionOutputEx(SQLiteQueryWithBlobs.outPointFromBytes(cursor
                     .getBlob(0)), cursor.getInt(1), cursor.getLong(2), cursor.getBlob(4), cursor.getInt(3) != 0);
               list.add(tex);
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public TransactionOutputEx getUnspentOutput(OutPoint outPoint) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(outPoint));
            cursor = blobQuery.query(false, utxoTableName, new String[]{"height", "value", "isCoinbase", "script"},
                  "outpoint = ?", null, null, null, null, null);
            if (cursor.moveToNext()) {
               return new TransactionOutputEx(outPoint, cursor.getInt(0), cursor.getLong(1),
                     cursor.getBlob(3), cursor.getInt(2) != 0);
            }
            return null;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public void deleteUnspentOutput(OutPoint outPoint) {
         _deleteUtxo.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(outPoint));
         _deleteUtxo.execute();
      }

      @Override
      public void putParentTransactionOuputs(List<TransactionOutputEx> outputsList) {
         if (outputsList.isEmpty()) {
            return;
         }
         _database.beginTransaction();
         String updateQuery = "INSERT OR REPLACE INTO " + ptxoTableName + " VALUES "
                 + TextUtils.join(",", Collections.nCopies(outputsList.size(), " (?,?,?,?,?) "));
         SQLiteStatement updateStatement = _database.compileStatement(updateQuery);
         try {
            for (int i = 0; i < outputsList.size(); i++) {
               int index = i * 5;
               final TransactionOutputEx outputEx = outputsList.get(i);
               updateStatement.bindBlob(index + 1, SQLiteQueryWithBlobs.outPointToBytes(outputEx.outPoint));
               updateStatement.bindLong(index + 2, outputEx.height);
               updateStatement.bindLong(index + 3, outputEx.value);
               updateStatement.bindLong(index + 4, outputEx.isCoinBase ? 1 : 0);
               updateStatement.bindBlob(index + 5, outputEx.script);
            }
            updateStatement.executeInsert();
            _database.setTransactionSuccessful();
         } finally {
            _database.endTransaction();
         }
      }

      @Override
      public void putParentTransactionOutput(TransactionOutputEx output) {
         _insertOrReplacePtxo.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(output.outPoint));
         _insertOrReplacePtxo.bindLong(2, output.height);
         _insertOrReplacePtxo.bindLong(3, output.value);
         _insertOrReplacePtxo.bindLong(4, output.isCoinBase ? 1 : 0);
         _insertOrReplacePtxo.bindBlob(5, output.script);
         _insertOrReplacePtxo.executeInsert();
      }

      @Override
      public void putTxRefersParentTransaction(Sha256Hash txId, List<OutPoint> refersOutputs) {
         for (OutPoint output : refersOutputs) {
            _insertTxRefersParentTx.bindBlob(1, txId.getBytes());
            _insertTxRefersParentTx.bindBlob(2, SQLiteQueryWithBlobs.outPointToBytes(output));
            _insertTxRefersParentTx.executeInsert();
         }
      }

      @Override
      public ColuTransaction getTx(Sha256Hash hash) {
         return null;
      }

      @Override
      public List<ColuTransaction> getTransactions(int offset, int limit) {
         Cursor cursor = null;
         List<ColuTransaction> result = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                            + " ORDER BY height desc limit ? offset ?",
                    new String[]{Integer.toString(limit), Integer.toString(offset)});
            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               ColuTransaction tex = null;
               ByteArrayInputStream bis = new ByteArrayInputStream(cursor.getBlob(4));
               ObjectInput in = null;
               try {
                  in = new ObjectInputStream(bis);
                  tex = (ColuTransaction) in.readObject();
               } catch (IOException e) {
                  e.printStackTrace();
               } catch (ClassNotFoundException e) {
                  e.printStackTrace();
               } finally {
                  try {
                     if (in != null) {
                        in.close();
                     }
                  } catch (IOException ignore) {
                  }
               }
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
      public void putTransactions(List<ColuTransaction> transactions) {
         if (transactions.isEmpty()) {
            return;
         }
         _database.beginTransaction();
         String updateQuery = "INSERT OR REPLACE INTO " + txTableName + " VALUES "
                 + TextUtils.join(",", Collections.nCopies(transactions.size(), " (?,?,?,?,?) "));
         SQLiteStatement updateStatement = _database.compileStatement(updateQuery);
         try {
            int i = 0;
            for (ColuTransaction transaction: transactions) {
               int index = i * 5;
               updateStatement.bindBlob(index + 1, transaction.getId().getBytes());
               updateStatement.bindBlob(index + 2, transaction.getHash().getBytes());
               updateStatement.bindLong(index + 3, transaction.getAppearedAtChainHeight() == -1 ? Integer.MAX_VALUE : transaction.getAppearedAtChainHeight());
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
                  Log.e("colu backing", "", e);
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

//            for (TransactionEx transaction : transactions) {
//               putReferencedOutputs(transaction.binary);
//            }
            _database.setTransactionSuccessful();
         } finally {
            _database.endTransaction();
         }

      }

      @Override
      public void deleteTxRefersParentTransaction(Sha256Hash txId) {
         _deleteTxRefersParentTx.bindBlob(1, txId.getBytes());
         _deleteTxRefersParentTx.execute();
      }

      @Override
      public Collection<Sha256Hash> getTransactionsReferencingOutPoint(OutPoint outPoint) {
         Cursor cursor = null;
         List<Sha256Hash> list = new LinkedList<>();
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(outPoint));
            cursor = blobQuery.query(false, txRefersParentTxTableName, new String[]{"txid"}, "input = ?", null, null, null, null, null);
            while (cursor.moveToNext()) {
               list.add(new Sha256Hash(cursor.getBlob(0)));
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public TransactionOutputEx getParentTransactionOutput(OutPoint outPoint) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(outPoint));
            cursor = blobQuery.query(false, ptxoTableName, new String[]{"height", "value", "isCoinbase", "script"},
                  "outpoint = ?", null, null, null, null, null);
            if (cursor.moveToNext()) {
               return new TransactionOutputEx(outPoint, cursor.getInt(0), cursor.getLong(1),
                     cursor.getBlob(3), cursor.getInt(2) != 0);
            }
            return null;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public boolean hasParentTransactionOutput(OutPoint outPoint) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, SQLiteQueryWithBlobs.outPointToBytes(outPoint));
            cursor = blobQuery.query(false, ptxoTableName, new String[]{"height"}, "outpoint = ?", null, null, null,
                  null, null);
            return cursor.moveToNext();
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public void putTransactions(Collection<? extends TransactionEx> transactions) {
         if (transactions.isEmpty()) {
            return;
         }
         _database.beginTransaction();
         String updateQuery = "INSERT OR REPLACE INTO " + txTableName + " VALUES "
                 + TextUtils.join(",", Collections.nCopies(transactions.size(), " (?,?,?,?,?) "));
         SQLiteStatement updateStatement = _database.compileStatement(updateQuery);
         try {
            int i = 0;
            for (TransactionEx transactionEx: transactions) {
               int index = i * 5;
               updateStatement.bindBlob(index + 1, transactionEx.txid.getBytes());
               updateStatement.bindBlob(index + 2, transactionEx.hash.getBytes());
               updateStatement.bindLong(index + 3, transactionEx.height == -1 ? Integer.MAX_VALUE : transactionEx.height);
               updateStatement.bindLong(index + 4, transactionEx.time);
               updateStatement.bindBlob(index + 5, transactionEx.binary);
               i++;
            }
            updateStatement.executeInsert();

            for (TransactionEx transaction : transactions) {
               putReferencedOutputs(transaction.binary);
            }
            _database.setTransactionSuccessful();
         } finally {
            _database.endTransaction();
         }
      }

      @Override
      public void putTransaction(TransactionEx tx) {
         _insertOrReplaceTx.bindBlob(1, tx.txid.getBytes());
         _insertOrReplaceTx.bindBlob(2, tx.hash.getBytes());
         _insertOrReplaceTx.bindLong(3, tx.height == -1 ? Integer.MAX_VALUE : tx.height);
         _insertOrReplaceTx.bindLong(4, tx.time);
         _insertOrReplaceTx.bindBlob(5, tx.binary);
         _insertOrReplaceTx.executeInsert();

         putReferencedOutputs(tx.binary);
      }

      private void putReferencedOutputs(byte[] rawTx) {
         try {
            final Transaction transaction = Transaction.fromBytes(rawTx);
            final List<OutPoint> refersOutpoint = new ArrayList<>();
            for (TransactionInput input : transaction.inputs) {
               refersOutpoint.add(input.outPoint);
            }
            putTxRefersParentTransaction(transaction.getId(), refersOutpoint);
         } catch (Transaction.TransactionParsingException e) {
            Log.w(LOG_TAG, "Unable to decode transaction: " + e.getMessage());
         }
      }

      @Override
      public TransactionEx getTransaction(Sha256Hash txid) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, txid.getBytes());
            cursor = blobQuery.query(false, txTableName, new String[]{"hash", "height", "time", "binary"}, "id = ?", null,
                  null, null, null, null);
            if (cursor.moveToNext()) {
               int height = cursor.getInt(1);
               if (height == Integer.MAX_VALUE) {
                  height = -1;
               }
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(0));
               return new TransactionEx(txid, hash, height, cursor.getInt(2), cursor.getBlob(3));
            }
            return null;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public void deleteTransaction(Sha256Hash txid) {
         _deleteTx.bindBlob(1, txid.getBytes());
         _deleteTx.execute();
         // also delete all output references for this tx
         deleteTxRefersParentTransaction(txid);
      }

      @Override
      public boolean hasTransaction(Sha256Hash txid) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, txid.getBytes());
            cursor = blobQuery.query(false, txTableName, new String[]{"height"}, "id = ?", null, null, null, null,
                  null);
            return cursor.moveToNext();
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public Collection<TransactionEx> getUnconfirmedTransactions() {
         Cursor cursor = null;
         List<TransactionEx> list = new LinkedList<>();
         try {
            // 2147483647 == Integer.MAX_VALUE
            cursor = _db.rawQuery("SELECT id, hash, time, binary FROM " + txTableName + " WHERE height = 2147483647",
                  new String[]{});
            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               TransactionEx tex = new TransactionEx(txid, hash, -1, cursor.getInt(2),
                     cursor.getBlob(3));
               list.add(tex);
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public Collection<TransactionEx> getYoungTransactions(int maxConfirmations, int blockChainHeight) {
         int maxHeight = blockChainHeight - maxConfirmations + 1;
         Cursor cursor = null;
         List<TransactionEx> list = new LinkedList<>();
         try {
            // return all transaction younger than maxConfirmations or have no confirmations at all
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName + " WHERE height >= ? OR height = -1 ",
                  new String[]{Integer.toString(maxHeight)});
            while (cursor.moveToNext()) {
               int height = cursor.getInt(2);
               if (height == Integer.MAX_VALUE) {
                  height = -1;
               }
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               TransactionEx tex = new TransactionEx(txid, hash, height, cursor.getInt(3),
                     cursor.getBlob(4));
               list.add(tex);
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public void putOutgoingTransaction(Sha256Hash txid, byte[] rawTransaction) {
         _insertOrReplaceOutTx.bindBlob(1, txid.getBytes());
         _insertOrReplaceOutTx.bindBlob(2, rawTransaction);
         _insertOrReplaceOutTx.executeInsert();

         putReferencedOutputs(rawTransaction);
      }

      @Override
      public Map<Sha256Hash, byte[]> getOutgoingTransactions() {
         Cursor cursor = null;
         HashMap<Sha256Hash, byte[]> list = new HashMap<>();
         try {
            cursor = _db.rawQuery("SELECT id, raw FROM " + outTxTableName, new String[]{});
            while (cursor.moveToNext()) {
               list.put(new Sha256Hash(cursor.getBlob(0)), cursor.getBlob(1));
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public void removeOutgoingTransaction(Sha256Hash txid) {
         _deleteOutTx.bindBlob(1, txid.getBytes());
         _deleteOutTx.execute();
      }

      @Override
      public boolean isOutgoingTransaction(Sha256Hash txid) {
         Cursor cursor = null;
         try {
            SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(_db);
            blobQuery.bindBlob(1, txid.getBytes());
            cursor = blobQuery.query(false, outTxTableName, new String[]{}, "id = ?", null, null, null, null,
                  null);
            return cursor.moveToNext();
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public List<TransactionEx> getTransactionHistory(int offset, int limit) {
         Cursor cursor = null;
         List<TransactionEx> list = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                        + " ORDER BY height desc limit ? offset ?",
                  new String[]{Integer.toString(limit), Integer.toString(offset)});
            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               TransactionEx tex = new TransactionEx(txid, hash, cursor.getInt(2),
                     cursor.getInt(3), cursor.getBlob(4));
               list.add(tex);
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

      @Override
      public List<TransactionEx> getTransactionsSince(long since) {
         Cursor cursor = null;
         List<TransactionEx> list = new LinkedList<>();
         try {
            cursor = _db.rawQuery("SELECT id, hash, height, time, binary FROM " + txTableName
                        + " WHERE time >= ?"
                        + " ORDER BY height desc",
                  new String[]{Long.toString(since / 1000)});
            while (cursor.moveToNext()) {
               Sha256Hash txid = new Sha256Hash(cursor.getBlob(0));
               Sha256Hash hash = new Sha256Hash(cursor.getBlob(1));
               TransactionEx tex = new TransactionEx(txid, hash, cursor.getInt(2),
                     cursor.getInt(3), cursor.getBlob(4));
               list.add(tex);
            }
            return list;
         } finally {
            if (cursor != null) {
               cursor.close();
            }
         }
      }

//      @Override
//      public void updateAccountContext(HDAccountContext context) {
//         updateBip44AccountContext(context);
//      }
//
//      @Override
//      public void updateAccountContext(SingleAddressAccountContext context) {
//         updateSingleAddressAccountContext(context);
//      }
   }

   private class OpenHelper extends SQLiteOpenHelper {
      private static final String DATABASE_NAME = "columanagerbacking.db";
      private static final int DATABASE_VERSION = 6;

      OpenHelper(Context context) {
         super(context, DATABASE_NAME, null, DATABASE_VERSION);

         // The backings tables should already exists, but try to recreate them anyhow, as the CREATE TABLE
         // uses the "IF NOT EXISTS" switch
         for (UUID account : getAccountIds(getWritableDatabase())) {
            createAccountBackingTables(account, getWritableDatabase());
         }
      }

      @Override
      public void onCreate(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE single (id TEXT PRIMARY KEY, addresses BLOB, archived INTEGER"
                 + ", blockheight INTEGER, addressType BLOB, coinId TEXT" +
                 ");");
         db.execSQL("CREATE TABLE bip44 (id TEXT PRIMARY KEY, accountIndex INTEGER, archived INTEGER" +
                 ", blockheight INTEGER, indexContexts BLOB, lastDiscovery INTEGER, accountType INTEGER"
                 + ", accountSubId INTEGER, addressType BLOB);");
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

            // add column to store what account type it is
            db.execSQL("ALTER TABLE bip44 ADD COLUMN accountType INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE bip44 ADD COLUMN accountSubId INTEGER DEFAULT 0");
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
               while (cursor.moveToNext()) {
                  UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
                  byte[] addressBytes = cursor.getBlob(1);
                  String addressString = cursor.getString(2);
                  Address address = new Address(addressBytes, addressString);
                  boolean isArchived = cursor.getInt(3) == 1;
                  int blockHeight = cursor.getInt(4);
                  list.add(new SingleAddressAccountContext(id, ImmutableMap.of(address.getType(), address), isArchived, blockHeight, AddressType.P2SH_P2WPKH));
               }
            } finally {
               if (cursor != null) {
                  cursor.close();
               }
            }
            db.execSQL("CREATE TABLE single_new (id TEXT PRIMARY KEY, addresses BLOB, archived INTEGER, blockheight INTEGER, addressType BLOB);");
            SQLiteStatement statement = db.compileStatement("INSERT OR REPLACE INTO single_new VALUES (?,?,?,?,?)");
            for (SingleAddressAccountContext context : list) {
               statement.bindBlob(1, uuidToBytes(context.getId()));
               ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
               try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
                  objectOutputStream.writeObject(context.getAddresses());
                  statement.bindBlob(2, byteStream.toByteArray());
               } catch (IOException ignore) {
                  // should never happen
               }

               statement.bindLong(3, context.isArchived() ? 1 : 0);
               statement.bindLong(4, context.getBlockHeight());

               byteStream = new ByteArrayOutputStream();
               try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
                  objectOutputStream.writeObject(context.getDefaultAddressType());
                  statement.bindBlob(5, byteStream.toByteArray());
               } catch (IOException ignore) {
                  // should never happen
               }

               statement.executeInsert();
            }
            db.execSQL("ALTER TABLE single RENAME TO single_old");
            db.execSQL("ALTER TABLE single_new RENAME TO single");
            db.execSQL("DROP TABLE single_old");

            //Migrate BIP44 accounts
            List<HDAccountContext> bip44List = new ArrayList<>();
            try {
               SQLiteQueryWithBlobs blobQuery = new SQLiteQueryWithBlobs(db);
               cursor = blobQuery.query(
                       false, "bip44",
                       new String[]{"id", "accountIndex", "archived", "blockheight",
                               "lastExternalIndexWithActivity", "lastInternalIndexWithActivity",
                               "firstMonitoredInternalIndex", "lastDiscovery", "accountType", "accountSubId"},
                       null, null, null, null, "accountIndex", null);

               while (cursor.moveToNext()) {
                  UUID id = SQLiteQueryWithBlobs.uuidFromBytes(cursor.getBlob(0));
                  int accountIndex = cursor.getInt(1);
                  boolean isArchived = cursor.getInt(2) == 1;
                  int blockHeight = cursor.getInt(3);
                  int lastExternalIndexWithActivity = cursor.getInt(4);
                  int lastInternalIndexWithActivity = cursor.getInt(5);
                  int firstMonitoredInternalIndex = cursor.getInt(6);
                  long lastDiscovery = cursor.getLong(7);
                  int accountType = cursor.getInt(8);
                  int accountSubId = (int) cursor.getLong(9);
                  Map<BipDerivationType, AccountIndexesContext> indexesContextMap = new HashMap<>();
                  AccountIndexesContext oldIndexes = new AccountIndexesContext(
                          lastExternalIndexWithActivity, lastInternalIndexWithActivity, firstMonitoredInternalIndex);
                  indexesContextMap.put(BipDerivationType.BIP44, oldIndexes);
                  bip44List.add(new HDAccountContext(id, accountIndex, isArchived, blockHeight, lastDiscovery,
                          indexesContextMap, accountType, accountSubId));
               }
            } finally {
               if (cursor != null) {
                  cursor.close();
               }
            }
            //db.execSQL("CREATE TABLE bip44 (id TEXT PRIMARY KEY, accountIndex INTEGER, archived INTEGER, blockheight INTEGER, lastExternalIndexWithActivity INTEGER, lastInternalIndexWithActivity INTEGER, firstMonitoredInternalIndex INTEGER, lastDiscovery, accountType INTEGER, accountSubId INTEGER);");
            db.execSQL("CREATE TABLE bip44_new (id TEXT PRIMARY KEY, accountIndex INTEGER, archived INTEGER, " +
                    "blockheight INTEGER, indexContexts BLOB, lastDiscovery INTEGER, accountType INTEGER, accountSubId " +
                    "INTEGER, addressType BLOB);");
            SQLiteStatement bip44Update = db.compileStatement("INSERT OR REPLACE INTO bip44_new" +
                    " VALUES (?,?,?,?,?,?,?,?,?)");
            for (HDAccountContext context : bip44List) {
               bip44Update.bindBlob(1, uuidToBytes(context.getId()));
               bip44Update.bindLong(2, context.getAccountIndex());
               bip44Update.bindLong(3, context.isArchived() ? 1 : 0);
               bip44Update.bindLong(4, context.getBlockHeight());
               ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
               try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
                  objectOutputStream.writeObject(context.getIndexesMap());
                  bip44Update.bindBlob(5, byteStream.toByteArray());
               } catch (IOException ignore) {
                  // should never happen
               }
               bip44Update.bindLong(6, context.getLastDiscovery());
               bip44Update.bindLong(7, context.getAccountType());
               bip44Update.bindLong(8, context.getAccountSubId());
               byteStream = new ByteArrayOutputStream();
               try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream)) {
                  objectOutputStream.writeObject(context.getDefaultAddressType());
                  bip44Update.bindBlob(9, byteStream.toByteArray());
               } catch (IOException ignore) {
                  // should never happen
               }
               bip44Update.executeInsert();
            }
            db.execSQL("ALTER TABLE bip44 RENAME TO bip44_old");
            db.execSQL("ALTER TABLE bip44_new RENAME TO bip44");
            db.execSQL("DROP TABLE bip44_old");
         }
         if(oldVersion < 6) {
            db.execSQL("ALTER TABLE single ADD COLUMN coinId TEXT");
         }
      }

      @Override
      public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         //We don't really support downgrade but some android devices need this empty method
      }
   }
}

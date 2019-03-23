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
import android.text.TextUtils;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;

import java.math.BigDecimal;
import java.util.*;

public class MetadataStorage extends GenericMetadataStorage {
   private static final MetadataCategory COINAPULT = new MetadataCategory("coinapult_adddr");
   private static final MetadataCategory ADDRESSLABEL_CATEGORY = new MetadataCategory("addresslabel");
   private static final MetadataCategory ACCOUNTLABEL_CATEGORY = new MetadataCategory("al");
   private static final MetadataCategory ARCHIVED = new MetadataCategory("archived");
   private static final MetadataCategory TRANSACTION_LABEL_CATEGORY = new MetadataCategory("tl");
   private static final MetadataCategory OTHER_ACCOUNT_BACKUPSTATE = new MetadataCategory("single_key_bs");
   private static final MetadataCategory PAIRED_SERVICES_CATEGORY = new MetadataCategory("paired_services");

   private static final MetadataCategory EXCHANGE_RATES_CATEGORY = new MetadataCategory("exchange_rates");

   // various key value fields info for colu
   private static final MetadataCategory COLU = new MetadataCategory("colu_data");
   // associates asset label for each assetId
   private static final MetadataCategory COLU_ASSET_LABEL_CATEGORY = new MetadataCategory("colu_asset_labels");
   // associates all asset data for each assetId
   private static final MetadataCategory COLU_ASSET_DATA_CATEGORY = new MetadataCategory("colu_asset_data");

   private static final MetadataKeyCategory SEED_BACKUPSTATE = new MetadataKeyCategory("seed", "backupstate");
   private static final MetadataKeyCategory PIN_RESET_BLOCKHEIGHT = new MetadataKeyCategory("pin", "reset_blockheight");
   private static final MetadataKeyCategory PIN_BLOCKHEIGHT = new MetadataKeyCategory("pin", "blockheight");
   private static final MetadataKeyCategory SYNC_LAST_FULLSYNC = new MetadataKeyCategory("lastFull", "sync");
   private static final MetadataKeyCategory SHOW_BIP44_PATH = new MetadataKeyCategory("ui", "show_bip44_path");
   private static final MetadataKeyCategory SWISH_CREDIT_CARD_IS_ENABLED = new MetadataKeyCategory("swish_cc", "enable");
   private static final MetadataKeyCategory SIMPLEX_IS_ENABLED = new MetadataKeyCategory("simplex", "enable");
   private static final MetadataKeyCategory CHANGELLY_IS_ENABLED = new MetadataKeyCategory("changelly", "enable");
   private static final String EMAIL = "email";
   public static final String PAIRED_SERVICE_COINAPULT = "coinapult";
   public static final String PAIRED_SERVICE_COLU = "colu";

   public MetadataStorage(Context context) {
      super(context);
   }

   public void storeTransactionLabel(Sha256Hash txid, String label) {
      if (!Strings.isNullOrEmpty(label)) {
         storeKeyCategoryValueEntry(TRANSACTION_LABEL_CATEGORY.of(txid.toString()), label);
      } else {
         // remove the transaction label
         deleteByKeyCategory(TRANSACTION_LABEL_CATEGORY.of(txid.toString()));
      }
   }

   public String getLabelByTransaction(Sha256Hash txid) {
      return getKeyCategoryValueEntry(TRANSACTION_LABEL_CATEGORY.of(txid.toString()), "");
   }

   public String getLabelByAccount(UUID account) {
      return getKeyCategoryValueEntry(ACCOUNTLABEL_CATEGORY.of(account.toString()), "");
   }

   public Optional<UUID> getAccountByLabel(String label) {
      Optional<String> account = getFirstKeyForCategoryValue(ACCOUNTLABEL_CATEGORY, label);

      if (account.isPresent()) {
         return Optional.of(UUID.fromString(account.get()));
      } else {
         return Optional.absent();
      }
   }

   public void storeAccountLabel(UUID account, String label) {
      if (!Strings.isNullOrEmpty(label)) {
         storeKeyCategoryValueEntry(ACCOUNTLABEL_CATEGORY.of(account.toString()), label);
      }
   }

   // Removes all metadata (account label,...) from the database
   public void deleteAccountMetadata(UUID account) {
      deleteAllByKey(account.toString());
   }

   public Map<Address, String> getAllAddressLabels() {
      Map<String, String> entries = getKeysAndValuesByCategory(ADDRESSLABEL_CATEGORY);
      Map<Address, String> addresses = new HashMap<>();
      for (Map.Entry<String, String> e : entries.entrySet()) {
         String val = e.getValue();
         String key = e.getKey();
         addresses.put(Address.fromString(key), val);
      }
      return addresses;
   }

   public String getLabelByAddress(Address address) {
      return getKeyCategoryValueEntry(ADDRESSLABEL_CATEGORY.of(address.toString()), "");
   }

   public void deleteAddressMetadata(Address address) {
      // delete everything related to this address from metadata
      deleteAllByKey(address.toString());
   }

   public Optional<Address> getAddressByLabel(String label) {
      Optional<String> address = getFirstKeyForCategoryValue(ADDRESSLABEL_CATEGORY, label);

      if (address.isPresent()) {
         return Optional.of(Address.fromString(address.get()));
      } else {
         return Optional.absent();
      }
   }

   public void storeAddressLabel(Address address, String label) {
      if (!Strings.isNullOrEmpty(label)) {
         storeKeyCategoryValueEntry(ADDRESSLABEL_CATEGORY.of(address.toString()), label);
      }
   }

   public boolean firstMasterseedBackupFinished() {
      return getMasterSeedBackupState().equals(BackupState.VERIFIED);
   }

   public BackupState getMasterSeedBackupState() {
      return BackupState.fromString(
              getKeyCategoryValueEntry(SEED_BACKUPSTATE, BackupState.UNKNOWN.toString())
      );
   }

   public BackupState getOtherAccountBackupState(UUID accountId) {
      return BackupState.fromString(
              getKeyCategoryValueEntry(OTHER_ACCOUNT_BACKUPSTATE.of(accountId.toString()), BackupState.UNKNOWN.toString())
      );
   }

   public void setOtherAccountBackupState(UUID accountId, BackupState state) {
      storeKeyCategoryValueEntry(OTHER_ACCOUNT_BACKUPSTATE.of(accountId.toString()), state.toString());
   }

   public void deleteOtherAccountBackupState(UUID accountId) {
      deleteByKeyCategory(OTHER_ACCOUNT_BACKUPSTATE.of(accountId.toString()));
   }

   public boolean isPairedService(String serviceName) {
      return Boolean.valueOf(getKeyCategoryValueEntry(PAIRED_SERVICES_CATEGORY.of(serviceName), "false"));
   }

   public void setPairedService(String serviceName, boolean paired) {
      storeKeyCategoryValueEntry(PAIRED_SERVICES_CATEGORY.of(serviceName), Boolean.toString(paired));
   }

   public void deleteMasterKeyBackupAgeMs() {
      deleteByKeyCategory(SEED_BACKUPSTATE);
   }

   public Optional<Long> getMasterKeyBackupAgeMs() {
      Optional<String> lastBackup = getKeyCategoryValueEntry(SEED_BACKUPSTATE);
      if (lastBackup.isPresent()) {
         return Optional.of(Calendar.getInstance().getTimeInMillis() - Long.valueOf(lastBackup.get()));
      } else {
         return Optional.absent();
      }
   }

   public void setMasterSeedBackupState(BackupState state) {
      storeKeyCategoryValueEntry(SEED_BACKUPSTATE, state.toString());

      // if this is the first verified backup, remember the date
      if (state == BackupState.VERIFIED && getMasterSeedBackupState() != BackupState.VERIFIED) {
         storeKeyCategoryValueEntry(SEED_BACKUPSTATE, String.valueOf(Calendar.getInstance().getTimeInMillis()));
      }
   }

   public void setResetPinStartBlockheight(int blockChainHeight) {
      storeKeyCategoryValueEntry(PIN_RESET_BLOCKHEIGHT, String.valueOf(blockChainHeight));
   }

   public void clearResetPinStartBlockheight() {
      deleteByKeyCategory(PIN_RESET_BLOCKHEIGHT);
   }

   public Optional<Integer> getResetPinStartBlockHeight() {
      Optional<String> resetIn = getKeyCategoryValueEntry(PIN_RESET_BLOCKHEIGHT);
      if (resetIn.isPresent()) {
         return Optional.of(Integer.valueOf(resetIn.get()));
      } else {
         return Optional.absent();
      }
   }

   public void setLastPinSetBlockheight(int blockChainHeight) {
      storeKeyCategoryValueEntry(PIN_BLOCKHEIGHT, String.valueOf(blockChainHeight));
   }

   public void clearLastPinSetBlockheight() {
      deleteByKeyCategory(PIN_BLOCKHEIGHT);
   }

   public Optional<Integer> getLastPinSetBlockheight() {
      Optional<String> lastSet = getKeyCategoryValueEntry(PIN_BLOCKHEIGHT);
      if (lastSet.isPresent()) {
         return Optional.of(Integer.valueOf(lastSet.get()));
      } else {
         return Optional.absent();
      }
   }

   public boolean getArchived(UUID uuid) {
      return "1".equals(getKeyCategoryValueEntry(ARCHIVED.of(uuid.toString()), "0"));
   }

   public void storeArchived(UUID uuid, boolean archived) {
      storeKeyCategoryValueEntry(ARCHIVED.of(uuid.toString()), archived ? "1" : "0");
   }

   public void storeCoinapultCurrencies(String currencies) {
      storeKeyCategoryValueEntry(COINAPULT.of("currencies"), currencies);
   }

   public String getCoinapultCurrencies() {
      return getKeyCategoryValueEntry(COINAPULT.of("currencies"), "");
   }

   public int getCoinapultLastFlush() {
      try {
         return Integer.valueOf(getKeyCategoryValueEntry(COINAPULT.of("flush"), "0"));
      } catch (NumberFormatException ex) {
         return 0;
      }
   }

   public void storeCoinapultLastFlush(int marker) {
      storeKeyCategoryValueEntry(COINAPULT.of("flush"), Integer.toString(marker));
   }

   public void storeCoinapultAddress(Address address, String forCurrency) {
      storeKeyCategoryValueEntry(COINAPULT.of("last" + forCurrency), address.toString());
   }

   public void deleteCoinapultAddress(String forCurrency) {
      deleteByKeyCategory(COINAPULT.of("last" + forCurrency));
   }

   public Optional<Address> getCoinapultAddress(String forCurrency) {
      Optional<String> last = getKeyCategoryValueEntry(COINAPULT.of("last" + forCurrency));
      if (!last.isPresent()) {
         return Optional.absent();
      }
      return Optional.fromNullable(Address.fromString(last.get()));
   }

   public String getCoinapultMail() {
      Optional<String> mail = getKeyCategoryValueEntry(COINAPULT.of(EMAIL));
      if (mail.isPresent()) {
         return mail.get();
      } else {
         return "";
      }
   }

   public void setCoinapultMail(String mail) {
      storeKeyCategoryValueEntry(COINAPULT.of(EMAIL), mail);
   }

   public void storeColuAssetCoinSupply(String assetIds, BigDecimal value) {
      storeKeyCategoryValueEntry(COLU.of("coinsupply" + assetIds), value.toPlainString());
   }

   public Optional<BigDecimal> getColuAssetCoinSupply(String assetIds) {
      Optional<String> valueEntry = getKeyCategoryValueEntry(COLU.of("coinsupply" + assetIds));
      Optional<BigDecimal> result = Optional.absent();
      if(valueEntry.isPresent()) {
         result = Optional.of(new BigDecimal(valueEntry.get()));
      }
      return result;
   }

   public void storeColuAssetIds(String assetIds) {
      storeKeyCategoryValueEntry(COLU.of("assetIds"), assetIds);
   }

   public Iterable<String> getColuAssetIds() {
      return Splitter.on(",").split(getKeyCategoryValueEntry(COLU.of("assetIds"), ""));
   }

   public void storeColuBalance(UUID coluAccountUuid, String balance) {
      storeKeyCategoryValueEntry(COLU.of("balance" + coluAccountUuid.toString()), balance);
   }

   public Optional<String> getColuBalance(UUID coluAccountUuid) {
      return getKeyCategoryValueEntry(COLU.of("balance" + coluAccountUuid.toString()));
   }


   //Example: currency = "BTC", basecurrency = "USD", market = "Bitstamp", rateValue = "4500.2"
   public void storeExchangeRate(String currency, String baseCurrency, String market, String rateValue) {
      storeKeyCategoryValueEntry(EXCHANGE_RATES_CATEGORY.of(market + "_" + currency + "_" + baseCurrency), rateValue);
   }

   public Map<String, String> getAllExchangeRates() {
      return getKeysAndValuesByCategory(EXCHANGE_RATES_CATEGORY);
   }

   public Optional<String> getExchangeRate(String currency, String baseCurrency, String market) {
      return getKeyCategoryValueEntry(EXCHANGE_RATES_CATEGORY.of(market + "_" + currency + "_" + baseCurrency));
   }

   public void addColuAssetUUIDs(String assetId, UUID uuid) {
      String value;

      UUID[] uuids = getColuAssetUUIDs(assetId);
      if (uuids.length > 0) {
         value = TextUtils.join(",", uuids);
         value += "," + uuid.toString();
      } else {
         value = uuid.toString();
      }
      storeKeyCategoryValueEntry(COLU.of(assetId), value);
   }

   public void removeColuAssetUUIDs(String assetId, UUID uuid) {

      UUID[] uuids = getColuAssetUUIDs(assetId);
      List<UUID> shortenedList = new ArrayList<>();

      for(UUID curUUID : uuids) {
         if (curUUID.equals(uuid))
            continue;
         shortenedList.add(curUUID);
      }

      storeKeyCategoryValueEntry(COLU.of(assetId), TextUtils.join(",", shortenedList));
   }

   public UUID[] getColuAssetUUIDs(String assetId) {
      Optional<String> uuid = getKeyCategoryValueEntry(COLU.of(assetId));

      if (!uuid.isPresent() || uuid.get().length() == 0) {
         return new UUID[]{};
      }

      String[] strUuids = uuid.get().split(",");
      UUID[] uuids = new UUID[strUuids.length];

      for(int i = 0; i < strUuids.length; i++) {
         uuids[i] =UUID.fromString(strUuids[i]);
      }

      return uuids;
   }

   public boolean getSwishCreditCardIsEnabled() {
      return getKeyCategoryValueEntry(SWISH_CREDIT_CARD_IS_ENABLED, "1").equals("1");
   }

   public void setSwishCreditCardIsEnabled(boolean enable) {
      storeKeyCategoryValueEntry(SWISH_CREDIT_CARD_IS_ENABLED, enable ? "1" : "0");
   }

   public boolean getSimplexIsEnabled() {
      return getKeyCategoryValueEntry(SIMPLEX_IS_ENABLED, "1").equals("1");
   }

   public void setSimplexIsEnabled(boolean enable) {
      storeKeyCategoryValueEntry(SIMPLEX_IS_ENABLED, enable ? "1" : "0");
   }

   public boolean getChangellyIsEnabled() {
      return getKeyCategoryValueEntry(CHANGELLY_IS_ENABLED, "1").equals("1");
   }

   public void setChangellyIsEnabled(boolean enable) {
      storeKeyCategoryValueEntry(CHANGELLY_IS_ENABLED, enable ? "1" : "0");
   }

   public Optional<Long> getLastFullSync() {
      final Optional<String> lastDateStr = getKeyCategoryValueEntry(SYNC_LAST_FULLSYNC);
      if (lastDateStr.isPresent()) {
         try {
            return Optional.fromNullable(Long.parseLong(lastDateStr.get()));
         } catch (NumberFormatException ex){
            return Optional.absent();
         }
      } else {
         return Optional.absent();
      }
   }

   public void setLastFullSync(long date){
      storeKeyCategoryValueEntry(SYNC_LAST_FULLSYNC, Long.toString(date));
   }

   public void setShowBip44Path(boolean show) {
      storeKeyCategoryValueEntry(SHOW_BIP44_PATH, show ? "1" : "0");
   }

   public boolean getShowBip44Path() {
      return getKeyCategoryValueEntry(SHOW_BIP44_PATH, "0").equals("1");
   }

   public enum BackupState {
      UNKNOWN(0), VERIFIED(1), IGNORED(2), NOT_VERIFIED(3);

      private final int _index;

      BackupState(int index) {
         _index = index;
      }

      public static BackupState fromString(String state) {
         return fromInt(Integer.parseInt(state));
      }

      public String toString() {
         return Integer.toString(_index);
      }

      public int toInt() {
         return _index;
      }

      public static BackupState fromInt(int integer) {
         switch (integer) {
            case 0:
               return UNKNOWN;
            case 1:
               return VERIFIED;
            case 2:
               return IGNORED;
            case 3:
               return NOT_VERIFIED;
            default:
               return UNKNOWN;
         }
      }
   }
}

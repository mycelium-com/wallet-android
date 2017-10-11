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

import android.os.Handler;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.ScriptOutput;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.ColuTxDetailsItem;
import com.mycelium.wallet.colu.json.Tx;
import com.mycelium.wallet.colu.json.Utxo;
import com.mycelium.wallet.colu.json.Vin;
import com.mycelium.wallet.colu.json.Vout;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.lib.TransactionExApi;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.AccountBacking;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.SynchronizeAbleWalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Bus;
import com.subgraph.orchid.encoders.Hex;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//import com.colu.api.httpclient.ColuClient;

public class ColuAccount extends SynchronizeAbleWalletAccount implements ExportableAccount {

   public static final String TAG = "ColuAccount";

   private static final Balance EMPTY_BALANCE = new Balance(0, 0, 0, 0, 0, 0, true, true);
   private static final BigDecimal SATOSHIS_PER_BTC = BigDecimal.valueOf(100000000);
   public static final int MILLISENCONDS_IN_SECOND = 1000;

   private final ColuManager manager;
   private final Bus eventBus;
   private final Handler handler;
   private final UUID uuid;
   private final ExchangeRateManager exchangeRateManager;
   private final MetadataStorage metadataStorage;
   private List<TransactionSummary> allTransactionSummaries;
   private long satoshiAmount;
   private long satoshiBtcOnlyAmount;

   private final ColuAsset coluAsset;

   // single address mode
   private final Address address;
   private InMemoryPrivateKey accountKey;

   private boolean archived;

   private CurrencyBasedBalance balanceFiat;

   private int height;

   private List<Utxo.Json> utxosList;

   private List<Tx.Json> historyTxList;

   private Collection<TransactionExApi> historyTxInfosList;
   private AccountBacking accountBacking;

   public InMemoryPrivateKey getPrivateKey() {
      return accountKey;
   }

   private SingleAddressAccount linkedAccount;

   private String label;

   public ColuAccount(ColuManager manager, AccountBacking backing, MetadataStorage metadataStorage, Address address,
                      ExchangeRateManager exchangeRateManager, Handler handler, Bus eventBus, WapiLogger logger, ColuAsset coluAsset) {
      this.accountBacking = backing;
      this.manager = manager;
      this.eventBus = eventBus;
      this.handler = handler;
      this.exchangeRateManager = exchangeRateManager;
      this.metadataStorage = metadataStorage;
      this.coluAsset = coluAsset;
      this.satoshiAmount = 0;
      this.address = address;

      uuid = getGuidForAsset(coluAsset, address.getAllAddressBytes());

      archived = metadataStorage.getArchived(uuid);
   }
   public ColuAccount(ColuManager manager, AccountBacking backing, MetadataStorage metadataStorage, InMemoryPrivateKey accountKey,
                      ExchangeRateManager exchangeRateManager, Handler handler, Bus eventBus, WapiLogger logger, ColuAsset coluAsset) {
      this.accountBacking = backing;
      this.manager = manager;
      this.eventBus = eventBus;
      this.handler = handler;
      this.exchangeRateManager = exchangeRateManager;
      this.metadataStorage = metadataStorage;
      this.coluAsset = coluAsset;
      this.satoshiAmount = 0;

      this.accountKey = accountKey;
      this.address = this.accountKey.getPublicKey().toAddress(manager.getNetwork());

      uuid = getGuidForAsset(coluAsset, accountKey.getPublicKey().toAddress(getNetwork()).getAllAddressBytes());

      archived = metadataStorage.getArchived(uuid);

   }

   public static UUID getGuidForAsset(ColuAsset coluAsset, byte[] addressBytes) {
      ByteWriter byteWriter = new ByteWriter(36);
      byteWriter.putBytes(addressBytes);
      byteWriter.putRawStringUtf8(coluAsset.id);
      Sha256Hash accountId = HashUtils.sha256(byteWriter.toBytes());
      return getGuidFromByteArray(accountId.getBytes());
   }

   public static UUID getGuidFromByteArray(byte[] bytes) {
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      long high = bb.getLong();
      long low = bb.getLong();
      return new UUID(high, low);
   }

   public UUID getUuid() {
      return uuid;
   }

   @Override
   public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.OutputTooSmallException {
      //TODO: remove this as we do not send money here ?
      Optional<ExactCurrencyValue> sendValue = com.mycelium.wapi.wallet.currency.CurrencyValue.checkCurrencyAmount(enteredAmount, "UNKNOWN");

      if (balanceFiat == null || sendValue.isPresent() && sendValue.get().getValue().compareTo(balanceFiat.confirmed.getValue()) > 0) {
         //not enough funds
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      }

   }

   /// @brief return true if at least one address in the list belongs to this account
   public boolean ownAddress(List<String> addresses) {
      for (String otherAddress : addresses) {
         if (address.toString().compareTo(otherAddress) == 0) {
            Log.d(TAG, "Address " + otherAddress + "matches with " + address.toString());
            return true;
         }
      }
      return false;
   }

   public ColuAsset getColuAsset() {
      return coluAsset;
   }


   // if it is a fiat value convert it, otherwise take the exact value
   private long getSatoshis(BigDecimal amount, String currency) {
     if (currency.equals("BTC")) {
        return amount.multiply(SATOSHIS_PER_BTC).longValue();
     }
          ExchangeRate rate = exchangeRateManager.getExchangeRate(currency);
          if (rate == null || rate.price == null) {
                 return 0;
          } else {
                  BigDecimal btc = amount.divide(new BigDecimal(rate.price), MathContext.DECIMAL32);
                  return btc.multiply(SATOSHIS_PER_BTC).longValue();
          }
   }

    private long getSatoshis(ExactCurrencyValue confirmed) {
       //TODO: for now we do not associate a currency or BTC value to assets
       return confirmed.getValue().multiply(SATOSHIS_PER_BTC).longValue();
       // if it is a fiat value convert it, otherwise take the exact value
       //return getSatoshis(confirmed.getValue(), confirmed.getCurrency());
    }

   public long getSatoshiAmount() {
      return satoshiAmount;
   }

   public void setBalanceFiat(CurrencyBasedBalance newBalanceFiat) {
      if (balanceFiat != null) {
         Log.d(TAG, "Updating balanceFiat from " + balanceFiat.toString() + " to " + newBalanceFiat.toString());
      } else {
         Log.d(TAG, "Setting initial balance to " + newBalanceFiat.toString());
      }
      balanceFiat = newBalanceFiat;
   }

   public void setBtcOnlyAmount(long satoshiBtcOnlyAmount) {
      this.satoshiBtcOnlyAmount = satoshiBtcOnlyAmount;
   }

   public long getSatoshiBtcOnlyAmount() {
      return satoshiBtcOnlyAmount;
   }

   public void setBalanceSatoshi(long satoshiAmount) {
      this.satoshiAmount = satoshiAmount;
   }

   public synchronized void setUtxos(List<Utxo.Json> utxos) {
      utxosList = utxos;

   }

   public synchronized void setHistory(List<Tx.Json> history) {
      Log.d(TAG, "History updated history.size=" + history.size());
      for (Tx.Json tx : history) {
         Log.d(TAG, "tx " + tx.txid);
      }
      historyTxList = history; // utxosList = utxos;
   }

   public synchronized void setHistoryTxInfos(Collection<TransactionExApi> txInfos) {
      historyTxInfosList = txInfos;
   }


   public List<Address> getSendingAddresses() {
      LinkedList<Address> sendingAddresses = new LinkedList<Address>();
      //TODO: make this dynamic and based on utxo with asset > 0
      sendingAddresses.add(address);
      return sendingAddresses;
   }

   private <T> List<T> limitedList(int offset, int limit, List<T> list) {
      if (offset >= list.size()) {
         return Collections.emptyList();
      }
      int endIndex = Math.min(offset + limit, list.size());
      return new ArrayList<T>(list.subList(offset, endIndex));
   }


   @Override
   public NetworkParameters getNetwork() {
      return manager.getNetwork();
   }

   @Override
   public UUID getId() {
      return uuid;
   }

   @Override
   public void setAllowZeroConfSpending(boolean allowZeroConfSpending) {
   }

   @Override
   public int getBlockChainHeight() {
      return height;
   }

   public void setBlockChainHeight(int height) {
      this.height = height;
   }

   @Override
   public Optional<Address> getReceivingAddress() {
      return Optional.of(address);
   }

   @Override
   public boolean canSpend() {
      return accountKey != null;
   }

   @Override
   public Balance getBalance() {
      //if (balanceColu == null) {
      if (balanceFiat == null) {
         return EMPTY_BALANCE;
      } else {
         return new Balance(getSatoshis(balanceFiat.confirmed),
                 getSatoshis(balanceFiat.receiving),
                 getSatoshis(balanceFiat.sending),
                 0, 0, 0, false, true);
      }
   }

   @Override
   public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
      if (historyTxList != null) {
         List<TransactionSummary> list = getTransactionSummaries();
         ArrayList<TransactionSummary> result = new ArrayList<>();
         for (TransactionSummary transactionSummary : list) {
            if(transactionSummary.value.getCurrency().equals(coluAsset.name)) {
               result.add(transactionSummary);
            }
         }
         return limitedList(offset, limit, result);

      } else {
         return Lists.newArrayList();
      }
   }

   private List<TransactionSummary> getTransactionSummaries() {
      //TODO: optimization - if allTransactionSummaries is not null and last one matches last utxo
      // return list immediately instead of re generating it
      allTransactionSummaries = new ArrayList<>();

      for (Tx.Json tx : historyTxList) {
         Sha256Hash hash = new Sha256Hash(Hex.decode(tx.txid));

         // look up for blockchain standard info coming from wapi server
         TransactionExApi extendedInfo = null;

         if (historyTxInfosList != null) {
            for (TransactionExApi tex : historyTxInfosList) {
               if (tex.txid.compareTo(hash) == 0) {
                  extendedInfo = tex;
                  Log.d(TAG, "Found additional blockchain data for hash " + hash);
                  break;
               }
            }
         } else {
            Log.d(TAG, "historyTxInfosList is null, ignoring it for allTransactionSummaries.");
         }

         if (extendedInfo == null) {
            Log.d(TAG, "Extended info for hash " + hash + " not found !");
         }

         // is it a BTC transaction or an asset transaction ?
         // count assets and BTC
         long outgoingAsset = 0;
         long outgoingSatoshi = 0;
         for (Vin.Json vin : tx.vin) {
            if (vin.assets.size() > 0) {
               if (vin.previousOutput.addresses != null && vin.previousOutput.addresses.contains(this.address.toString())) {
                  for (Asset.Json anAsset : vin.assets) {
                     if (anAsset.assetId.contentEquals(coluAsset.id)) {
                        outgoingAsset = outgoingAsset + anAsset.amount;
                     }
                  }
               }
            } else {
               if (vin.previousOutput.addresses != null && vin.previousOutput.addresses.contains(this.address.toString())) {
                  outgoingSatoshi += vin.value;
               }
            }

         }

         Log.d(TAG, "Debug: valueAsset=" + outgoingAsset);
         Log.d(TAG, "Debug: valueSatoshi=" + outgoingSatoshi);


         long incomingAsset = 0;
         long incomingSatoshi = 0;

         List<Address> toAddresses = new ArrayList<>();
         Optional<Address> destinationAddress = null;

         for (Vout.Json vout : tx.vout) {

            if (vout.scriptPubKey.addresses != null) {
               for(String address : vout.scriptPubKey.addresses) {
                  toAddresses.add(Address.fromString(address));
               }
               if (vout.scriptPubKey.addresses.size() > 0) {
                  Address address = Address.fromString(vout.scriptPubKey.addresses.get(0));
                  if(!isMine(address)) {
                     destinationAddress = Optional.fromNullable(address);
                  }
               }
            }

            if (vout.assets.size() > 0) {
               if (vout.scriptPubKey.addresses != null && vout.scriptPubKey.addresses.contains(this.address.toString())) {
                  for (Asset.Json anAsset : vout.assets) {
                     if (anAsset.assetId.contentEquals(coluAsset.id)) {
                        incomingAsset = incomingAsset + anAsset.amount;
                     }
                  }
                  break;
               }
            } else {
               if (vout.scriptPubKey.addresses != null && vout.scriptPubKey.addresses.contains(this.address.toString())) {
                  incomingSatoshi += vout.value;
               }
            }
         }

         BigDecimal valueBigDecimal;
         ExactCurrencyValue value;

         long assetBalance = incomingAsset - outgoingAsset;
         long satoshiBalance = incomingSatoshi - outgoingSatoshi;

         boolean isIncoming;

         if (assetBalance != 0) {
            isIncoming = assetBalance > 0;
            valueBigDecimal = BigDecimal.valueOf(Math.abs(assetBalance), coluAsset.scale);
            value = ExactCurrencyValue.from(valueBigDecimal, coluAsset.name);
         } else if (satoshiBalance != 0) {
            isIncoming = satoshiBalance > 0;
            valueBigDecimal = BigDecimal.valueOf(Math.abs(satoshiBalance), 8);
            value = ExactCurrencyValue.from(valueBigDecimal, "BTC");
         } else {
            //We can hypothetically have a situation when we our Colu address received assets with another assetId (not RMC)
            //So we should not display this transaction here as it doesn't relate to RMC asset
            continue;
         }

         long time = 0;
         int height = (int)tx.blockheight;
         boolean isQueuedOutgoing = false;

         if (extendedInfo != null) {
            time = extendedInfo.time;
         } else {
            //Since we merge transactions information from two data sources - colu server and WAPI server, there might be a data syncronization issue -
            //colu server detects recently added transactions earlier then WAPI server, so at some moment they may return different numbers of transactions available
            //So for transaction retuned inside colu list we may not found the corresponding transaction in the list returned by WAPI
            //The syncronization lag seems to be small, so we can return current time
            time = new Date().getTime();
         }

         int confirmations = tx.confirmations;

         if (destinationAddress == null) {
            destinationAddress = Optional.absent();
         }
         if (destinationAddress != null && destinationAddress.isPresent()) {
            Log.d(TAG, "getTransactionSummaries: creating transaction summary: hash=" + hash

                    + " time=" + time + " height=" + height + " confirmations="
                    + confirmations + " destinationAddress=" + destinationAddress.get().toString());
         } else {
            Log.d(TAG, "getTransactionSummaries: creating transaction summary: hash=" + hash

                    + " time=" + time + " height=" + height + " confirmations="
                    + confirmations);
         }
         //ExactCurrencyValue value = ExactCurrencyValue.from(new BigDecimal(0), CurrencyValue.BTC);
         TransactionSummary summary = new TransactionSummary(hash, value, isIncoming, time,
                 height, confirmations,
                 isQueuedOutgoing, null, destinationAddress, toAddresses);
         allTransactionSummaries.add(summary);
      }
      Log.d(TAG, "getTransactionSummaries: returning " + allTransactionSummaries.size() + " transactions.");

      Collections.sort(allTransactionSummaries, new Comparator<TransactionSummary>(){
         public int compare(TransactionSummary p1, TransactionSummary p2){
            return (int)(p2.time - p1.time);
         }
      });

      return allTransactionSummaries;
   }

   @Override
   public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
      if (historyTxList != null) {
         List<TransactionSummary> list = getTransactionSummaries();
         final ArrayList<TransactionSummary> result = new ArrayList<TransactionSummary>();
         for (TransactionSummary item : list) {
            if (item.time * MILLISENCONDS_IN_SECOND < receivingSince) {
               break;
            }
            result.add(item);
         }
         return result;
      } else {
         return Lists.newArrayList();
      }
   }

   @Override
   public TransactionSummary getTransactionSummary(Sha256Hash txid) {
      //TODO: call getTransactionSummaries to always work on fresh data
      if (allTransactionSummaries != null) {
         for (TransactionSummary summary : allTransactionSummaries) {
            if (summary.txid.compareTo(txid) == 0) {
               return summary;
            }
         }
      }
      return null;
   }

   private Tx.Json GetColuTransactionInfoById(Sha256Hash txid) {
      for (Tx.Json tx : historyTxList) {
         if (tx.txid.equals(txid.toString())) {
            return tx;
         }
      }
      return null;
   }

   public TransactionDetails getTransactionDetails(Sha256Hash txid) {
      TransactionDetails details = null;
      TransactionSummary summary = getTransactionSummary(txid);
      Log.d(TAG, "getTransactionDetails: " + txid);
      if (summary != null) {
         Log.d(TAG, "getTransactionDetails: summary is not null: height=" + summary.height
                 + " summary.time: " + summary.time);

         // parsing additional data
         TransactionExApi extendedInfo = null;
         if (historyTxInfosList != null) {
            for (TransactionExApi tex : historyTxInfosList) {
               if (tex.txid.compareTo(txid) == 0) {
                  extendedInfo = tex;
                  Log.d(TAG, "Found additional blockchain data for hash " + txid);
                  break;
               }
            }
         } else {
            Log.d(TAG, "historyTxInfosList is null, ignoring it for allTransactionSummaries.");
         }

         if (extendedInfo == null) {
            Log.d(TAG, "Extended info for hash " + txid + " not found !");
         }

         Transaction tx = TransactionEx.toTransaction(extendedInfo);
         if (tx == null) {
            throw new RuntimeException();
         }

         Tx.Json coluTxInfo = GetColuTransactionInfoById(txid);

         List<TransactionDetails.Item> inputs = new ArrayList<>();

         if (tx.isCoinbase()) {
            // We have a coinbase transaction. Create one input with the sum of the outputs as its value,
            // and make the address the null address
            long value = 0;
            for (TransactionOutput out : tx.outputs) {
               value += out.value;
            }
            inputs.add(new TransactionDetails.Item(Address.getNullAddress(getNetwork()), value, true));
         } else {
            for (Vin.Json vin : coluTxInfo.vin) {
               if (vin.assets.size() > 0) {
                     for (Asset.Json anAsset : vin.assets) {
                        if (anAsset.assetId.contentEquals(coluAsset.id) && vin.previousOutput.addresses.size() > 0) {
                           inputs.add(new ColuTxDetailsItem(Address.fromString(vin.previousOutput.addresses.get(0)), vin.value, false, anAsset.amount, anAsset.divisibility));
                        }
                     }
               } else {
                     if (vin.previousOutput.addresses.size() > 0)
                        inputs.add(new TransactionDetails.Item(Address.fromString(vin.previousOutput.addresses.get(0)), vin.value, false));
               }
            }
         }

         List<TransactionDetails.Item> outputs = new ArrayList<>();

         for (Vout.Json vout : coluTxInfo.vout) {
            if (vout.assets.size() > 0) {
                  for (Asset.Json anAsset : vout.assets) {
                     if (anAsset.assetId.contentEquals(coluAsset.id) && vout.scriptPubKey.addresses.size() > 0) {
                        outputs.add(new ColuTxDetailsItem(Address.fromString(vout.scriptPubKey.addresses.get(0)), vout.value, false, anAsset.amount, anAsset.divisibility));
                     }
                  }
            } else {
               if (vout.value > 0 && vout.scriptPubKey.addresses.size() > 0)
                  outputs.add(new TransactionDetails.Item(Address.fromString(vout.scriptPubKey.addresses.get(0)), vout.value, false));
            }
         }

         details = new TransactionDetails(
                 txid, extendedInfo.height, extendedInfo.time,
                 inputs.toArray(new TransactionDetails.Item[inputs.size()]),
                 outputs.toArray(new TransactionDetails.Item[outputs.size()]),
                 extendedInfo.binary.length);
      }

      return details;
   }

// archive and activation are identical to Coinapult. Nothing to modify.

   @Override
   public boolean isArchived() {
      return !isActive();
   }

   @Override
   public boolean isActive() {
      return !archived;
   }

   @Override
   public void archiveAccount() {
      archived = true;
      metadataStorage.storeArchived(uuid, true);
   }

   @Override
   public void activateAccount() {
      archived = false;
      metadataStorage.storeArchived(uuid, false);
   }

   @Override
   public void dropCachedData() {
      utxosList = null;
   }

   @Override
   public Data getExportData(KeyCipher cipher) {
      Optional<String> privateKey = Optional.absent();
      if (canSpend()) {
         privateKey = Optional.of(accountKey.getBase58EncodedPrivateKey(manager.getNetwork()));
      }
      Optional<String> pubKey = Optional.of(address.toString());
      return new Data(privateKey, pubKey);
   }

   @Override
   public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      throw new IllegalStateException("not supported, use prepareColuTX instead");
   }

   @Override
   public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      return null;
   }

   @Override
   public com.mrd.bitlib.model.Transaction signTransaction(
           StandardTransactionBuilder.UnsignedTransaction unsigned,
           KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
      Log.d(TAG, "Do not use this method ");
      return null;
   }

   @Override
   public boolean broadcastOutgoingTransactions() {
      return true;
   }

   /**
    * Determine whether a transaction output was sent from one of our own
    * addresses
    *
    * @param output the output to investigate
    * @return true iff the putput was sent from one of our own addresses
    */
   protected boolean isMine(TransactionOutputEx output) {
      ScriptOutput script = ScriptOutput.fromScriptBytes(output.script);
      return isMine(script);
   }

   /**
    * Determine whether an output script was created by one of our own addresses
    *
    * @param script the script to investigate
    * @return true iff the script was created by one of our own addresses
    */
   protected boolean isMine(ScriptOutput script) {
      Address address = script.getAddress(getNetwork());
      return isMine(address);
   }

   @Override
   public boolean isMine(Address address) {
      // there might be more, but currently we only know about this one...
      Optional<Address> receivingAddress = getReceivingAddress();
      return receivingAddress.isPresent() && receivingAddress.get().equals(address);
   }

   @Override
   protected boolean doSynchronization(SyncMode mode) {
      try {
         manager.updateAccountBalance(this);
         if(linkedAccount != null) {
            linkedAccount.doSynchronization(SyncMode.NORMAL);
         }
      } catch (IOException e) {
         Optional<String> balance = manager.getColuBalance(this.uuid);
         if (balance.isPresent()) {
            ExactCurrencyValue confirmed = ExactCurrencyValue.from(new BigDecimal(balance.get()), getColuAsset().name);
            setBalanceFiat(new CurrencyBasedBalance(confirmed, ExactCurrencyValue.from(BigDecimal.ZERO, getColuAsset().name), ExactCurrencyValue.from(BigDecimal.ZERO, getColuAsset().name)));
         }
      }
      return true;
   }

   @Override
   public BroadcastResult broadcastTransaction(com.mrd.bitlib.model.Transaction transaction) {
      return null;
   }

   @Override
   public TransactionEx getTransaction(Sha256Hash txid) {
      TransactionEx tex = null;
      for (Utxo.Json utxo : utxosList) {
         if (utxo.txid.contentEquals(txid.toString())) {
            Sha256Hash tHash = new Sha256Hash(Hex.decode(utxo.txid));
            tex = new TransactionEx(tHash,
                    utxo.blockheight,
                    utxo.blockheight,
                    null);
         }
      }

      return tex;
   }

   @Override
   public void queueTransaction(TransactionEx transaction) {

   }

   @Override
   public boolean cancelQueuedTransaction(Sha256Hash transactionId) {
      return false;
   }

   @Override
   public boolean deleteTransaction(Sha256Hash transactionId) {
      return false;
   }

   @Override
   public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse) {
      return getCurrencyBasedBalance().confirmed;
   }
   @Override
   public boolean isValidEncryptionKey(KeyCipher cipher) {
      return false;
   }

   @Override
   public boolean isDerivedFromInternalMasterseed() {
      return false;
   }

   @Override
   public boolean isOwnInternalAddress(Address address) {
      return false;
   }

   @Override
   public StandardTransactionBuilder.UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce) {
      return null;
   }

   @Override
   public boolean isOwnExternalAddress(Address address) {
      return isMine(address);
   }

   @Override
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
      return new ArrayList<>();
   }

   /// returns all utxo associated with this address
   public List<Utxo.Json> getAddressUnspent(String address) {
      LinkedList<Utxo.Json> addressUnspent = new LinkedList<>();
      if (utxosList != null) {
         for (Utxo.Json utxo : utxosList) {
            for (String addr : utxo.scriptPubKey.addresses) {
               if (address.compareTo(addr) == 0) {
                  addressUnspent.add(utxo);
                  break;
               } else {
                  Log.d(TAG, " skipping utxo " + utxo.txid + ":" + utxo.index + " addr=" + addr);
               }
            }
            if (utxo.scriptPubKey.addresses.size() == 0) {
               Log.d(TAG, "addresses list is zero, skipping utxo " + utxo.txid + ":" + utxo.index);
            }
         }
      }
      return addressUnspent;
   }

   public CurrencyBasedBalance getCurrencyBasedBalance() {
      if (balanceFiat != null) {
         return balanceFiat;
      } //result precomputed from accountHistory query

      // if we dont have a balance, return 0 in the accounts native currency
      ExactCurrencyValue zero = ExactCurrencyValue.from(BigDecimal.ZERO, getColuAsset().name);
      return new CurrencyBasedBalance(zero, zero, zero, true);
   }

   @Override
   public boolean onlySyncWhenActive() {
      return true;
   }

   public String getDefaultLabel() {
      return coluAsset.label;
   }

   public static class Currency {
      public static final Currency USD = new Currency("USD", BigDecimal.ONE);
      public static final Currency EUR = new Currency("EUR", BigDecimal.ONE);
      public static final Currency GBP = new Currency("GBP", BigDecimal.ONE);
      public static final Currency BTC = new Currency("BTC", BigDecimal.ZERO);
      public static final Map<String, Currency> all = ImmutableMap.of(
              USD.name, USD,
              EUR.name, EUR,
              GBP.name, GBP,
              BTC.name, BTC
      );

      final public String name;
      final public BigDecimal minimumConversationValue;

      private Currency(String name, BigDecimal minimumConversationValue) {
         this.name = name;
         this.minimumConversationValue = minimumConversationValue;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }

         Currency currency = (Currency) o;

         if (!name.equals(currency.name)) {
            return false;
         }
         return minimumConversationValue.equals(currency.minimumConversationValue);
      }

      @Override
      public int hashCode() {
         int result = name.hashCode();
         result = 31 * result + minimumConversationValue.hashCode();
         return result;
      }
   }

   public enum ColuAssetType {
      MT,
      MASS,
      RMC;

      public static ColuAssetType parse(String type) {
         if (type.equalsIgnoreCase("mt")) {
            return MT;
         } else if (type.equalsIgnoreCase("mass") || type.equalsIgnoreCase("mss")) {
            return MASS;
         } else if (type.equalsIgnoreCase("rmc")) {
            return RMC;
         }
         return null;
      }
   }

   public static class ColuAsset {
      private static final ColuAsset assetMT = new ColuAsset(ColuAssetType.MT, "MT","MT", BuildConfig.MTAssetID, 7, "5babce48bfeecbcca827bfea5a655df66b3abd529e1f93c1264cb07dbe2bffe8/0");
      private static final ColuAsset assetMass = new ColuAsset(ColuAssetType.MASS, "MSS", "MSS", BuildConfig.MassAssetID, 0, "ff3a31bef5aad630057ce3985d7df31cae5b5b91343e6216428a3731c69b0441/0");
      private static final ColuAsset assetRMC = new ColuAsset(ColuAssetType.RMC, "RMC", "RMC", BuildConfig.RMCAssetID, 4, "");

      private static final Map<String, ColuAsset> assetMap = ImmutableMap.of(
              assetMT.id, assetMT,
              assetMass.id, assetMass,
              assetRMC.id, assetRMC
      );

       public static Map<String, ColuAsset> getAssetMap() {
          return assetMap;
      }

      public static final List<String> getAllAssetNames() {
         LinkedList<String> assetNames = new LinkedList<String>();
         for (ColuAsset asset : getAssetMap().values()) {
            assetNames.add(asset.name);
         }
         return assetNames;
      }

      public static final ColuAsset getByType(ColuAssetType assetType) {
            switch (assetType) {
               case MT:
                  return assetMT;
               case MASS:
                  return assetMass;
               case RMC:
                  return assetRMC;
            }
            return null;
      }

      final public String label;
      final public String name;
      final public String id;
      final public int scale; // number of fractional digits
      final public String issuance;
      final public ColuAssetType assetType;

      private ColuAsset(ColuAssetType assetType, String label, String name, String id, int scale, String issuance) {
         this.assetType= assetType;
         this.label = label;
         this.name = name;
         this.id = id;
         this.scale = scale;
         this.issuance = issuance;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }
         if (o == null || getClass() != o.getClass()) {
            return false;
         }

         ColuAsset asset = (ColuAsset) o;

         if (!name.equals(asset.name)) {
            return false;
         }
         return id.equals(asset.id);
      }

   }

   @Override
   public String getAccountDefaultCurrency() {
      return getColuAsset().name;
   }

   public SingleAddressAccount getLinkedAccount() {
      return linkedAccount;
   }

   public void setLinkedAccount(SingleAddressAccount linkedAccount) {
      this.linkedAccount = linkedAccount;
   }

   public Address getAddress() {
      return address;
   }

   public void forgetPrivateKey() {
      accountKey = null;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }
}

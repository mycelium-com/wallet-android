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

package com.mycelium.wallet.coinapult;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import com.coinapult.api.httpclient.AccountInfo;
import com.coinapult.api.httpclient.CoinapultClient;
import com.coinapult.api.httpclient.Transaction;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.*;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.SynchronizeAbleWalletAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.squareup.otto.Bus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;

public class CoinapultAccount extends SynchronizeAbleWalletAccount {
   private static final Balance EMPTY_BALANCE = new Balance(0, 0, 0, 0, 0, 0, true, true);
   private static final BigDecimal SATOSHIS_PER_BTC = BigDecimal.valueOf(100000000);

   public static final Predicate<Transaction.Json> TX_NOT_CONVERSION = Predicates.not(new Predicate<com.coinapult.api.httpclient.Transaction.Json>() {
      @Override
      public boolean apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
         return input.type.equals("conversion");
      }
   });

   private final CoinapultManager manager;
   private final Bus eventBus;
   private final Handler handler;
   private final UUID uuid;
   private final ExchangeRateManager exchangeRateManager;
   private final MetadataStorage metadataStorage;

   private final Currency coinapultCurrency;

   private boolean archived;
   private List<com.coinapult.api.httpclient.Transaction.Json> accountHistory;

   // list to hold local generated transactions, to ensure they show up in the tx-hist list correctly
   private List<com.coinapult.api.httpclient.Transaction.Json> extraHistory = Lists.newArrayList();

   private Optional<Address> currentAddress = Optional.absent();
   private CurrencyBasedBalance balanceFiat;


   public CoinapultAccount(CoinapultManager manager, MetadataStorage metadataStorage, InMemoryPrivateKey accountKey,
                           ExchangeRateManager exchangeRateManager, Handler handler, Bus eventBus, WapiLogger logger, Currency coinapultCurrency) {
      this.manager = manager;
      this.eventBus = eventBus;
      this.handler = handler;
      this.exchangeRateManager = exchangeRateManager;
      this.metadataStorage = metadataStorage;
      this.coinapultCurrency = coinapultCurrency;

      // derive the UUID for the account from the "sha256(PubKey(AccountPrivateKey) || <FiatCurrency>)"
      ByteWriter byteWriter = new ByteWriter(36);
      byteWriter.putBytes(accountKey.getPublicKey().getPublicKeyBytes());
      byteWriter.putRawStringUtf8(coinapultCurrency.name);
      Sha256Hash accountId = HashUtils.sha256(byteWriter.toBytes());
      uuid = getGuidFromByteArray(accountId.getBytes());

      archived = metadataStorage.getArchived(uuid);
   }

   public static UUID getGuidFromByteArray(byte[] bytes) {
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      long high = bb.getLong();
      long low = bb.getLong();
      return new UUID(high, low);
   }

   public void queryActive() throws CoinapultClient.CoinapultBackendException {
      if (manager.userAccountExists()) {
         refreshReceivingAddress();
      }
   }


   @Override
   public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.OutputTooSmallException {
      Optional<ExactCurrencyValue> sendValue = com.mycelium.wapi.wallet.currency.CurrencyValue.checkCurrencyAmount(enteredAmount, coinapultCurrency.name);

      if (balanceFiat == null || sendValue.isPresent() && sendValue.get().getValue().compareTo(balanceFiat.confirmed.getValue()) > 0) {
         //not enough funds
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      }
      if (!sendValue.isPresent() && receiver.amount > getSatoshis(balanceFiat.confirmed)) {
         //non-fiat value, but approximately not enough funds
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      }
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

   // if it is a fiat value convert it, otherwise take the exact value
   private long getSatoshis(ExactCurrencyValue confirmed) {
      return getSatoshis(confirmed.getValue(), confirmed.getCurrency());
   }

   private ExactCurrencyValue getCurrencyValue(com.coinapult.api.httpclient.Transaction.Half half) {
      return ExactCurrencyValue.from(half.expected, half.currency);
   }

   private void queryHistory() throws CoinapultClient.CoinapultBackendException {
      // get the complete history from the manager, and filter out only the relevant tx
      accountHistory = filterHistory(manager.getHistory());

      // now we know the accountHistory, we know if funds are incoming
      buildBalance();
   }

   private List<Transaction.Json> filterHistory(List<Transaction.Json> completeHistory) {
      if (completeHistory == null) {
         return null;
      }
      return Lists.newArrayList(
            Iterables.filter(completeHistory, new Predicate<Transaction.Json>() {
                     @Override
                     public boolean apply(@Nullable Transaction.Json input) {
                        // only add items with the correct currency for the current selected account
                        if (input != null) {
                           if (input.state.equals("canceled")) {
                              // todo: show somehow that a tx got canceled
                              // dont show canceled transactions
                              return false;
                           }

                           boolean isSending = isSending(input);
                           // depending on the sending/incoming direction, check either in or out half
                           if (isSending) {
                              return input.in.currency.equals(coinapultCurrency.name);
                           } else {
                              return input.out.currency.equals(coinapultCurrency.name);
                           }
                        }
                        return false;
                     }
                  }
            )
      );

   }

   public PreparedCoinapult prepareCoinapultTx(Address receivingAddress, ExactCurrencyValue amountEntered) throws StandardTransactionBuilder.InsufficientFundsException {
      if (balanceFiat == null || balanceFiat.confirmed.getValue().compareTo(amountEntered.getValue()) < 0) {
         throw new StandardTransactionBuilder.InsufficientFundsException(getSatoshis(amountEntered), 0);
      }
      return new PreparedCoinapult(receivingAddress, amountEntered);
   }


   public PreparedCoinapult prepareCoinapultTx(WalletAccount.Receiver receiver) throws StandardTransactionBuilder.InsufficientFundsException {
      if (balanceFiat == null || getSatoshis(balanceFiat.confirmed) < receiver.amount) {
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      } else {
         return new PreparedCoinapult(receiver);
      }
   }

   public boolean broadcast(PreparedCoinapult preparedCoinapult) {
      try {
         final com.coinapult.api.httpclient.Transaction.Json send;
         if (preparedCoinapult.amount != null) {
            send = manager.getClient().send(preparedCoinapult.amount.getValue(), coinapultCurrency.name, preparedCoinapult.address.toString(), BigDecimal.ZERO, null, null, null);
         } else {
            BigDecimal fullBtc = new BigDecimal(preparedCoinapult.satoshis).divide(SATOSHIS_PER_BTC, MathContext.DECIMAL32);
            send = manager.getClient().send(BigDecimal.ZERO, coinapultCurrency.name, preparedCoinapult.address.toString(), fullBtc, null, null, null);
         }
         Object error = send.get("error");
         if (error != null) {
            return false;
         }
         Object transaction_id = send.get("transaction_id");
         boolean hasId = transaction_id != null;
         if (hasId) {
            extraHistory.add(send);
         }
         return hasId;
      } catch (IOException e) {
         return false;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }


   private AccountInfo.Balance getBalanceFiat() throws CoinapultClient.CoinapultBackendException {
      Map<String, AccountInfo.Balance> balances;
      balances = manager.getBalances();

      if (balances.containsKey(coinapultCurrency.name)) {
         return balances.get(coinapultCurrency.name);
      } else {
         AccountInfo.Balance empty = new AccountInfo.Balance();
         empty.currency = coinapultCurrency.name;
         empty.amount = BigDecimal.ZERO;
         return empty;
      }
   }

   @Override
   public Type getType() {
      return Type.COINAPULT;
   }

   private void buildBalance() throws CoinapultClient.CoinapultBackendException {
      AccountInfo.Balance balanceFiat = getBalanceFiat();
      final long oneMinuteAgo = new Date().getTime() - 1000 * 60;

      BigDecimal receivingFiat = BigDecimal.ZERO;
      BigDecimal receivingFiatNotIncludedInBalance = BigDecimal.ZERO;
      BigDecimal sendingFiatNotIncludedInBalance = BigDecimal.ZERO;
      for (com.coinapult.api.httpclient.Transaction.Json json : getHistoryWithExtras()) {
         boolean sending = isSending(json);
         if (json.state.equals("processing") || json.completeTime * 1000 > oneMinuteAgo) {
            if (sending) {
               sendingFiatNotIncludedInBalance = sendingFiatNotIncludedInBalance.add(json.in.expected);
            } else {
               receivingFiat = receivingFiat.add(json.out.amount);
            }
         } else if (json.state.equals("confirming")) {
            if (sending) {
               sendingFiatNotIncludedInBalance = sendingFiatNotIncludedInBalance.add(json.in.expected);
            } else {
               receivingFiatNotIncludedInBalance = receivingFiatNotIncludedInBalance.add(json.out.expected);
            }
         }
      }
      BigDecimal withoutUnconfirmed = balanceFiat.amount.subtract(receivingFiat);
      BigDecimal totalReceiving = receivingFiat.add(receivingFiatNotIncludedInBalance);

      if (withoutUnconfirmed.compareTo(BigDecimal.ZERO) < 0) {
         MbwManager.getInstance(null).reportIgnoredException(
               new RuntimeException(String
                     .format(Locale.getDefault(), "Calculated withoutUnconfirmed-amount for coinapult account is negative withoutUnconfirmed: %f, sending: %f, recv: %f", withoutUnconfirmed, sendingFiatNotIncludedInBalance, totalReceiving))
         );
         withoutUnconfirmed = BigDecimal.ZERO;
      }

      if (sendingFiatNotIncludedInBalance.compareTo(BigDecimal.ZERO) < 0) {
         MbwManager.getInstance(null).reportIgnoredException(
               new RuntimeException(String
                     .format(Locale.getDefault(), "Calculated sendingUsdNotIncludedInBalance-amount for coinapult account is negative withoutUnconfirmed: %f, sending: %f, recv: %f", withoutUnconfirmed, sendingFiatNotIncludedInBalance, totalReceiving))
         );
         sendingFiatNotIncludedInBalance = BigDecimal.ZERO;
      }

      if (totalReceiving.compareTo(BigDecimal.ZERO) < 0) {
         MbwManager.getInstance(null).reportIgnoredException(
               new RuntimeException(String
                     .format(Locale.getDefault(), "Calculated totalReceiving-amount for coinapult account is negative withoutUnconfirmed: %f, sending: %f, recv: %f", withoutUnconfirmed, sendingFiatNotIncludedInBalance, totalReceiving))
         );
         sendingFiatNotIncludedInBalance = BigDecimal.ZERO;
      }


      this.balanceFiat = new CurrencyBasedBalance(
            ExactCurrencyValue.from(withoutUnconfirmed, coinapultCurrency.name),
            ExactCurrencyValue.from(sendingFiatNotIncludedInBalance, coinapultCurrency.name),
            ExactCurrencyValue.from(totalReceiving, coinapultCurrency.name)
      );
   }


   private boolean isSending(com.coinapult.api.httpclient.Transaction.Json input) {
      boolean isPayment = input.type.equals("payment");
      if (isPayment) {
         return true;
      }
      boolean isInvoice = input.type.equals("invoice");
      if (isInvoice) {
         return false;
      }
      // other unexpected tx type - but ignore it
      return false;
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
      return NetworkParameters.productionNetwork;
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
      return 0;
   }

   @Override
   public Optional<Address> getReceivingAddress() {
      if (!currentAddress.isPresent()) {
         currentAddress = metadataStorage.getCoinapultAddress(coinapultCurrency.name);
      }
      return currentAddress;
   }

   @Override
   public boolean canSpend() {
      return true;
   }

   @Override
   public Balance getBalance() {
      if (balanceFiat == null) {
         return EMPTY_BALANCE;
      }
      ExactCurrencyValue confirmed = balanceFiat.confirmed;
      return new Balance(getSatoshis(confirmed), getSatoshis(balanceFiat.receiving), getSatoshis(balanceFiat.sending), 0, 0, 0, false, true);
   }

   @Override
   public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
      if (accountHistory != null) {
         List<TransactionSummary> list = getTransactionSummaries();
         return limitedList(offset, limit, list);

      } else {
         return Lists.newArrayList();
      }
   }

   @NonNull
   private List<TransactionSummary> getTransactionSummaries() {
      return Lists.transform(getHistoryWithExtras(), new Function<Transaction.Json, TransactionSummary>() {
         @Nullable
         @Override
         public TransactionSummary apply(@Nullable Transaction.Json input) {
            input = Preconditions.checkNotNull(input);
            Optional<Address> address = Optional.fromNullable(input.address).transform(Address.FROM_STRING);
            boolean isIncoming = !isSending(input);
            // use the relevant amount from the transaction.
            // if it is an incoming transaction, the "out"-side of the tx is in the native currency
            // and v.v. for outgoing tx
            Transaction.Half half = isIncoming ? input.out : input.in;

            return new CoinapultTransactionSummary(address, getCurrencyValue(half), isIncoming, input);
         }
      });
   }

   @Override
   public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
      if (accountHistory != null) {
         List<TransactionSummary> list = getTransactionSummaries();
         final ArrayList<TransactionSummary> result = new ArrayList<TransactionSummary>();
         for (TransactionSummary item : list) {
            if (item.time < receivingSince) {
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
      return null;
   }

   @Override
   public TransactionDetails getTransactionDetails(Sha256Hash txid) {
      return null;
   }

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
      // try to recreate the coinapult account, this fails if it already exists
      // otherwise it will fix a locked state where the wallet thinks it has an account
      // but it does not
      // TODO: remove Coinapult
      // new AddCoinapultAsyncTask().execute();
   }

   private void refreshReceivingAddress() {
      try {
         String address;
         Optional<Address> lastCoinapultAddress = metadataStorage.getCoinapultAddress(coinapultCurrency.name);
         if (!lastCoinapultAddress.isPresent()) {
            //requesting fresh address
            address = manager.getClient().getBitcoinAddress().address;
         } else {
            // check if address was already used (via new coinapult api),
            // if so: request a fresh one from coinapult
            HashMap<String, String> criteria = new HashMap<String, String>(1);
            criteria.put("to", lastCoinapultAddress.get().toString());
            com.coinapult.api.httpclient.Transaction.Json search = manager.getClient().search(criteria);
            boolean alreadyUsed = search.containsKey("transaction_id");
            if (alreadyUsed) {
               // get a new one
               address = manager.getClient().getBitcoinAddress().address;
            } else {
               address = lastCoinapultAddress.get().toString();
            }
         }
         //setting preference to the selected fiat currency
         manager.getClient().config(address, coinapultCurrency.name);
         currentAddress = Optional.of(Address.fromString(address));
         metadataStorage.storeCoinapultAddress(currentAddress.get(), coinapultCurrency.name);
      } catch (Exception e) {
         Log.e("CoinapultManager", "Failed to refreshReceivingAddress", e);
         handler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new SyncFailed());
            }
         });
      }
   }


   @Override
   public UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      throw new IllegalStateException("not supported, use prepareCoinaputTX instead");
   }

   @Override
   public UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      return null;
   }

   @Override
   public com.mrd.bitlib.model.Transaction signTransaction(UnsignedTransaction unsigned, KeyCipher
         cipher) throws KeyCipher.InvalidKeyCipher {
      return null;
   }

   @Override
   public boolean broadcastOutgoingTransactions() {
      return true;
   }

   @Override
   public boolean isMine(Address address) {
      // there might be more, but currently we only know about this one...
      Optional<Address> receivingAddress = getReceivingAddress();
      return receivingAddress.isPresent() && receivingAddress.get().equals(address);
   }

   @Override
   protected boolean doSynchronization(SyncMode mode) {
      return synchronizeIntern(mode, true);
   }

   public boolean synchronizeIntern(SyncMode mode, boolean scanForAccounts) {
      try {
         queryActive();
         if (!mode.ignoreTransactionHistory) {
            queryHistory();
         }
      } catch (CoinapultClient.CoinapultBackendException e) {
         Log.e("CoinapultManager", "Failed to query history", e);
         handler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new SyncFailed());
            }
         });
         return false;
      }
      handler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.post(new BalanceChanged(uuid));
         }
      });

      if (scanForAccounts) {
         manager.scanForAccounts();
      }
      return true;
   }


   private List<com.coinapult.api.httpclient.Transaction.Json> getHistoryWithExtras() {
      if (accountHistory == null) {
         return Lists.newArrayList();
      }
      Function<com.coinapult.api.httpclient.Transaction.Json, String> txMapping = new Function<com.coinapult.api.httpclient.Transaction.Json, String>() {
         @Nullable
         @Override
         public String apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
            return input.tid;
         }
      };
      ImmutableMap<String, Transaction.Json> localHistoryMap = Maps.uniqueIndex(extraHistory, txMapping);
      final HashMap<String, Transaction.Json> historyMap = new HashMap<String, Transaction.Json>();
      for (Transaction.Json historyEntry : accountHistory) {
         // sometimes the entry contains the same tx twice - timing problem in combination with paging-request
         if (!historyMap.containsKey(historyEntry.tid)) {
            historyMap.put(historyEntry.tid, historyEntry);
         }
      }
      HashMap<String, Transaction.Json> merged = Maps.newHashMap();
      merged.putAll(localHistoryMap);
      merged.putAll(historyMap); //accountHistory overwrites local results
      Collection<Transaction.Json> unfiltered = merged.values();
      Iterable<com.coinapult.api.httpclient.Transaction.Json> withoutConversion = Iterables.filter(unfiltered, TX_NOT_CONVERSION);
      ImmutableList<Transaction.Json> ret = Ordering.natural().onResultOf(new Function<com.coinapult.api.httpclient.Transaction.Json, Comparable>() {
         @Nullable
         @Override
         public Comparable apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
            Long completeTime = input.completeTime;
            if (completeTime.equals(0L)) {
               return input.timestamp;
            }
            return completeTime;
         }
      }).reverse().immutableSortedCopy(withoutConversion);
      return ret;
   }

   @Override
   public BroadcastResult broadcastTransaction(com.mrd.bitlib.model.Transaction transaction) {
      return null;
   }

   @Override
   public TransactionEx getTransaction(Sha256Hash txid) {
      return null;
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
   public void removeAllQueuedTransactions() {

   }

   @Override
   public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse) {
      return getCurrencyBasedBalance().confirmed;
   }

   @Override
   public ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse, Address destinationAddress) {
      return calculateMaxSpendableAmount(minerFeeToUse);
   }

   @Override
   public boolean isValidEncryptionKey(KeyCipher cipher) {
      return false;
   }

   @Override
   public boolean isDerivedFromInternalMasterseed() {
      return true;
   }

   @Override
   public boolean isOwnInternalAddress(Address address) {
      return false;
   }

   @Override
   public UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce) {
      return null;
   }

   @Override
   public boolean isOwnExternalAddress(Address address) {
      return isMine(address);
   }

   @Override
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
      return null;
   }

   public CurrencyBasedBalance getCurrencyBasedBalance() {
      if (balanceFiat != null) {
         return balanceFiat;
      } //result precomputed from accountHistory query

      // if we dont have a balance, return 0 in the accounts native currency
      ExactCurrencyValue zero = ExactCurrencyValue.from(BigDecimal.ZERO, coinapultCurrency.name);
      return new CurrencyBasedBalance(zero, zero, zero, true);
   }

   @Override
   public boolean onlySyncWhenActive() {
      return true;
   }

   public static class PreparedCoinapult implements Serializable {
      final Address address;
      Long satoshis;
      ExactCurrencyValue amount;

      public PreparedCoinapult(Address address, ExactCurrencyValue value) {
         this.address = address;
         this.amount = value;
      }

      public PreparedCoinapult(WalletAccount.Receiver receiver) {
         address = receiver.address;
         satoshis = receiver.amount;
      }

      @Override
      public String toString() {
         String sat = satoshis != null ? ", satoshis=" + satoshis : "";
         String fiat = amount != null ? ", amount=" + amount : "";
         return "address=" + address + sat + fiat;
      }
   }

   public String getDefaultLabel() {
      return "Coinapult " + coinapultCurrency.name;
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
         return name.equals(currency.name) && minimumConversationValue.equals(currency.minimumConversationValue);
      }

      @Override
      public int hashCode() {
         int result = name.hashCode();
         result = 31 * result + minimumConversationValue.hashCode();
         return result;
      }

      public String getMinimumConversationString() {
         return new DecimalFormat("#0.00##").format(minimumConversationValue) + " " + name;
      }
   }

   public Currency getCoinapultCurrency() {
      return coinapultCurrency;
   }

   @Override
   public String getAccountDefaultCurrency() {
      return getCoinapultCurrency().name;
   }

   @Override
   public int getSyncTotalRetrievedTransactions() {
      return 0;
   }
}

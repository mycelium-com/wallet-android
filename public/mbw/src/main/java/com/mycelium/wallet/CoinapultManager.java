package com.mycelium.wallet;

import android.os.Handler;
import android.util.Log;
import com.coinapult.api.httpclient.*;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.*;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactFiatValue;
import com.squareup.otto.Bus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.coinapult.api.httpclient.CoinapultClient.*;

public class CoinapultManager implements WalletAccount {

   public static final BigDecimal SATOSHIS_PER_BTC = BigDecimal.valueOf(100000000);
   public static final Balance EMPTY_BALANCE = new Balance(0, 0, 0, 0, 0, 0, true, true);
   public static final BigDecimal MINIMUM_USD_AMOUNT = new BigDecimal("1.0");
   public static final Predicate<com.coinapult.api.httpclient.Transaction.Json> TX_NOT_CONVERSION = Predicates.not(new Predicate<com.coinapult.api.httpclient.Transaction.Json>() {
      @Override
      public boolean apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
         return input.type.equals("conversion");
      }
   });
   private final MbwEnvironment env;
   private final Bus eventBus;
   private final UUID uuid;
   private CoinapultClient coinapultClient;
   private Optional<Address> currentAddress = Optional.absent();
   private CurrencyBasedBalance balanceUSD;
   private final InMemoryPrivateKey accountKey;
   private final Handler handler;
   private final MetadataStorage metadataStorage;
   private final ExchangeRateManager exchangeRateManager;
   private final WapiLogger logger;
   private boolean archived;
   private List<com.coinapult.api.httpclient.Transaction.Json> history;
   private List<com.coinapult.api.httpclient.Transaction.Json> extraHistory = Lists.newArrayList();

   public CoinapultManager(MbwEnvironment env, BitidKeyDerivation bitidKeyDerivation, final Bus eventBus, Handler handler, MetadataStorage metadataStorage, ExchangeRateManager exchangeRateManager, WapiLogger logger) {
      this.env = env;
      this.eventBus = eventBus;
      this.handler = handler;
      this.metadataStorage = metadataStorage;
      this.exchangeRateManager = exchangeRateManager;
      this.logger = logger;
      accountKey = bitidKeyDerivation.deriveKey(0, "coinapult.com");
      coinapultClient = createClient();
      uuid = UUID.fromString(getGuidFromByteArray(accountKey.getPrivateKeyBytes()));
      archived = metadataStorage.getArchived(uuid);
      metadataStorage.storeAccountLabel(uuid, "Coinapult USD");
      handler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.register(CoinapultManager.this);
         }
      });

   }

   public InMemoryPrivateKey getAccountKey() {
      return accountKey;
   }

   public static String getGuidFromByteArray(byte[] bytes) {
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      long high = bb.getLong();
      long low = bb.getLong();
      UUID uuid = new UUID(high, low);
      return uuid.toString();
   }

   public void queryActive() throws CoinapultBackendException {
      if (coinapultClient.accountExists()) {
         initBalance();
      }
   }

   public void addUSD(Optional<String> mail) throws CoinapultBackendException {
      try {
         if (!coinapultClient.accountExists()) {
            Map<String, String> options = new HashMap<String, String>();
            if (mail.isPresent()) {
               options.put("email", mail.get());
            }
            coinapultClient.createAccount(options);
            coinapultClient.activateAccount(true);
         }
         initBalance();
      } catch (Exception e) {
         Log.e("CoinapultManager", "Failed to addUsd account", e);
         throw new CoinapultClient.CoinapultBackendException(e);
      }
   }

   private void initBalance() {
      try {
         String address;
         Optional<Address> lastCoinapultAddress = metadataStorage.getCoinapultAddress();
         if (!lastCoinapultAddress.isPresent()){
            //requesting fresh address
            address = coinapultClient.getBitcoinAddress().address;
         } else {
            // check if address was already used (via new coinapult api),
            // if so: request a fresh one from coinapult
            HashMap<String, String> criteria = new HashMap<String, String>(1);
            criteria.put("to", lastCoinapultAddress.get().toString());
            com.coinapult.api.httpclient.Transaction.Json search = coinapultClient.search(criteria);
            boolean alreadyUsed = search.containsKey("transaction_id");
            if (alreadyUsed){
               // get a new one
               address = coinapultClient.getBitcoinAddress().address;
            } else {
               address = lastCoinapultAddress.get().toString();
            }
         }
         //setting preference to USD
         coinapultClient.config(address, "USD");
         currentAddress = Optional.of(Address.fromString(address));
         metadataStorage.storeCoinapultAddress(currentAddress.get());
      } catch (Exception e) {
         Log.e("CoinapultManager", "Failed to initBalance", e);
         handler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new SyncFailed());
            }
         });
      }
   }

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

   private AccountInfo.Balance getBalanceUSD() throws CoinapultBackendException {
      List<AccountInfo.Balance> balances;
      try {
         balances = coinapultClient.accountInfo().balances;
         //expect only a usd balance
         for (AccountInfo.Balance bal : balances) {
            if (bal.currency.equals("USD")) {
               return bal;
            }
         }
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new CoinapultBackendException(e);
      }

      AccountInfo.Balance empty = new AccountInfo.Balance();
      empty.currency = "USD";
      empty.amount = BigDecimal.ZERO;
      return empty;
   }


   private CoinapultClient createClient() {
      CoinapultConfig cc;
      NetworkParameters network = env.getNetwork();
      if (network.equals(NetworkParameters.testNetwork)) {
         cc = new CoinapultPlaygroundConfig();
      } else if (network.equals(NetworkParameters.productionNetwork)) {
         cc = new CoinapultProdConfig();
      } else {
         throw new IllegalStateException("unknown network: " + network);
      }

      return new CoinapultClient(AndroidKeyConverter.convertKeyFormat(accountKey), new ECC_SC(), cc, logger);
   }

   @Override
   public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws StandardTransactionBuilder.InsufficientFundsException, StandardTransactionBuilder.OutputTooSmallException {
      Optional<ExactFiatValue> usd = CurrencyValue.checkUsdAmount(enteredAmount);
      if (usd.isPresent() && usd.get().getValue().compareTo(MINIMUM_USD_AMOUNT) < 0) {
         //output too small
         //JD, July 16th, 2015: commented this out, because if the number pad does not accept small amounts without explanation,
         //users will get confused -> opt to show an explanation in sendmain
         //throw new StandardTransactionBuilder.OutputTooSmallException(receiver.amount);
      }
      if (balanceUSD == null || usd.isPresent() && usd.get().getValue().compareTo(balanceUSD.confirmed.getValue()) > 0) {
         //not enough funds
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      }
      if (!usd.isPresent() && receiver.amount > getSatoshis(balanceUSD.confirmed)) {
         //non-usd value, but approximately not enough funds
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      }
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
         currentAddress = metadataStorage.getCoinapultAddress();
      }
      return currentAddress;
   }

   @Override
   public boolean canSpend() {
      return true;
   }

   @Override
   public Balance getBalance() {
      if (balanceUSD == null) {
         return EMPTY_BALANCE;
      }
      ExactCurrencyValue confirmed = balanceUSD.confirmed;
      return new Balance(getSatoshis(confirmed), getSatoshis(balanceUSD.receiving), getSatoshis(balanceUSD.sending), 0, 0, 0, false, true);
   }

   private long getSatoshis(ExactCurrencyValue confirmed) {
      return getSatoshis(confirmed.getValue(), confirmed.getCurrency()); //currency should be USD
   }

   @Override
   public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
      if (history != null) {
         List<TransactionSummary> list = Lists.transform(getHistoryWithExtras(), new Function<com.coinapult.api.httpclient.Transaction.Json, TransactionSummary>() {
            @Nullable
            @Override
            public TransactionSummary apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
               input = Preconditions.checkNotNull(input);
               Optional<Address> address = Optional.fromNullable(input.address).transform(Address.FROM_STRING);
               return new CoinapultTransactionSummary(address, satoshiDifference(input), input);
            }
         });
         return limitedList(offset, limit, list);

      } else {
         return Lists.newArrayList();
      }
   }

   private List<com.coinapult.api.httpclient.Transaction.Json> getHistoryWithExtras() {
      Function<com.coinapult.api.httpclient.Transaction.Json, String> tx = new Function<com.coinapult.api.httpclient.Transaction.Json, String>() {
         @Nullable
         @Override
         public String apply(@Nullable com.coinapult.api.httpclient.Transaction.Json input) {
            return input.tid;
         }
      };
      ImmutableMap<String, com.coinapult.api.httpclient.Transaction.Json> id2Tx1 = Maps.uniqueIndex(extraHistory, tx);
      ImmutableMap<String, com.coinapult.api.httpclient.Transaction.Json> id2Tx2 = Maps.uniqueIndex(history, tx);
      HashMap<String, com.coinapult.api.httpclient.Transaction.Json> merged = Maps.newHashMap();
      merged.putAll(id2Tx1);
      merged.putAll(id2Tx2); //history overwrites local results
      Collection<com.coinapult.api.httpclient.Transaction.Json> unfiltered = merged.values();
      Iterable<com.coinapult.api.httpclient.Transaction.Json> withoutConversion = Iterables.filter(unfiltered, TX_NOT_CONVERSION);
      ImmutableList<com.coinapult.api.httpclient.Transaction.Json> ret = Ordering.natural().onResultOf(new Function<com.coinapult.api.httpclient.Transaction.Json, Comparable>() {
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

   private long satoshiDifference(com.coinapult.api.httpclient.Transaction.Json input) {
      boolean isSending = isSending(input);
      int sign = isSending ? -1 : 1;
      return sign * CoinapultManager.this.getSatoshis(input.out.amount, input.out.currency);
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
      throw new IllegalStateException("unexpected tx type: " + input.type);

   }

   private <T> List<T> limitedList(int offset, int limit, List<T> list) {
      if (offset >= list.size()) {
         return Collections.emptyList();
      }
      int endIndex = Math.min(offset + limit, list.size());
      return Collections.unmodifiableList(list.subList(offset, endIndex));
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

   }

   @Override
   public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      throw new IllegalStateException("not supported, use prepareCoinaputTX instead");
   }

   @Override
   public StandardTransactionBuilder.UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws StandardTransactionBuilder.OutputTooSmallException, StandardTransactionBuilder.InsufficientFundsException {
      return null;
   }

   @Override
   public Transaction signTransaction(StandardTransactionBuilder.UnsignedTransaction unsigned, KeyCipher cipher) throws KeyCipher.InvalidKeyCipher {
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
      return  receivingAddress.isPresent() && receivingAddress.get().equals(address);
   }

   @Override
   public boolean synchronize(boolean synchronizeTransactionHistory) {

      try {
         queryActive();
         if (synchronizeTransactionHistory) {
            queryHistory();
         }
      } catch (CoinapultBackendException e) {
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
      return true;
   }

   private void queryHistory() throws CoinapultBackendException {
      SearchMany.Json batch;
      //get first page to get pageCount
      batch = coinapultClient.history(1);
      history = Lists.newArrayList();
      addToHistory(batch);
      //get extra pages
      for (int i = 2; batch.page < batch.pageCount; i++) {
         batch = coinapultClient.history(i);
         addToHistory(batch);
      }
      //now we know the history, we know if funds are incoming

      buildBalance();
   }

   private void buildBalance() throws CoinapultBackendException {
      AccountInfo.Balance balanceUSD1 = getBalanceUSD();
      final long oneMinuteAgo = new Date().getTime() - 1000 * 60;

      BigDecimal receivingUsd = BigDecimal.ZERO;
      BigDecimal sendingUsd = BigDecimal.ZERO;
      BigDecimal receivingUsdNotIncludedInBalance = BigDecimal.ZERO;
      BigDecimal sendingUsdNotIncludedInBalance = BigDecimal.ZERO;
      for (com.coinapult.api.httpclient.Transaction.Json json : getHistoryWithExtras()) {
         boolean sending = isSending(json);
         if (json.state.equals("processing") || json.completeTime * 1000 > oneMinuteAgo) {
            if (sending) {
               sendingUsdNotIncludedInBalance = sendingUsdNotIncludedInBalance.add(json.in.amount);
            } else {
               receivingUsd = receivingUsd.add(json.out.amount);
            }
         } else if (json.state.equals("confirming")) {
            if (sending) {
               sendingUsdNotIncludedInBalance = sendingUsdNotIncludedInBalance.add(json.in.amount);
            } else {
               receivingUsdNotIncludedInBalance = receivingUsdNotIncludedInBalance.add(json.out.amount);
            }
         }
      }
      BigDecimal withoutUnconfirmed = balanceUSD1.amount.subtract(receivingUsd).add(sendingUsd);
      if (withoutUnconfirmed.compareTo(BigDecimal.ZERO) < 0) {
         MbwManager.getInstance(null).reportIgnoredException(
               new RuntimeException(String
                     .format("Calculated withoutUnconfirmed-amount for coinapult account is negative {}, sending: {}, recv: {}", withoutUnconfirmed, sendingUsd, receivingUsd))
         );
         withoutUnconfirmed=BigDecimal.ZERO;
      }
      balanceUSD = new CurrencyBasedBalance(
            ExactCurrencyValue.from(withoutUnconfirmed, "USD"),
            ExactCurrencyValue.from(sendingUsd.add(sendingUsdNotIncludedInBalance), "USD"),
            ExactCurrencyValue.from(receivingUsd.add(receivingUsdNotIncludedInBalance), "USD")
      );
   }

   private void addToHistory(SearchMany.Json batch) {
      if (batch == null) {
         return;
      }
      if (batch.result == null) {
         return;
      }
      history.addAll(batch.result);
   }

   @Override
   public BroadcastResult broadcastTransaction(Transaction transaction) {
      return null;
   }

   @Override
   public TransactionEx getTransaction(Sha256Hash txid) {
      return null;
   }

   @Override
   public void queueTransaction(Transaction transaction) {

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
      return true;
   }

   @Override
   public boolean isOwnInternalAddress(Address address) {
      return false;
   }

   @Override
   public boolean isOwnExternalAddress(Address address) {
      return isMine(address);
   }

   @Override
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
      return null;
   }

   public PreparedCoinapult prepareCoinapultTx(Receiver receiver) throws StandardTransactionBuilder.InsufficientFundsException {
      if (getSatoshis(balanceUSD.confirmed) < receiver.amount) {
         throw new StandardTransactionBuilder.InsufficientFundsException(receiver.amount, 0);
      } else {
         return new PreparedCoinapult(receiver);
      }
   }

   public boolean broadcast(PreparedCoinapult preparedCoinapult) {
      try {
         final com.coinapult.api.httpclient.Transaction.Json send;
         if (preparedCoinapult.amount != null) {
            send = coinapultClient.send(preparedCoinapult.amount.getValue(), "USD", preparedCoinapult.address.toString(), BigDecimal.ZERO, null, null, null);
         } else {
            BigDecimal fullBtc = new BigDecimal(preparedCoinapult.satoshis).divide(SATOSHIS_PER_BTC, MathContext.DECIMAL32);
            send = coinapultClient.send(BigDecimal.ZERO, "USD", preparedCoinapult.address.toString(), fullBtc, null, null, null);
         }
         extraHistory.add(send);
         Object error = send.get("error");
         if (error != null) {
            return false;
         }
         Object transaction_id = send.get("transaction_id");
         if (transaction_id != null) {
            return true;
         }
         return false;
      } catch (IOException e) {
         return false;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   public CurrencyBasedBalance getCurrencyBasedBalance() {
      if (balanceUSD != null) {
         return balanceUSD;
      } //result precomputed from history query

      // if we dont have a balance, return 0
      ExactFiatValue zero = new ExactFiatValue("USD", BigDecimal.ZERO);
      return new CurrencyBasedBalance(zero, zero, zero, true);
   }

   public PreparedCoinapult prepareCoinapultTx(Address receivingAddress, ExactFiatValue amountEntered) throws StandardTransactionBuilder.InsufficientFundsException {
      if (balanceUSD.confirmed.getValue().compareTo(amountEntered.getValue()) < 0) {
         throw new StandardTransactionBuilder.InsufficientFundsException(getSatoshis(amountEntered), 0);
      }
      return new PreparedCoinapult(receivingAddress, amountEntered);
   }

   public boolean setMail(Optional<String> mail) {
      if (!mail.isPresent()) {
         return false;
      }
      try {
         EmailAddress.Json result = coinapultClient.setMail(mail.get());
         return result.email != null && result.email.equals(mail.get());
      } catch (IOException e) {
         return false;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   public boolean verifyMail(String link, String email) {
      try {
         EmailAddress.Json result = coinapultClient.verifyMail(link, email);
         if (!result.verified){
            logger.logError("Coinapult email error: " + result.error);
         }
         return result.verified;
      } catch (IOException e) {
         return false;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   public static class PreparedCoinapult implements Serializable {
      final Address address;
      Long satoshis;
      ExactFiatValue amount;

      public PreparedCoinapult(Address address, ExactFiatValue value) {
         this.address = address;
         this.amount = value;
      }

      public PreparedCoinapult(Receiver receiver) {
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

   @Override
   public boolean onlySyncWhenActive() {
      return true;
   }

}
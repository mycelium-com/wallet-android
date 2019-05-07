package com.mycelium.wallet.coinapult;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.coinapult.api.httpclient.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.BitIdKeyDerivation;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwEnvironment;
import com.mycelium.wallet.event.ExtraAccountsChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AccountProvider;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Bus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CoinapultManager implements AccountProvider {
   private static final int CACHE_DURATION = 10;
   private final MbwEnvironment env;
   private final Bus eventBus;
   private final Handler handler;
   private final ExchangeRateManager exchangeRateManager;
   private final CoinapultClient coinapultClient;
   private final InMemoryPrivateKey accountKey;
   private final MetadataStorage metadataStorage;
   private final WapiLogger logger;
   private final HashMap<UUID, CoinapultAccount> coinapultAccounts;

   public CoinapultManager(MbwEnvironment env, BitIdKeyDerivation bitIdKeyDerivation, final Bus eventBus, Handler handler,
                           MetadataStorage metadataStorage, ExchangeRateManager exchangeRateManager, WapiLogger logger) {
      this.env = env;
      this.eventBus = eventBus;
      this.handler = handler;
      this.metadataStorage = metadataStorage;
      this.exchangeRateManager = exchangeRateManager;
      this.logger = logger;
      accountKey = bitIdKeyDerivation.deriveKey(0, "coinapult.com");
      coinapultClient = createClient();
      handler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.register(CoinapultManager.this);
         }
      });
      coinapultAccounts = new HashMap<>();
      flushCacheIfNeeded();
      loadAccounts();
   }

   private void flushCacheIfNeeded() {
      // coinpault migrates their backend to a new wallet provider - flush cache to prevent payments to old
      // addresses from previous backend
      if (metadataStorage.getCoinapultLastFlush() < 1) {
         metadataStorage.deleteCoinapultAddress("USD");
         metadataStorage.deleteCoinapultAddress("EUR");
         metadataStorage.deleteCoinapultAddress("GBP");
         metadataStorage.deleteCoinapultAddress("BTC");
         metadataStorage.storeCoinapultLastFlush(1);
      }
   }

   private void saveEnabledCurrencies() {
      String all = Joiner.on(",").join(Iterables.transform(coinapultAccounts.values(), new Function<CoinapultAccount, String>() {
         @Nullable
         @Override
         public String apply(@Nullable CoinapultAccount input) {
            Preconditions.checkNotNull(input);
            return input.getCoinapultCurrency().name;
         }
      }));
      metadataStorage.storeCoinapultCurrencies(all);
   }

   private void loadAccounts() {
      Iterable<String> currencies = Splitter.on(",").split(metadataStorage.getCoinapultCurrencies());
      int countAccounts = 0;
      for (String currency : currencies) {
         if (!Strings.isNullOrEmpty(currency)) {
            CoinapultAccount.Currency currencyDefinition = CoinapultAccount.Currency.all.get(currency);
            createAccount(currencyDefinition);
            countAccounts++;
         }
      }

      if (countAccounts == 0) {
         // if there were no accounts active, try to fetch the balance anyhow and activate
         // all accounts with a balance > 0
         // but do it in background, as this function gets called via the constructor, which
         // gets called in the MbwManager constructor
         new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
               scanForAccounts();
               return null;
            }
         }.execute();
      }
   }

   private CoinapultAccount createAccount(CoinapultAccount.Currency currency) {
      CoinapultAccount account = new CoinapultAccount(
            CoinapultManager.this, metadataStorage, accountKey,
            exchangeRateManager, handler, eventBus, logger, currency
      );
      coinapultAccounts.put(account.getId(), account);
      return account;
   }

   private void enableCurrency(String currency) {
      if (CoinapultAccount.Currency.all.containsKey(currency)) {
         CoinapultAccount.Currency currencyAccount = CoinapultAccount.Currency.all.get(currency);
         enableCurrency(currencyAccount);
      }
   }

   public UUID enableCurrency(CoinapultAccount.Currency currency) {
      // check if we already have it enabled
      CoinapultAccount account = getAccountForCurrency(currency);
      if (account != null) {
         return account.getId();
      }

      // otherwise create a new account for it, and persist the setting
      CoinapultAccount newAccount = createAccount(currency);

      // check if we already have a label for this account, otherwise set the default one
      String label = metadataStorage.getLabelByAccount(newAccount.getId());
      if (Strings.isNullOrEmpty(label)) {
         metadataStorage.storeAccountLabel(newAccount.getId(), newAccount.getDefaultLabel());
      }

      // get its initial balance
      newAccount.synchronizeIntern(SyncMode.FULL_SYNC_ALL_ACCOUNTS, false);

      // save each account if it was used once
      saveEnabledCurrencies();

      // broadcast event, so that the UI shows the newly added account
      handler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.post(new ExtraAccountsChanged());
         }
      });

      // and save it
      saveEnabledCurrencies();

      return newAccount.getId();
   }

   @android.support.annotation.Nullable
   private CoinapultAccount getAccountForCurrency(CoinapultAccount.Currency currency) {
      for (CoinapultAccount account : coinapultAccounts.values()) {
         if (account.getCoinapultCurrency().equals(currency)) {
            return account;
         }
      }
      return null;
   }

   @android.support.annotation.Nullable
   private CoinapultAccount getAccountForCurrency(String currency) {
      if (CoinapultAccount.Currency.all.containsKey(currency)) {
         CoinapultAccount.Currency currencyAccount = CoinapultAccount.Currency.all.get(currency);
         return getAccountForCurrency(currencyAccount);
      } else {
         return null;
      }
   }

   public boolean hasCurrencyEnabled(CoinapultAccount.Currency currency){
      return getAccountForCurrency(currency) != null;
   }


   @Override
   public Map<UUID, WalletAccount> getAccounts() {
      return ImmutableMap.<UUID, WalletAccount>copyOf(coinapultAccounts);
   }

   @Override
   public CoinapultAccount getAccount(UUID id) {
      return coinapultAccounts.get(id);
   }

   public void scanForAccounts() {
      try {
         Map<String, AccountInfo.Balance> balances = getBalances();

         // check if we have accounts enabled for all currencies with balances,
         // if not, enable the account, if we know how to handle it
         for (AccountInfo.Balance balance : balances.values()) {
            if (balance.amount.compareTo(BigDecimal.ZERO) > 0) {
               CoinapultAccount accountForCurrency = getAccountForCurrency(balance.currency);
               if (accountForCurrency == null) {
                  enableCurrency(balance.currency);
               }
            }
         }
      } catch (CoinapultClient.CoinapultBackendException e) {
         logger.logError("error while scanning for accounts");
      }
   }

   @Override
   public boolean hasAccount(UUID uuid) {
      return coinapultAccounts.containsKey(uuid);
   }

   // no timeout needed for this cache - if we created or know that the account exists,
   // it will likely be true from here on
   private Boolean userAccountExistsCache = null;

   public boolean userAccountExists() throws CoinapultClient.CoinapultBackendException {
      // if we already know it exists, dont try to query the coinapult server again,
      // otherwise make a call to accountInfo and see if we get any result
      if (userAccountExistsCache == null || !userAccountExistsCache) {
         userAccountExistsCache = getClient().accountExists();
      }
      return userAccountExistsCache;
   }

   public InMemoryPrivateKey getAccountKey() {
      return accountKey;
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

   private Supplier<List<AccountInfo.Balance>> queryBalances = new Supplier<List<AccountInfo.Balance>>() {
      @Override
      public List<AccountInfo.Balance> get() {
         try {
            return getClient().accountInfo().balances;
         } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
         } catch (IOException e) {
            logger.logError("error while getting balances");
            return null;
         }
      }
   };

   private final Supplier<List<AccountInfo.Balance>> balancesCache =
         Suppliers.memoizeWithExpiration(queryBalances, CACHE_DURATION, TimeUnit.SECONDS);

   private final Supplier<List<Transaction.Json>> historyCache = Suppliers.synchronizedSupplier(
         Suppliers.memoizeWithExpiration(queryHistory(), CACHE_DURATION, TimeUnit.SECONDS)
   );

   public Map<String, AccountInfo.Balance> getBalances() throws CoinapultClient.CoinapultBackendException {
      List<AccountInfo.Balance> balances;
      balances = balancesCache.get();
      if (balances == null) {
         throw new CoinapultClient.CoinapultBackendException("unable to get balances");
      }

      return Maps.uniqueIndex(balances, new Function<AccountInfo.Balance, String>() {
         @Nullable
         @Override
         public String apply(AccountInfo.Balance input) {
            return input.currency;
         }
      });
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
         if (!result.verified) {
            logger.logError("Coinapult email error: " + result.error);
         }
         return result.verified;
      } catch (IOException e) {
         return false;
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   public CoinapultClient getClient() {
      return coinapultClient;
   }

   private Supplier<List<Transaction.Json>> queryHistory() {
      return Suppliers.synchronizedSupplier(new Supplier<List<Transaction.Json>>() {
         private List<Transaction.Json> history;

         @Override
         public synchronized List<Transaction.Json> get() {
            SearchMany.Json batch;
            history = Lists.newArrayList();
            //get first page to get pageCount
            try {
               batch = getClient().history(1);
               addToHistory(batch);
               //get extra pages
               for (int i = 2; batch.page < batch.pageCount; i++) {
                  batch = getClient().history(i);
                  addToHistory(batch);
               }

               return history;
            } catch (CoinapultClient.CoinapultBackendException e) {
               logger.logError("error while getting history", e);
               return null;
            }
         }

         private void addToHistory(SearchMany.Json batch) {
            if (batch == null || batch.result == null) {
               return;
            }
            history.addAll(batch.result);
         }
      });
   }

   public List<Transaction.Json> getHistory() throws CoinapultClient.CoinapultBackendException {
      return historyCache.get();
   }
}
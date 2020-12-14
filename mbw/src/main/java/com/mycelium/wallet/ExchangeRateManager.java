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

package com.mycelium.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycelium.wallet.exchange.GetExchangeRate;
import com.mycelium.wallet.external.changelly.ChangellyAPIService;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyAnswerDouble;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.GetExchangeRatesRequest;
import com.mycelium.wapi.api.response.GetExchangeRatesResponse;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.Util;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BCH;
// import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC; gets shadowed by the local definition of the same value.

public class ExchangeRateManager implements ExchangeRateProvider {
    private static final long MIN_RATE_AGE_MS = TimeUnit.SECONDS.toMillis(5);
    public static final String BTC = "BTC";

    private static final Pattern EXCHANGE_RATE_PATTERN;
    private static final String CHANGELLY_MARKET = "Changelly";
    private static final String RMC_MARKET = "Coinmarketcap";

    static {
        String regexKeyExchangeRate = "(.*)_(.*)_(.*)";
        EXCHANGE_RATE_PATTERN = Pattern.compile(regexKeyExchangeRate);
    }

    public interface Observer {
        void refreshingExchangeRatesSucceeded();

        void refreshingExchangeRatesFailed();

        void exchangeSourceChanged();
    }

    private final Context _applicationContext;
    private final Wapi _api;

    private volatile List<String> _fiatCurrencies;
    private Map<String, Map<String, GetExchangeRatesResponse>> _latestRates;
    private long _latestRatesTime;
    private volatile Fetcher _fetcher;
    private final Object _requestLock = new Object();
    private final List<Observer> _subscribers;
    private Map<String, String> _currentExchangeSourceName = new HashMap<>();

    private float rateRmcBtc;
    private float rateBchBtc;
    // value hardcoded for now, but in future we need get from somewhere
    private static final float MSS_RATE = 3125f;

    private MetadataStorage storage;

    ExchangeRateManager(Context applicationContext, Wapi api, MetadataStorage storage) {
        _applicationContext = applicationContext;
        _api = api;
        _latestRatesTime = 0;
        Gson gson = new GsonBuilder().create();
        String preferenceValue = getPreferences().getString(Constants.EXCHANGE_RATE_SETTING, null);
        if (preferenceValue == null) {
            _currentExchangeSourceName.put(Utils.getBtcCoinType().getSymbol(), Constants.DEFAULT_EXCHANGE);
        } else {
            _currentExchangeSourceName = gson.fromJson(preferenceValue, new TypeToken<Map<String, String>>(){}.getType());
        }
        _subscribers = new LinkedList<>();
        _latestRates = new HashMap<>();
        this.storage = storage;
        ChangellyAPIService.retrofit.create(ChangellyAPIService.class)
                .getExchangeAmount(BCH, BTC, 1)
                .enqueue(new GetOfferCallback());
    }

    public synchronized void subscribe(Observer subscriber) {
        _subscribers.add(subscriber);
    }

    public synchronized void unsubscribe(Observer subscriber) {
        _subscribers.remove(subscriber);
    }

    private class Fetcher implements Runnable {
        public void run() {
            List<String> selectedCurrencies;
            List<String> cryptocurrencies = MbwManager.getInstance(_applicationContext).getWalletManager(false).getCryptocurrenciesSymbols();

            synchronized (_requestLock) {
                selectedCurrencies = new ArrayList<>(_fiatCurrencies);
            }

            try {
                List<GetExchangeRatesResponse> responses = new ArrayList<>(cryptocurrencies.size() * selectedCurrencies.size());
                for (String cryptocurrency : cryptocurrencies) {
                    for (String currency : selectedCurrencies) {
                        responses.add(_api.getExchangeRates(new GetExchangeRatesRequest(Wapi.VERSION,
                                Util.trimTestnetSymbolDecoration(cryptocurrency), currency)).getResult());
                    }
                }
                synchronized (_requestLock) {
                    setLatestRates(responses);
                    _fetcher = null;
                    notifyRefreshingExchangeRatesSucceeded();
                }
            } catch (WapiException e) {
                // we failed to get the exchange rate, try to restore saved values from the local database
                Map<String, String> savedExchangeRates = storage.getAllExchangeRates();
                if (savedExchangeRates.entrySet().size() > 0) {
                    synchronized (_requestLock) {
                        setLatestRates(localValues(cryptocurrencies, selectedCurrencies, savedExchangeRates));
                        _fetcher = null;
                        notifyRefreshingExchangeRatesSucceeded();
                    }
                } else {
                    synchronized (_requestLock) {
                        _fetcher = null;
                        notifyRefreshingExchangeRatesFailed();
                    }
                }
            }
            Optional<String> rate = storage.getExchangeRate("BCH", "BTC", CHANGELLY_MARKET);
            if (rate.isPresent()) {
                rateBchBtc = Float.parseFloat(rate.get());
            }
        }
    }


    private List<GetExchangeRatesResponse> localValues(List<String> cryptocurrencies, List<String> selectedCurrencies,
                                                       Map<String, String> savedExchangeRates) {
        List<GetExchangeRatesResponse> responses = new ArrayList<>(selectedCurrencies.size());

        for (String currency : selectedCurrencies) {
            for (String cryptocurrency : cryptocurrencies) {
                List<ExchangeRate> exchangeRates = new ArrayList<>();
                for (Map.Entry<String, String> entry : savedExchangeRates.entrySet()) {
                    String key = entry.getKey();
                    Matcher matcher = EXCHANGE_RATE_PATTERN.matcher(key);

                    if (matcher.find()) {
                        String market = matcher.group(1);
                        String relatedCurrency = matcher.group(2); //BTC
                        String baseCurrency = matcher.group(3); //fiat

                        if (relatedCurrency.equals(cryptocurrency) && baseCurrency.equals(currency)) {
                            double price;
                            try {
                                price = Double.parseDouble(entry.getValue());
                            } catch (NumberFormatException nfe) {
                                price = 0.0;
                            }
                            ExchangeRate exchangeRate = new ExchangeRate(market, new Date().getTime(), price, currency);
                            exchangeRates.add(exchangeRate);
                        }
                    }
                }

                responses.add(new GetExchangeRatesResponse(cryptocurrency, currency, exchangeRates.toArray(new ExchangeRate[0])));
            }
        }

        return responses;
    }

    private void notifyRefreshingExchangeRatesSucceeded() {
        for (Observer s : _subscribers) {
            s.refreshingExchangeRatesSucceeded();
        }
    }

    private void notifyRefreshingExchangeRatesFailed() {
        for (Observer s : _subscribers) {
            s.refreshingExchangeRatesFailed();
        }
    }

    private void notifyExchangeSourceChanged() {
        for (Observer s : _subscribers) {
            s.exchangeSourceChanged();
        }
    }

    // only refresh if last refresh is old
    public void requestOptionalRefresh() {
        if (System.currentTimeMillis() - _latestRatesTime > MIN_RATE_AGE_MS) {
            requestRefresh();
        }
    }

    public void requestRefresh() {
        synchronized (_requestLock) {
            // Only start fetching if we are not already on it
            if (_fetcher == null) {
                _fetcher = new Fetcher();
                Thread t = new Thread(_fetcher);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private synchronized void setLatestRates(List<GetExchangeRatesResponse> latestRates) {
        if (latestRates.isEmpty()) {
            return;
        }
        _latestRates = new HashMap<>();
        for (GetExchangeRatesResponse response : latestRates) {
            String fromCurrency = Util.addTestnetSymbolDecoration(response.getFromCurrency(), BuildConfig.FLAVOR.equals("btctestnet"));
            String toCurrency = Util.addTestnetSymbolDecoration(response.getToCurrency(), BuildConfig.FLAVOR.equals("btctestnet"));
            if (_latestRates.get(fromCurrency) != null) {
                _latestRates.get(fromCurrency).put(toCurrency, response);
            } else {
                _latestRates.put(fromCurrency, new HashMap<>(Collections.singletonMap(toCurrency, response)));
            }
            for (ExchangeRate rate : response.getExchangeRates()) {
                storage.storeExchangeRate(fromCurrency, rate.currency, rate.name, String.valueOf(rate.price));
            }
            if (_currentExchangeSourceName.get(fromCurrency) == null) {
                // This only happens the first time the wallet picks up exchange rates.
                List<String> exchangeSourceNames = getExchangeSourceNames(fromCurrency);
                if (exchangeSourceNames != null && !exchangeSourceNames.isEmpty()) {
                    String exchange = exchangeSourceNames.contains(Constants.DEFAULT_EXCHANGE) ?
                            Constants.DEFAULT_EXCHANGE : exchangeSourceNames.get(0);
                    _currentExchangeSourceName.put(fromCurrency, exchange);
                }
            }
        }
        _latestRatesTime = System.currentTimeMillis();

    }

    /**
     * Get the name of the current exchange rate. May be null the first time the
     * app is running
     */
    public String getCurrentExchangeSourceName(String coinSymbol) {
        return _currentExchangeSourceName.get(coinSymbol);
    }

    /**
     * Get the names of the currently available exchange rates. May be empty the
     * first time the app is running
     */
    public synchronized List<String> getExchangeSourceNames(String cryptocurrency) {
        List<String> result = new LinkedList<>();
        //check whether we have any rates
        Map<String, GetExchangeRatesResponse> latestRatesForCryptocurrency = _latestRates.get(cryptocurrency);
        if (latestRatesForCryptocurrency == null || latestRatesForCryptocurrency.isEmpty()) {
            return result;
        }
        GetExchangeRatesResponse latestRates = latestRatesForCryptocurrency.values().iterator().next();
        if (latestRates != null) {
            for (ExchangeRate r : latestRates.getExchangeRates()) {
                result.add(r.name);
            }
        }
        return result;
    }

    public void setCurrentExchangeSourceName(String coinSymbol, String name) {
        _currentExchangeSourceName.put(coinSymbol, name);
        Gson gson = new GsonBuilder().create();
        getEditor().putString(Constants.EXCHANGE_RATE_SETTING, gson.toJson(_currentExchangeSourceName)).apply();
        notifyExchangeSourceChanged();
    }

    /**
     * Get the exchange rate for the specified currency.
     * <p/>
     * Returns null if the current rate is too old
     * In that the case the caller could choose to call refreshRates() and listen
     * for callbacks. If a rate is returned the contained price may be null if
     * the currently chosen exchange source is not available.
     */
    public ExchangeRate getExchangeRate(String source, String destination, String exchangeSource) {
        Map<String, GetExchangeRatesResponse> latestRatesForSourceCurrency = _latestRates.get(source);

        // TODO need some refactoring for this
        String injectCurrency = null;
        if (destination.equals("RMC") || destination.equals("MSS") || destination.equals("BCH")) {
            injectCurrency = destination;
            destination = "USD";
        }
        if (latestRatesForSourceCurrency == null || latestRatesForSourceCurrency.isEmpty() || !latestRatesForSourceCurrency.containsKey(destination)) {
            return null;
        }
        GetExchangeRatesResponse latestRatesForTargetCurrency = latestRatesForSourceCurrency.get(destination);
        if (latestRatesForTargetCurrency == null) {
            //rate is too old or does not exists, exchange source seems to not be available
            //we return a rate with null price to indicate there is something wrong with the exchange rate source
            return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination);
        }

        ExchangeRate[] exchangeRates = latestRatesForTargetCurrency.getExchangeRates();
        if (exchangeRates == null) {
            return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination);
        }
        for (ExchangeRate r : exchangeRates) {
            if (r.name.equals(exchangeSource)) {
                //if the price is 0, obviously something went wrong
                if (r.price.equals(0d)) {
                    //we return an exchange rate with null price -> indicating missing rate
                    return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination);
                }
                //everything is fine, return the rate
                return getOtherExchangeRate(source, injectCurrency, r);
            }
        }
        if (exchangeSource != null) {
            // We end up here if the exchange is no longer on the list
            return ExchangeRate.missingRate(exchangeSource, System.currentTimeMillis(), destination);
        }
        return null;
    }

    @Override
    public ExchangeRate getExchangeRate(String source, String destination) {
        return getExchangeRate(source, destination, _currentExchangeSourceName.get(source));
    }

    private ExchangeRate getOtherExchangeRate(String coinSymbol, String injectCurrency, ExchangeRate r) {
        double rate = r.price;
        if ("RMC".equals(injectCurrency)) {
            if (rateRmcBtc != 0) {
                rate = 1 / rateRmcBtc;
            } else {
                return ExchangeRate.missingRate(_currentExchangeSourceName.get(coinSymbol), System.currentTimeMillis(), "RMC");
            }
        }
        if ("MSS".equals(injectCurrency)) {
            rate = r.price * MSS_RATE;
        }
        if ("BCH".equals(injectCurrency)) {
            if (rateBchBtc != 0) {
                rate = 1 / rateBchBtc;
            } else {
                return ExchangeRate.missingRate(_currentExchangeSourceName.get(coinSymbol), System.currentTimeMillis(), "BCH");
            }
        }
        return new ExchangeRate(r.name, r.time, rate, injectCurrency);
    }

    private SharedPreferences.Editor getEditor() {
        return getPreferences().edit();
    }

    private SharedPreferences getPreferences() {
        return _applicationContext.getSharedPreferences(Constants.EXCHANGE_DATA, Activity.MODE_PRIVATE);
    }

    // set for which fiat currencies we should get fx rates for
    public void setCurrencyList(Set<AssetInfo> currencies) {
        synchronized (_requestLock) {
            // copy list to prevent changes from outside
            ImmutableList.Builder<String> listBuilder = new ImmutableList.Builder<>();
            for (AssetInfo currency : currencies) {
                listBuilder.add(currency.getSymbol());
            }
            _fiatCurrencies = listBuilder.build();
        }

        requestRefresh();
    }

    class GetOfferCallback implements Callback<ChangellyAnswerDouble> {
        @Override
        public void onResponse(@NonNull Call<ChangellyAnswerDouble> call,
                               @NonNull Response<ChangellyAnswerDouble> response) {
            ChangellyAnswerDouble result = response.body();
            if (result != null) {
                rateBchBtc = (float) result.result;
                storage.storeExchangeRate("BCH", "BTC", CHANGELLY_MARKET, String.valueOf(rateBchBtc));
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyAnswerDouble> call, @NonNull Throwable t) {
            Toast.makeText(_applicationContext, "Service unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    public Value get(Value value, AssetInfo toCurrency) {
        GetExchangeRate rate = new GetExchangeRate(MbwManager.getInstance(_applicationContext).getWalletManager(false),
                toCurrency.getSymbol(), value.type.getSymbol(), this).invoke();
        BigDecimal rateValue = rate.getRate();
        if (rateValue != null) {
            BigDecimal bigDecimal = rateValue.multiply(new BigDecimal(value.value))
                    .movePointLeft(value.type.getUnitExponent())
                    .round(MathContext.DECIMAL128);
            return Value.parse(toCurrency, bigDecimal);
        } else {
            return null;
        }
    }
}
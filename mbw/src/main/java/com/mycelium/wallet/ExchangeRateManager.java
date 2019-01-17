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
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mycelium.wallet.exchange.CoinmarketcapApi;
import com.mycelium.wallet.exchange.model.CoinmarketcapRate;
import com.mycelium.wallet.external.changelly.ChangellyAPIService;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyAnswerDouble;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.request.QueryExchangeRatesRequest;
import com.mycelium.wapi.api.response.QueryExchangeRatesResponse;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.currency.ExchangeRateProvider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.RetrofitError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BCH;
// import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC; gets shadowed by the local definition of the same value.

public class ExchangeRateManager implements ExchangeRateProvider {
    private static final long MAX_RATE_AGE_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long MIN_RATE_AGE_MS = TimeUnit.SECONDS.toMillis(5);
    private static final String EXCHANGE_DATA = "wapi_exchange_rates";
    public static final String BTC = "BTC";

    private static final Pattern EXCHANGE_RATE_PATTERN;
    private static final String CHANGELLY_MARKET = "Changelly";
    private static final String RMC_MARKET = "Coinmarketcap";
    private static final String DEFAULT_EXCHANGE = "Bitstamp";

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
    private Map<String, QueryExchangeRatesResponse> _latestRates;
    private long _latestRatesTime;
    private volatile Fetcher _fetcher;
    private final Object _requestLock = new Object();
    private final List<Observer> _subscribers;
    private String _currentExchangeSourceName;

    private float rateRmcBtc;
    private float rateBchBtc;
    // value hardcoded for now, but in future we need get from somewhere
    private static final float MSS_RATE = 3125f;

    private MetadataStorage storage;

    ExchangeRateManager(Context applicationContext, Wapi api, MetadataStorage storage) {
        _applicationContext = applicationContext;
        _api = api;
        _latestRates = null;
        _latestRatesTime = 0;
        _currentExchangeSourceName = getPreferences().getString("currentRateName", DEFAULT_EXCHANGE);

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

            synchronized (_requestLock) {
                selectedCurrencies = new ArrayList<>(_fiatCurrencies);
            }

            try {
                List<QueryExchangeRatesResponse> responses = new ArrayList<>();

                for (String currency : selectedCurrencies) {
                    responses.add(_api.queryExchangeRates(new QueryExchangeRatesRequest(Wapi.VERSION, currency)).getResult());
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
                        setLatestRates(localValues(selectedCurrencies, savedExchangeRates));
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
            //Get rates from Coinmarket
            try {
                CoinmarketcapRate rmcRate = CoinmarketcapApi.getRate();
                if (rmcRate != null) {
                    rateRmcBtc = rmcRate.getPriceBtc();
                    storage.storeExchangeRate("RMC", "BTC", RMC_MARKET, String.valueOf(rateRmcBtc));
                } else {
                    Optional<String> rate = storage.getExchangeRate("RMC", "BTC", RMC_MARKET);
                    if (rate.isPresent()) {
                        rateRmcBtc = Float.parseFloat(rate.get());
                    }
                }
            } catch (RetrofitError error) {
                Log.e("ExcangeRateManager", "get rmc rate from Coinmarketcap ", error);
            }
            Optional<String> rate = storage.getExchangeRate("BCH", "BTC", CHANGELLY_MARKET);
            if (rate.isPresent()) {
                rateBchBtc = Float.parseFloat(rate.get());
            }
        }
    }

    private List<QueryExchangeRatesResponse> localValues(List<String> selectedCurrencies,
                                                                Map<String, String> savedExchangeRates) {
        List<QueryExchangeRatesResponse> responses = new ArrayList<>();

        for (String currency : selectedCurrencies) {
            List<ExchangeRate> exchangeRates = new ArrayList<>();
            for (Map.Entry<String, String> entry : savedExchangeRates.entrySet()) {
                String key = entry.getKey();
                Matcher matcher = EXCHANGE_RATE_PATTERN.matcher(key);

                if (matcher.find()) {
                    String market = matcher.group(1);
                    String relatedCurrency = matcher.group(2); //BTC
                    String baseCurrency = matcher.group(3); //fiat

                    if (relatedCurrency.equals(BTC) && baseCurrency.equals(currency)) {
                        Double price;
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

            responses.add(new QueryExchangeRatesResponse(currency, exchangeRates.toArray(new ExchangeRate[0])));
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

    private synchronized void setLatestRates(List<QueryExchangeRatesResponse> latestRates) {
        _latestRates = new HashMap<>();
        for (QueryExchangeRatesResponse response : latestRates) {
            _latestRates.put(response.currency, response);

            for(ExchangeRate rate : response.exchangeRates) {
                storage.storeExchangeRate(BTC, rate.currency, rate.name, String.valueOf(rate.price));
            }
        }
        _latestRatesTime = System.currentTimeMillis();

        if (_currentExchangeSourceName == null) {
            // This only happens the first time the wallet picks up exchange rates.
            // We will default to the first exchange
            if (latestRates.size() > 0 && latestRates.get(0).exchangeRates.length > 0) {
                _currentExchangeSourceName = latestRates.get(0).exchangeRates[0].name;
            }
        }
    }

    /**
     * Get the name of the current exchange rate. May be null the first time the
     * app is running
     */
    public String getCurrentExchangeSourceName() {
        return _currentExchangeSourceName;
    }

    /**
     * Get the names of the currently available exchange rates. May be empty the
     * first time the app is running
     */
    public synchronized List<String> getExchangeSourceNames() {
        List<String> result = new LinkedList<>();
        //check whether we have any rates
        if (_latestRates.isEmpty()) return result;
        QueryExchangeRatesResponse latestRates = _latestRates.values().iterator().next();
        if (latestRates != null) {
            for (ExchangeRate r : latestRates.exchangeRates) {
                result.add(r.name);
            }
        }
        return result;
    }

    public void setCurrentExchangeSourceName(String name) {
        _currentExchangeSourceName = name;
        getEditor().putString("currentRateName", _currentExchangeSourceName).apply();
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
    public ExchangeRate getExchangeRate(String currency, String source) {
        // TODO need some refactoring for this
        String injectCurrency = null;
        if (currency.equals("RMC") || currency.equals("MSS") || currency.equals("BCH")) {
            injectCurrency = currency;
            currency = "USD";
        }
        if (_latestRates == null || _latestRates.isEmpty() || !_latestRates.containsKey(currency)) {
            return null;
        }
        QueryExchangeRatesResponse latestRatesForCurrency = _latestRates.get(currency);
        if (_latestRatesTime + MAX_RATE_AGE_MS < System.currentTimeMillis() || latestRatesForCurrency == null) {
            //rate is too old or does not exists, source seems to not be available
            //we return a rate with null price to indicate there is something wrong with the exchange rate source
            return ExchangeRate.missingRate(source, System.currentTimeMillis(), currency);
        }

        ExchangeRate[] exchangeRates = latestRatesForCurrency.exchangeRates;
        if (exchangeRates == null) {
            return ExchangeRate.missingRate(source, System.currentTimeMillis(), currency);
        }
        for (ExchangeRate r : exchangeRates) {
            if (r.name.equals(source)) {
                //if the price is 0, obviously something went wrong
                if (r.price.equals(0d)) {
                    //we return an exchange rate with null price -> indicating missing rate
                    return ExchangeRate.missingRate(source, System.currentTimeMillis(), currency);
                }
                //everything is fine, return the rate
                return getOtherExchangeRate(injectCurrency, r);
            }
        }
        if (source != null) {
            // We end up here if the exchange is no longer on the list
            return ExchangeRate.missingRate(source, System.currentTimeMillis(), currency);
        }
        return null;
    }

    @Override
    public ExchangeRate getExchangeRate(String currency) {
        return getExchangeRate(currency, _currentExchangeSourceName);
    }

    private ExchangeRate getOtherExchangeRate(String injectCurrency, ExchangeRate r) {
        double rate = r.price;
        if ("RMC".equals(injectCurrency)) {
            if (rateRmcBtc != 0) {
                rate = 1 / rateRmcBtc;
            } else {
                return ExchangeRate.missingRate(_currentExchangeSourceName, System.currentTimeMillis(), "RMC");
            }
        }
        if ("MSS".equals(injectCurrency)) {
            rate = r.price * MSS_RATE;
        }
        if ("BCH".equals(injectCurrency)) {
            if (rateBchBtc != 0) {
                rate = 1 / rateBchBtc;
            } else {
                return ExchangeRate.missingRate(_currentExchangeSourceName, System.currentTimeMillis(), "BCH");
            }
        }
        return new ExchangeRate(r.name, r.time, rate, injectCurrency);
    }

    private SharedPreferences.Editor getEditor() {
        return _applicationContext.getSharedPreferences(EXCHANGE_DATA, Activity.MODE_PRIVATE).edit();
    }

    private SharedPreferences getPreferences() {
        return _applicationContext.getSharedPreferences(EXCHANGE_DATA, Activity.MODE_PRIVATE);
    }

    // set for which fiat currencies we should get fx rates for
    void setCurrencyList(Set<String> currencies) {
        synchronized (_requestLock) {
            // copy list to prevent changes from outside
            ImmutableList.Builder<String> listBuilder = new ImmutableList.Builder<String>().addAll(currencies);
            _fiatCurrencies = listBuilder.build();
        }

        requestRefresh();
    }

    class GetOfferCallback implements Callback<ChangellyAnswerDouble> {
        @Override
        public void onResponse(@NonNull Call<ChangellyAnswerDouble> call,
                               @NonNull Response<ChangellyAnswerDouble> response) {
            ChangellyAnswerDouble result = response.body();
            if(result != null) {
                rateBchBtc = (float) result.result;
                storage.storeExchangeRate("BCH", "BTC", CHANGELLY_MARKET, String.valueOf(rateBchBtc));
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyAnswerDouble> call, @NonNull Throwable t) {
            Toast.makeText(_applicationContext, "Service unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.mycelium.wallet.external.changelly;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;

import com.mycelium.wallet.external.changelly.ChangellyAPIService.*;

public class ChangellyService extends IntentService {
    private static final String LOG_TAG="ChangellyService";
    private static final String PACKAGE_NAME = "com.mycelium.wallet.external.changelly";
    public static final String ACTION_GET_CURRENCIES = PACKAGE_NAME + ".GETCURRENCIES";
    public static final String ACTION_GET_MIN_EXCHANGE = PACKAGE_NAME + ".GETMINEXCHANGE";
    public static final String ACTION_GET_EXCHANGE_AMOUNT = PACKAGE_NAME + ".GETEXCHANGEAMOUNT";
    public static final String ACTION_CREATE_TRANSACTION = PACKAGE_NAME + ".CREATETRANSACTION";

    public static final String INFO_CURRENCIES = PACKAGE_NAME + ".INFOCURRENCIES";
    public static final String INFO_MIN_AMOUNT = PACKAGE_NAME + ".INFOMINAMOUNT";
    public static final String INFO_EXCH_AMOUNT = PACKAGE_NAME + ".INFOEXCHAMOUNT";
    public static final String INFO_TRANSACTION = PACKAGE_NAME + ".INFOTRANSACTION";
    public static final String INFO_ERROR       = PACKAGE_NAME + ".INFOERROR";

    public static final String BCH = "BCH";
    public static final String BTC = "BTC";

    public static final String CURRENCIES = "CURRENCIES";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String AMOUNT = "AMOUNT";
    public static final String DESTADDRESS = "DESTADDRESS";
    public static final String TXID = "TXID";
    public static final String OFFER = "OFFER";

    private ChangellyAPIService changellyAPIService = ChangellyAPIService.retrofit.create(ChangellyAPIService.class);

    private List<String> currencies;
    private Date dateCurrencies;

    private HashMap<String, Double> currenciesMinAmounts;

    public ChangellyService() {
        super("ChangellyService");
    }

    private void loadCurrencies() {
        // 1. request list of supported currencies
        try {
            ChangellyAnswerListString result = changellyAPIService.getCurrencies().execute().body();
            String currs = "";
            if(result != null) {
                currencies = result.result;
                dateCurrencies = new Date();
                currs = TextUtils.join(" ", currencies);
            }
            Log.d(LOG_TAG, "Active currencies: " + currs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getMinAmount(String from, String to) {
        assert(currencies != null);
        assert(currencies.contains(from));
        assert(currencies.contains(to));
        // 2. ask for minimum amount to exchange
        Call<ChangellyAnswerDouble> call2 = changellyAPIService.getMinAmount(from, to);
        try {
            ChangellyAnswerDouble result = call2.execute().body();
            if(result != null) {
                Log.d("MyceliumChangelly", "Minimum amount " + from + to + ": " + result.result);
                if(currenciesMinAmounts == null) {
                    currenciesMinAmounts = new HashMap<>();
                }
                currenciesMinAmounts.put(from+to, result.result);
                return result.result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private double getExchangeAmount(String from, String to, double amount) {
        Call<ChangellyAnswerDouble> call3 = changellyAPIService.getExchangeAmount(from, to, amount);
        try {
            ChangellyAnswerDouble result = call3.execute().body();
            if(result != null) {
                Log.d("MyceliumChangelly", "You will receive the following " + to + " amount: " + result.result);
                return result.result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // return txid?
    private ChangellyTransactionOffer createTransaction(String from, String to, double amount, String destAddress) {
        Call<ChangellyTransaction> call4 = changellyAPIService.createTransaction(from, to, amount, destAddress);
        try {
            ChangellyTransaction result = call4.execute().body();
            if(result != null) {

                //Log.d("MyceliumChangelly", "createTransaction answer: " + result.result);
                return result.result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(LOG_TAG, "onHandleIntent: ${intent?.action}");
        if (intent != null && intent.getAction() != null) {
            String from, to, destAddress;
            double min, amount, offer;
            switch(intent.getAction()) {
                case ACTION_GET_CURRENCIES:
                    if(currencies == null) { // TODO: check freshness || dateCurrencies.before(new Date())) {
                        loadCurrencies();
                    }
                    // if failed to load, return error
                    if(currencies == null || currencies.size() == 0) {

                        Intent errorIntent = new Intent(ChangellyService.INFO_ERROR, null,
                                this, ChangellyService.class);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
                        break;
                    }
                    Intent currenciesIntent = new Intent(ChangellyService.INFO_CURRENCIES, null, this,
                            ChangellyService.class);
                    currenciesIntent.putExtra(CURRENCIES, currencies.toArray());
                    currenciesIntent.putStringArrayListExtra(CURRENCIES, new ArrayList<>(currencies));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(currenciesIntent);
                    break;
                case ACTION_GET_MIN_EXCHANGE:
                    from = intent.getStringExtra(FROM);
                    to = intent.getStringExtra(TO);
                    min = getMinAmount(from, to);
                    if(min == -1) {
                        // service unavailable
                        Intent errorIntent = new Intent(ChangellyService.INFO_ERROR, null,
                                this, ChangellyService.class);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
                        return;
                    }
                    // service available
                    Intent minAmountIntent = new Intent(ChangellyService.INFO_MIN_AMOUNT, null,
                            this, ChangellyService.class);
                        minAmountIntent.putExtra(FROM, from);
                        minAmountIntent.putExtra(TO, to);
                        minAmountIntent.putExtra(AMOUNT, min);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(minAmountIntent);
                    break;
                case ACTION_GET_EXCHANGE_AMOUNT:
                    from = intent.getStringExtra(FROM);
                    to = intent.getStringExtra(TO);
                    amount = intent.getDoubleExtra(AMOUNT, 0);
                    offer = getExchangeAmount(from, to, amount);
                    if(offer == -1) {
                        // service unavailable
                        Intent errorIntent = new Intent(ChangellyService.INFO_ERROR, null,
                                this, ChangellyService.class);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
                        return;
                    }
                    // service available
                    Intent exchangeAmountIntent = new Intent(ChangellyService.INFO_EXCH_AMOUNT, null,
                            this, ChangellyService.class);
                    exchangeAmountIntent.putExtra(FROM, from);
                    exchangeAmountIntent.putExtra(TO, to);
                    exchangeAmountIntent.putExtra(AMOUNT, offer);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(exchangeAmountIntent);
                    break;
                case ACTION_CREATE_TRANSACTION:
                    from = intent.getStringExtra(FROM);
                    to = intent.getStringExtra(TO);
                    amount = intent.getDoubleExtra(AMOUNT, 0);
                    destAddress = intent.getStringExtra(DESTADDRESS);
                    ChangellyTransactionOffer res = createTransaction(from, to, amount, destAddress);
                    Intent transactionIntent;
                    if(res == null) {
                        // service unavailable
                        transactionIntent = new Intent(ChangellyService.INFO_ERROR, null,
                                this, ChangellyService.class);
                    } else {
                        // service available
                        // example answer
                        //{"jsonrpc":"2.0","id":"test","result":{"id":"39526c0eb6ba","apiExtraFee":"0","changellyFee":"0.5","payinExtraId":null,"status":"new","currencyFrom":"eth","currencyTo":"BTC","amountTo":0,"payinAddress":"0xdd0a917944efc6a371829053ad318a6a20ee1090","payoutAddress":"1J3cP281yiy39x3gcPaErDR6CSbLZZKzGz","createdAt":"2017-11-22T18:47:19.000Z"}}
                        transactionIntent = new Intent(ChangellyService.INFO_TRANSACTION, null,
                                this, ChangellyService.class);
                        res.amountFrom = amount;
                        transactionIntent.putExtra(OFFER, res);
                    }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(transactionIntent);
                    break;
            }
        }
    }
}

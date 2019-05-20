package com.mycelium.wallet.external.changelly;

import java.io.Serializable;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 *  Interface to describing Changelly API for retrofit2 library and providing retrofit object intialization.
 **/
public interface ChangellyAPIService {
    String BCH = "BCH";
    String BTC = "BTC";
    String FROM = "FROM";
    String TO = "TO";
    String AMOUNT = "AMOUNT";
    String DESTADDRESS = "DESTADDRESS";
    class ChangellyCurrency {
        public String currency;
        public boolean enabled;
    }

    class ChangellyTransactionOffer implements Serializable {
        public String id;
        public double apiExtraFee;
        public double changellyFee;
        public String payinExtraId;
        public String status;
        public String currencyFrom;
        public String currencyTo;
        public double amountTo;
        public String payinAddress;
        public String payoutAddress;
        public String createdAt;
    }

    //{"jsonrpc":"2.0","id":"test","result":{"id":"39526c0eb6ba","apiExtraFee":"0","changellyFee":"0.5","payinExtraId":null,"status":"new","currencyFrom":"eth","currencyTo":"BTC","amountTo":0,"payinAddress":"0xdd0a917944efc6a371829053ad318a6a20ee1090","payoutAddress":"1J3cP281yiy39x3gcPaErDR6CSbLZZKzGz","createdAt":"2017-11-22T18:47:19.000Z"}}
    class ChangellyTransaction {
        public ChangellyTransactionOffer result; // payin_address, ID
    }

    // {"jsonrpc":"2.0","id":"test","result":"0.03595702"}
    class ChangellyAnswerDouble {
        public double result;
    }

    class ChangellyAnswerListString {
        public List<String> result;
    }
    // end data classes

    @POST("getCurrencies")
    Call<ChangellyAnswerListString> getCurrencies();

    @POST("getCurrenciesFull")
    List<ChangellyCurrency> getCurrenciesFull();

    @POST("getMinAmount")
    Call<ChangellyAnswerDouble> getMinAmount(@Query("from") String from, @Query("to") String to);

    @POST("getExchangeAmount")
    Call<ChangellyAnswerDouble> getExchangeAmount(@Query("from") String from, @Query("to") String to, @Query("amount") double amount);

    @POST("createTransaction")
    Call<ChangellyTransaction> createTransaction(@Query("from") String from, @Query("to") String to, @Query("amount") double amount, @Query("address") String address);

//    @POST("getStatus")
//    Call<ChangellyStatus> getStatus(@Query("transaction") String transaction);

    @POST("getTransactions")
    Call<List<ChangellyTransaction>> getTransactions();

    //public static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    ChangellyHeaderInterceptor changellyHeader = new ChangellyHeaderInterceptor();
    //public static final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(changellyHeader).addInterceptor(logging).build();
    OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(changellyHeader).build();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.changelly.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build();
}

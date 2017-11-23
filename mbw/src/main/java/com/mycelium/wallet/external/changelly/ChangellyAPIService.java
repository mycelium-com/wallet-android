package com.mycelium.wallet.external.changelly;


import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** @brief Interface to describing Changelly API for retrofit2 library and providing retrofit object intialization. **/
public interface ChangellyAPIService {

    class ChangellyJsonRpc {
        String id = "test";
        String jsonrpc = "2.0";
        String method;
        public ChangellyJsonRpc(String method) {
            this.method = method;
        }
    }

    class ChangellyGetCurrencies extends ChangellyJsonRpc {
        public ChangellyGetCurrencies() {
            super("getCurrencies");
        }
    }

    class ChangellyCurrency {
        public String currency;
        public boolean enabled;
    }

    class ChangellyMinAmount extends ChangellyJsonRpc {
        Params params;
        class Params {
            public String from;
            public String to;
            public Params(String from, String to) {
                this.from = from;
                this.to = to;
            }
        }
        public ChangellyMinAmount(String from, String to) {
            super("getMinAmount");
            this.params = new Params(from, to);
        }
    }

    class ChangellyExchangeAmount extends ChangellyJsonRpc {
        Params params;
        class Params {
            public String from;
            public String to;
            public double amount;
            public Params(String from, String to, double amount) {
                this.from = from;
                this.to = to;
                this.amount = amount;
            }
        }
        public ChangellyExchangeAmount(String from, String to, double amount) {
            super("getExchangeAmount");
            this.params = new Params(from, to, amount);
        }
    }

    class ChangellyCreateTransaction extends ChangellyJsonRpc {
        Params params;
        class Params {
            public String from;
            public String to;
            public double amount;
            public String address;
            public Params(String from, String to, double amount, String address) {
                this.from = from;
                this.to = to;
                this.amount = amount;
                this.address = address;
            }
        }
        public ChangellyCreateTransaction(String from, String to, double amount, String address) {
            super("createTransaction");
            this.params = new Params(from, to, amount, address);
        }
    }

    //{"jsonrpc":"2.0","id":"test","result":{"id":"39526c0eb6ba","apiExtraFee":"0","changellyFee":"0.5","payinExtraId":null,"status":"new","currencyFrom":"eth","currencyTo":"BTC","amountTo":0,"payinAddress":"0xdd0a917944efc6a371829053ad318a6a20ee1090","payoutAddress":"1J3cP281yiy39x3gcPaErDR6CSbLZZKzGz","createdAt":"2017-11-22T18:47:19.000Z"}}
    public class ChangellyTransaction extends ChangellyJsonRpcAnswer {
        public ChangellyTransactionOffer result; // payin_address, ID
    }

    // ChangellyStatus
    class ChangellyStatus extends ChangellyJsonRpc {
        Params params;
        class Params {
            public String id;
            public Params(String id) {
                this.id = id;
            }
        }
        public ChangellyStatus(String id) {
            super("getStatus");
            this.params = new Params(id);
        }
    }

    class ChangellyGetTransactions extends ChangellyJsonRpc {
        Params params;
        class Params {
        }
        public ChangellyGetTransactions(String id) {
            super("getTransactions");
            this.params = new Params();
        }
    }

    class ChangellyJsonRpcAnswer {
        String id = "test";
        String jsonrpc = "2.0";
    }

    // {"jsonrpc":"2.0","id":"test","result":"0.03595702"}
    class ChangellyAnswerDouble extends ChangellyJsonRpcAnswer {
        public double result;
        public ChangellyAnswerDouble(double result) {
            super();
            this.result = result;
        }
    }

    class ChangellyAnswerListString extends ChangellyJsonRpcAnswer {
        public List<String> result;
//        public ChangellyAnswerListString(List<String> result) {
//            super();
//            this.result = result;
//        }
    }

    @POST("getCurrencies")
    Call<ChangellyAnswerListString> getCurrencies(@Body ChangellyGetCurrencies body);

    @POST("getCurrenciesFull")
    List<ChangellyCurrency> getCurrenciesFull();

    @POST("getMinAmount")
    Call<ChangellyAnswerDouble> getMinAmount(@Body ChangellyMinAmount minAmount);

    @POST("getExchangeAmount")
    Call<ChangellyAnswerDouble> getExchangeAmount(@Body ChangellyExchangeAmount exchangeAmount);

    @POST("createTransaction")
    Call<ChangellyTransaction> createTransaction(@Body ChangellyCreateTransaction createTransaction);

    // getStatus
    @POST("getStatus")
    Call<ChangellyStatus> getStatus(String transaction);

    // getTransactions
    @POST("getTransactions")
    Call<List<ChangellyTransaction>> getTransactions(@Body ChangellyGetTransactions body);

    //public static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    public static final ChangellyHeaderInterceptor changellyHeader = new ChangellyHeaderInterceptor();
    //public static final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(changellyHeader).addInterceptor(logging).build();
    public static final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(changellyHeader).build();

    public static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.changelly.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build();
}

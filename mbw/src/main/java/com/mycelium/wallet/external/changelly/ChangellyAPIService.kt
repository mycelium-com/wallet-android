package com.mycelium.wallet.external.changelly

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.Serializable

/**
 * Interface to describing Changelly API for retrofit2 library and providing retrofit object intialization.
 */
interface ChangellyAPIService {
    class ChangellyCurrency {
        var currency: String? = null
        var enabled = false
    }

    class ChangellyTransactionOffer : Serializable {
        @JvmField
        var id: String? = null
        var apiExtraFee = 0.0
        var changellyFee = 0.0
        @JvmField
        var payinExtraId: String? = null
        var status: String? = null
        @JvmField
        var currencyFrom: String? = null
        var currencyTo: String? = null
        @JvmField
        var amountTo = 0.0
        @JvmField
        var payinAddress: String? = null
        var payoutAddress: String? = null
        var payoutExtraId: String? = null
        var createdAt: String? = null
    }

    //{"jsonrpc":"2.0","id":"test","result":{"id":"39526c0eb6ba","apiExtraFee":"0","changellyFee":"0.5","payinExtraId":null,"status":"new","currencyFrom":"eth","currencyTo":"BTC","amountTo":0,"payinAddress":"0xdd0a917944efc6a371829053ad318a6a20ee1090","payoutAddress":"1J3cP281yiy39x3gcPaErDR6CSbLZZKzGz","createdAt":"2017-11-22T18:47:19.000Z"}}
    class ChangellyTransaction {
        @JvmField
        var result // payin_address, ID
                : ChangellyTransactionOffer? = null
    }

    // {"jsonrpc":"2.0","id":"test","result":"0.03595702"}
    class ChangellyAnswerDouble {
        var result = 0.0
    }

    class ChangellyAnswerListString {
        var result: List<String>? = null
    }

    // end data classes
    @POST("getCurrencies")
    fun getCurrencies(): Call<ChangellyAnswerListString?>

    @POST("getCurrenciesFull")
    fun getCurrenciesFull(): List<ChangellyCurrency?>

    @POST("getMinAmount")
    fun getMinAmount(@Query("from") from: String?, @Query("to") to: String?): Call<ChangellyAnswerDouble?>

    @POST("getExchangeAmount")
    fun getExchangeAmount(@Query("from") from: String?, @Query("to") to: String?, @Query("amount") amount: Double): Call<ChangellyAnswerDouble?>

    @POST("getExchangeAmount")
    suspend fun exchangeAmount(@Query("from") from: String,
                               @Query("to") to: String,
                               @Query("amount") amount: Double): Response<ChangellyAnswerDouble>

    @POST("createTransaction")
    fun createTransaction(@Query("from") from: String?, @Query("to") to: String?, @Query("amount") amount: Double, @Query("address") address: String?): Call<ChangellyTransaction?>

    //    @POST("getStatus")
    //    Call<ChangellyStatus> getStatus(@Query("transaction") String transaction);
    @POST("getTransactions")
    fun getTransactions(): Call<List<ChangellyTransaction?>?>?

    companion object {
        const val BCH = "BCH"
        const val BTC = "BTC"
        const val FROM = "FROM"
        const val TO = "TO"
        const val AMOUNT = "AMOUNT"
        const val DESTADDRESS = "DESTADDRESS"

        //public static final HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
        val changellyHeader = ChangellyHeaderInterceptor()

        //public static final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(changellyHeader).addInterceptor(logging).build();
        val httpClient = OkHttpClient.Builder().addInterceptor(changellyHeader).build()

        @JvmStatic
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.changelly.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
    }
}